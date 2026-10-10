package com.eduguard.risk_recommendation_service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RiskAssessmentCalculatorTest {

    private final RiskAssessmentCalculator calculator =
            new RiskAssessmentCalculator();

    @Test
    void calculatesAllIndicatorsOnAConsistentScale() {
        RiskAssessment result = calculator.calculate(1L, PerformanceSnapshot.from(Map.of(
                "attendancePercentage", 80,
                "participationScore", 0.5,
                "assignmentCompletionRate", 70,
                "assessments", List.of(Map.of("percentage", 40)))));

        assertEquals(38, result.score());
        assertEquals("MEDIUM", result.tier());
        assertEquals("COMPLETE", result.status());
        assertEquals(4, result.availableIndicatorCount());
        assertEquals(4, result.indicators().size());
    }

    @Test
    void keepsBoundaryBandsStable() {
        RiskAssessment low = calculator.calculate(1L, snapshot(66, 66, 66, 66));
        RiskAssessment medium = calculator.calculate(1L, snapshot(65, 65, 65, 65));
        RiskAssessment high = calculator.calculate(1L, snapshot(40, 40, 40, 40));

        assertEquals("LOW", low.tier());
        assertEquals("MEDIUM", medium.tier());
        assertEquals("HIGH", high.tier());
        assertEquals(35, medium.score());
        assertEquals(60, high.score());
    }

    @Test
    void ignoresInvalidAndMissingValuesWithoutTreatingThemAsPerfect() {
        PerformanceSnapshot snapshot = PerformanceSnapshot.from(Map.of(
                "attendancePercentage", -1,
                "participationScore", 150,
                "assessments", List.of(Map.of("percentage", "bad"))));

        assertEquals(null, snapshot.attendance());
        assertEquals(null, snapshot.assessmentAverage());
        assertEquals(null, snapshot.participation());
        assertEquals(null, snapshot.assignmentCompletion());
        assertThrows(ResponseStatusException.class,
                () -> calculator.calculate(1L, new PerformanceSnapshot(
                        null, null, null, null, false)));
    }

    @Test
    void normalizesScoreToZeroThroughOneHundred() {
        RiskAssessment result = calculator.calculate(1L, snapshot(0, 0, 0, 0));

        assertEquals(100, result.score());
        assertEquals("HIGH", result.tier());
    }

    private static PerformanceSnapshot snapshot(
            double attendance, double assessment, double participation,
            double assignments) {
        return new PerformanceSnapshot(
                attendance, assessment, participation, assignments, true);
    }
}
