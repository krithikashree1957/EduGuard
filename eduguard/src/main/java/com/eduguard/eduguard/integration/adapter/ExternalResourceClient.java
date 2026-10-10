package com.eduguard.eduguard.integration.adapter;

import com.eduguard.eduguard.integration.config.IntegrationProviderProperties;
import com.eduguard.eduguard.integration.model.IntegrationType;

import java.util.List;

public interface ExternalResourceClient {
    List<ProviderResourceResponse> fetchResources(
            IntegrationType integration,
            IntegrationProviderProperties properties);

    boolean isAvailable(IntegrationType integration, IntegrationProviderProperties properties);
}
