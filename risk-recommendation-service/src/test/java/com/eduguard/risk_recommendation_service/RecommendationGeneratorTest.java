package com.eduguard.risk_recommendation_service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationGeneratorTest {

    private final RecommendationGenerator generator =
            new RecommendationGenerator();

    @Test
    void recommendsActionsWithReasonsAndPriorityForTriggeredIndicators() {
        RiskAssessment assessment = new RiskAssessment(
                70, "HIGH", "COMPLETE", 4, 4,
                List.of(
                        indicator("attendance", 30),
                        indicator("assessmentPerformance", 80),
                        indicator("participation", 20),
                        indicator("assignmentCompletion", 90)),
                List.of("Low attendance", "Low participation"));

        List<Recommendation> recommendations = generator.generate(assessment);

        assertEquals(3, recommendations.size());
        assertTrue(recommendations.get(0).recommendation()
                .contains("attendance barriers"));
        assertEquals("HIGH", recommendations.get(0).priority());
        assertTrue(recommendations.get(0).reason().contains("30.0%"));
        assertEquals("HIGH", recommendations.get(2).priority());
    }

    @Test
    void returnsAdvisoryLowPriorityMonitoringWhenNoThresholdIsTriggered() {
        RiskAssessment assessment = new RiskAssessment(
                10, "LOW", "PARTIAL", 1, 4,
                List.of(indicator("attendance", 95),
                        new IndicatorResult("participation", 0, 0, .15,
                                0, "UNAVAILABLE", "Unavailable")),
                List.of());

        List<Recommendation> recommendations = generator.generate(assessment);

        assertEquals(1, recommendations.size());
        assertEquals("LOW", recommendations.get(0).priority());
        assertTrue(recommendations.get(0).reason().contains("No available"));
    }

    @Test
    void usesPositiveFittedModelContributionsForSupportiveRecommendations() {
        ModelRiskPrediction prediction = new ModelRiskPrediction(
                0.72, "HIGH", List.of(
                        new FeatureContribution("attendance", 45, -2.0, 70, 0.42),
                        new FeatureContribution("assessmentPerformance", 50, -1.5, 70, 0.30),
                        new FeatureContribution("participation", 80, -1.0, 60, -0.20)));

        List<Recommendation> recommendations = generator.generate(
                new RiskAssessment(40, "MEDIUM", "COMPLETE", 4, 4,
                        List.of(), List.of()), prediction);

        assertEquals(3, recommendations.size());
        assertTrue(recommendations.get(0).recommendation()
                .contains("attendance barriers"));
        assertTrue(recommendations.get(0).reason().contains("+0.420 log-odds"));
        assertTrue(recommendations.get(0).reason().contains("not a cause"));
        assertEquals("HIGH", recommendations.get(0).priority());
    }

    private static IndicatorResult indicator(String name, double performance) {
        return new IndicatorResult(name, performance, 100 - performance,
                .15, (100 - performance) * .15, "AVAILABLE", "normalized");
    }
}
