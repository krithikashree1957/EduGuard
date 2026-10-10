package com.eduguard.eduguard.integration.adapter;

import com.eduguard.eduguard.integration.config.IntegrationProviderProperties;
import com.eduguard.eduguard.integration.model.IntegrationHealth;
import com.eduguard.eduguard.integration.model.IntegrationHealthState;
import com.eduguard.eduguard.integration.model.IntegrationMode;
import com.eduguard.eduguard.integration.model.IntegrationType;
import com.eduguard.eduguard.integration.model.ResourceQuery;
import com.eduguard.eduguard.integration.model.ResourceSearchResponse;
import com.eduguard.eduguard.integration.model.ResourceSource;
import com.eduguard.eduguard.integration.model.SupportResource;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConfigurableResourceIntegrationAdapter implements ResourceIntegrationAdapter {
    private final ExternalResourceClient externalClient;
    private final IntegrationType type;
    private final IntegrationProviderProperties properties;
    private final List<SupportResource> mockResources;

    public ConfigurableResourceIntegrationAdapter(
            ExternalResourceClient externalClient,
            IntegrationProviderProperties properties,
            IntegrationType type,
            List<SupportResource> mockResources) {
        this.externalClient = externalClient;
        this.properties = properties;
        this.type = type;
        this.mockResources = List.copyOf(mockResources);
    }

    @Override
    public IntegrationType type() {
        return type;
    }

    @Override
    public ResourceSearchResponse search(ResourceQuery query) {
        if (!properties.isEnabled()) {
            throw new IntegrationCallException(IntegrationFailure.DISABLED);
        }
        ResourceSource source = properties.getMode() == IntegrationMode.MOCK
                ? ResourceSource.MOCK : ResourceSource.EXTERNAL;
        List<SupportResource> results;
        if (source == ResourceSource.MOCK) {
            results = mockResources;
        } else {
            results = externalClient.fetchResources(type, properties).stream()
                    .map(this::normalize)
                    .toList();
        }
        return new ResourceSearchResponse(type, source, results.stream()
                .filter(resource -> matches(resource, query))
                .limit(query.pageSize())
                .toList());
    }

    @Override
    public IntegrationHealth health() {
        List<IntegrationHealthState> states = new ArrayList<>();
        if (!properties.isEnabled()) {
            states.add(IntegrationHealthState.DISABLED);
            return new IntegrationHealth(type, false, properties.getMode(), List.copyOf(states));
        }

        states.add(IntegrationHealthState.ENABLED);
        if (properties.getMode() == IntegrationMode.MOCK) {
            states.add(IntegrationHealthState.MOCK);
        } else if (externalClient.isAvailable(type, properties)) {
            states.add(IntegrationHealthState.AVAILABLE);
        } else {
            states.add(IntegrationHealthState.UNAVAILABLE);
        }
        return new IntegrationHealth(type, true, properties.getMode(), List.copyOf(states));
    }

    private SupportResource normalize(ProviderResourceResponse response) {
        if (response == null || isBlank(response.id()) || isBlank(response.title())
                || isBlank(response.description()) || isBlank(response.category())
                || isBlank(response.provider()) || isBlank(response.url())) {
            throw new IntegrationCallException(IntegrationFailure.MALFORMED_RESPONSE);
        }

        URI resourceUri;
        try {
            resourceUri = URI.create(response.url().trim());
        } catch (IllegalArgumentException exception) {
            throw new IntegrationCallException(IntegrationFailure.MALFORMED_RESPONSE);
        }
        String scheme = resourceUri.getScheme();
        if (!resourceUri.isAbsolute() || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            throw new IntegrationCallException(IntegrationFailure.MALFORMED_RESPONSE);
        }
        return new SupportResource(
                response.id().trim(),
                response.title().trim(),
                response.description().trim(),
                response.category().trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "-"),
                response.provider().trim(),
                resourceUri.toString(),
                ResourceSource.EXTERNAL);
    }

    private boolean matches(SupportResource resource, ResourceQuery query) {
        boolean categoryMatches = query.category() == null
                || resource.category().equalsIgnoreCase(query.category());
        if (!categoryMatches) {
            return false;
        }
        if (query.keyword() == null) {
            return true;
        }
        String keyword = query.keyword().toLowerCase(Locale.ROOT);
        return resource.title().toLowerCase(Locale.ROOT).contains(keyword)
                || resource.description().toLowerCase(Locale.ROOT).contains(keyword)
                || resource.category().toLowerCase(Locale.ROOT).contains(keyword)
                || resource.provider().toLowerCase(Locale.ROOT).contains(keyword);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static List<SupportResource> academicMockResources() {
        return List.of(new SupportResource(
                "mock-academic-1",
                "Academic tutoring example (mock)",
                "Demonstration fixture only; replace with a verified tutoring provider.",
                "tutoring",
                "EduGuard mock dataset",
                "https://example.invalid/academic/tutoring",
                ResourceSource.MOCK));
    }

    public static List<SupportResource> wellbeingMockResources() {
        return List.of(new SupportResource(
                "mock-wellbeing-1",
                "General wellbeing information example (mock)",
                "Demonstration fixture only; general educational information, not counselling or clinical advice.",
                "wellbeing-education",
                "EduGuard mock dataset",
                "https://example.invalid/wellbeing/information",
                ResourceSource.MOCK));
    }
}
