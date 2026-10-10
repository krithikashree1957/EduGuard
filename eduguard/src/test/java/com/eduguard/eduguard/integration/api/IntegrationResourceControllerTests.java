package com.eduguard.eduguard.integration.api;

import com.eduguard.eduguard.integration.adapter.IntegrationCallException;
import com.eduguard.eduguard.integration.adapter.IntegrationFailure;
import com.eduguard.eduguard.integration.model.IntegrationHealth;
import com.eduguard.eduguard.integration.model.IntegrationHealthState;
import com.eduguard.eduguard.integration.model.IntegrationMode;
import com.eduguard.eduguard.integration.model.IntegrationType;
import com.eduguard.eduguard.integration.model.ResourceQuery;
import com.eduguard.eduguard.integration.model.ResourceSearchResponse;
import com.eduguard.eduguard.integration.model.ResourceSource;
import com.eduguard.eduguard.integration.model.SupportResource;
import com.eduguard.eduguard.integration.service.ResourceIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IntegrationResourceControllerTests {
    private StubResourceIntegrationService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = new StubResourceIntegrationService();
        mockMvc = MockMvcBuilders.standaloneSetup(new IntegrationResourceController(service))
                .setControllerAdvice(new IntegrationApiExceptionHandler())
                .build();
    }

    @Test
    void academicEndpointReturnsStableResourceEnvelope() throws Exception {
        mockMvc.perform(get("/api/integrations/resources/academic")
                        .param("category", " tutoring ")
                        .param("keyword", " algebra ")
                        .param("pageSize", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.integration").value("ACADEMIC"))
                .andExpect(jsonPath("$.source").value("MOCK"))
                .andExpect(jsonPath("$.resources[0].source").value("MOCK"));
        assertEquals(new ResourceQuery("tutoring", "algebra", 7), service.lastQuery);
    }

    @Test
    void rejectsInvalidPageSizeAndBlankSearchValues() throws Exception {
        mockMvc.perform(get("/api/integrations/resources/academic").param("pageSize", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_QUERY"));
        mockMvc.perform(get("/api/integrations/resources/wellbeing").param("keyword", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_QUERY"));
        mockMvc.perform(get("/api/integrations/resources/wellbeing").param("pageSize", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_QUERY"));
    }

    @Test
    void disabledIntegrationReturnsSafeConsistentError() throws Exception {
        service.failure = new IntegrationCallException(IntegrationFailure.DISABLED);

        mockMvc.perform(get("/api/integrations/resources/academic"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("INTEGRATION_DISABLED"))
                .andExpect(jsonPath("$.error.message").value("This integration is currently disabled."));
    }

    @Test
    void upstreamTimeoutAndUnexpectedErrorDoNotExposeExceptionDetails() throws Exception {
        service.failure = new IntegrationCallException(IntegrationFailure.TIMEOUT);
        mockMvc.perform(get("/api/integrations/resources/academic"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.error.code").value("UPSTREAM_TIMEOUT"));

        service.failure = new IllegalStateException("secret-token-stacktrace");
        mockMvc.perform(get("/api/integrations/resources/academic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.message").value("The integration request could not be completed."))
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("secret-token"))));
    }

    @Test
    void healthEndpointReturnsOnlySafeConfigurationState() throws Exception {
        mockMvc.perform(get("/api/integrations/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].states[0]").value("ENABLED"))
                .andExpect(jsonPath("$[0].states[1]").value("MOCK"))
                .andExpect(jsonPath("$[0].apiKey").doesNotExist());
    }

    private static class StubResourceIntegrationService implements ResourceIntegrationService {
        private RuntimeException failure;
        private ResourceQuery lastQuery;

        @Override
        public ResourceSearchResponse search(IntegrationType integration, ResourceQuery query) {
            if (failure != null) {
                throw failure;
            }
            lastQuery = query;
            return new ResourceSearchResponse(integration, ResourceSource.MOCK, List.of(new SupportResource(
                    "mock-1", "Algebra example", "Mock response", "tutoring", "EduGuard mock dataset",
                    "https://example.invalid/resource", ResourceSource.MOCK)));
        }

        @Override
        public List<IntegrationHealth> health() {
            return List.of(new IntegrationHealth(IntegrationType.ACADEMIC, true, IntegrationMode.MOCK,
                    List.of(IntegrationHealthState.ENABLED, IntegrationHealthState.MOCK)));
        }
    }
}
