package com.eduguard.risk_recommendation_service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RiskAssessmentControllerTest {

    private final StudentPerformanceClient client =
            mock(StudentPerformanceClient.class);
    private final RiskAssessmentController controller =
            new RiskAssessmentController(
                    client, new RiskAssessmentCalculator(),
                    new RecommendationGenerator());

    @Test
    void preservesLegacyFieldsAndAddsStructuredAssessmentDetails() {
        when(client.fetch(1L)).thenReturn(new PerformanceSnapshot(
                80.0, 70.0, 60.0, 90.0, true));

        Map<String, Object> response = controller.assessRisk(1L);

        assertEquals(24.5, response.get("riskScore"));
        assertEquals("LOW", response.get("riskTier"));
        assertEquals("COMPLETE", response.get("assessmentStatus"));
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
}
