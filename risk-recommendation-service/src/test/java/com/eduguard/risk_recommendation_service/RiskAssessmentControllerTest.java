package com.eduguard.risk_recommendation_service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RiskAssessmentControllerTest {

    private final StudentPerformanceClient client =
            mock(StudentPerformanceClient.class);
    private final LogisticRegressionRiskModel riskModel =
            new LogisticRegressionRiskModel();
    private final RiskAssessmentController controller =
            new RiskAssessmentController(
                    client, new RiskAssessmentCalculator(),
                    new RecommendationGenerator(), riskModel);

    @Test
    void preservesLegacyFieldsAndAddsStructuredAssessmentDetails() {
        when(client.fetch(1L)).thenReturn(new PerformanceSnapshot(
                80.0, 70.0, 60.0, 90.0, true));

        Map<String, Object> response = controller.assessRisk(1L);

        assertEquals(24.5, response.get("riskScore"));
        assertEquals("LOW", response.get("riskTier"));
        assertEquals("COMPLETE", response.get("assessmentStatus"));
        assertEquals("LOGISTIC_REGRESSION", response.get("riskModel"));
        assertEquals("synthetic_support_need", response.get("modelOutcome"));
        assertTrue(java.util.List.of("HIGH", "MEDIUM", "LOW")
                .contains(response.get("modelRiskTier")));
        assertTrue(
                (double) response.get("modelProbability") >= 0
                        && (double) response.get("modelProbability") <= 1);
        assertEquals(4, ((java.util.List<?>) response.get("indicators")).size());
        assertEquals(1, ((java.util.List<?>) response
                .get("recommendedInterventions")).size());
        assertEquals(1, ((java.util.List<?>) response
                .get("recommendations")).size());
    }

    @Test
    void propagatesExplicitUpstreamStatusWithoutLeakingDetails() {
        ResponseStatusException upstreamFailure = new ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_GATEWAY,
                "Student service response was unavailable or malformed");
        when(client.fetch(1L)).thenThrow(upstreamFailure);

        ResponseStatusException result = assertThrows(
                ResponseStatusException.class,
                () -> controller.assessRisk(1L));

        assertEquals(502, result.getStatusCode().value());
        assertEquals("Student service response was unavailable or malformed",
                result.getReason());
    }

    @Test
    void keepsHeuristicAvailableWhenModelFeaturesAreOutOfRange() {
        when(client.fetch(1L)).thenReturn(new PerformanceSnapshot(
                101.0, 70.0, 60.0, 90.0, true));

        Map<String, Object> response = controller.assessRisk(1L);

        assertEquals("COMPLETE", response.get("assessmentStatus"));
        assertEquals(4, response.get("availableIndicatorCount"));
        assertEquals(16.5, response.get("riskScore"));
        assertEquals("UNAVAILABLE", response.get("modelRiskTier"));
        assertNull(response.get("modelProbability"));
    }
}
