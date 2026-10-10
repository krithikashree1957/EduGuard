package com.eduguard.eduguard.integration.adapter;

import com.eduguard.eduguard.integration.config.IntegrationProviderProperties;
import com.eduguard.eduguard.integration.model.IntegrationHealthState;
import com.eduguard.eduguard.integration.model.IntegrationMode;
import com.eduguard.eduguard.integration.model.IntegrationType;
import com.eduguard.eduguard.integration.model.ResourceQuery;
import com.eduguard.eduguard.integration.model.ResourceSource;
import com.eduguard.eduguard.integration.model.SupportResource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigurableResourceIntegrationAdapterTests {
    private static final ResourceQuery ALL_RESOURCES = new ResourceQuery(null, null, 50);

    @Test
    void mockAdaptersReturnClearlyIdentifiedFixtureResources() {
        FakeExternalResourceClient externalClient = new FakeExternalResourceClient();
        var academic = adapter(IntegrationType.ACADEMIC, IntegrationMode.MOCK, true,
                ConfigurableResourceIntegrationAdapter.academicMockResources(), externalClient);
        var wellbeing = adapter(IntegrationType.WELLBEING, IntegrationMode.MOCK, true,
                ConfigurableResourceIntegrationAdapter.wellbeingMockResources(), externalClient);

        var academicResponse = academic.search(ALL_RESOURCES);
        var wellbeingResponse = wellbeing.search(ALL_RESOURCES);

        assertEquals(ResourceSource.MOCK, academicResponse.source());
        assertEquals(ResourceSource.MOCK, academicResponse.resources().getFirst().source());
        assertEquals(ResourceSource.MOCK, wellbeingResponse.source());
        assertEquals(ResourceSource.MOCK, wellbeingResponse.resources().getFirst().source());
        assertEquals("EduGuard mock dataset", wellbeingResponse.resources().getFirst().provider());
        assertEquals(0, externalClient.fetchCount);
    }

    @Test
    void normalizesExternalResourcesAndFiltersLocally() {
        FakeExternalResourceClient externalClient = new FakeExternalResourceClient();
        externalClient.resources = List.of(new ProviderResourceResponse(
                " resource-1 ", "  Algebra tutoring ", "Practice support", "Study Skills", "Provider",
                "https://provider.example/resources/1"));
        var adapter = adapter(IntegrationType.ACADEMIC, IntegrationMode.EXTERNAL, true, List.of(), externalClient);

        var result = adapter.search(new ResourceQuery("study-skills", "algebra", 1));

        assertEquals(ResourceSource.EXTERNAL, result.source());
        assertEquals(1, result.resources().size());
        assertEquals("resource-1", result.resources().getFirst().resourceId());
        assertEquals("study-skills", result.resources().getFirst().category());
        assertEquals(ResourceSource.EXTERNAL, result.resources().getFirst().source());
        assertEquals(1, externalClient.fetchCount);
    }

    @Test
    void validSearchWithoutMatchesReturnsEmptyList() {
        FakeExternalResourceClient externalClient = new FakeExternalResourceClient();
        var adapter = adapter(IntegrationType.ACADEMIC, IntegrationMode.MOCK, true,
                ConfigurableResourceIntegrationAdapter.academicMockResources(), externalClient);

        var result = adapter.search(new ResourceQuery("unknown-category", null, 20));

        assertEquals(List.of(), result.resources());
    }

    @Test
    void disabledAdapterRejectsSearchAndReportsDisabled() {
        var adapter = adapter(IntegrationType.ACADEMIC, IntegrationMode.MOCK, false, List.of(),
                new FakeExternalResourceClient());

        IntegrationCallException exception = assertThrows(
                IntegrationCallException.class, () -> adapter.search(ALL_RESOURCES));

        assertEquals(IntegrationFailure.DISABLED, exception.getFailure());
        assertEquals(List.of(IntegrationHealthState.DISABLED), adapter.health().states());
    }

    @Test
    void healthReportsMockAvailableAndUnavailableStates() {
        FakeExternalResourceClient availableClient = new FakeExternalResourceClient();
        availableClient.available = true;
        var mockAdapter = adapter(IntegrationType.ACADEMIC, IntegrationMode.MOCK, true, List.of(), availableClient);
        var availableAdapter = adapter(IntegrationType.ACADEMIC, IntegrationMode.EXTERNAL, true, List.of(),
                availableClient);
        var unavailableAdapter = adapter(IntegrationType.ACADEMIC, IntegrationMode.EXTERNAL, true, List.of(),
                new FakeExternalResourceClient());

        assertEquals(List.of(IntegrationHealthState.ENABLED, IntegrationHealthState.MOCK),
                mockAdapter.health().states());
        assertEquals(List.of(IntegrationHealthState.ENABLED, IntegrationHealthState.AVAILABLE),
                availableAdapter.health().states());
        assertEquals(List.of(IntegrationHealthState.ENABLED, IntegrationHealthState.UNAVAILABLE),
                unavailableAdapter.health().states());
    }

    @Test
    void timeoutAndMalformedResponsesAreRepresentedWithoutProviderDetails() {
        FakeExternalResourceClient timeoutClient = new FakeExternalResourceClient();
        timeoutClient.failure = new IntegrationCallException(IntegrationFailure.TIMEOUT);
        var timeoutAdapter = adapter(IntegrationType.ACADEMIC, IntegrationMode.EXTERNAL, true, List.of(),
                timeoutClient);
        FakeExternalResourceClient malformedClient = new FakeExternalResourceClient();
        malformedClient.resources = List.of(new ProviderResourceResponse(
                "id", "Title", "Description", "category", "provider", "javascript:alert(1)"));
        var malformedAdapter = adapter(IntegrationType.ACADEMIC, IntegrationMode.EXTERNAL, true, List.of(),
                malformedClient);

        assertEquals(IntegrationFailure.TIMEOUT, assertThrows(
                IntegrationCallException.class, () -> timeoutAdapter.search(ALL_RESOURCES)).getFailure());
        assertEquals(IntegrationFailure.MALFORMED_RESPONSE, assertThrows(
                IntegrationCallException.class, () -> malformedAdapter.search(ALL_RESOURCES)).getFailure());
    }

    private ConfigurableResourceIntegrationAdapter adapter(
            IntegrationType type,
            IntegrationMode mode,
            boolean enabled,
            List<SupportResource> mockResources,
            FakeExternalResourceClient externalClient) {
        IntegrationProviderProperties properties = new IntegrationProviderProperties();
        properties.setMode(mode);
        properties.setEnabled(enabled);
        return new ConfigurableResourceIntegrationAdapter(externalClient, properties, type, mockResources);
    }

    private static class FakeExternalResourceClient implements ExternalResourceClient {
        private List<ProviderResourceResponse> resources = List.of();
        private boolean available;
        private int fetchCount;
        private IntegrationCallException failure;

        @Override
        public List<ProviderResourceResponse> fetchResources(
                IntegrationType integration,
                IntegrationProviderProperties properties) {
            fetchCount++;
            if (failure != null) {
                throw failure;
            }
            return resources;
        }

        @Override
        public boolean isAvailable(IntegrationType integration, IntegrationProviderProperties properties) {
            return available;
        }
    }
}
