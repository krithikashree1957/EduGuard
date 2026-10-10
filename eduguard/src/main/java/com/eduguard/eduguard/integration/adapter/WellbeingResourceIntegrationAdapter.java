package com.eduguard.eduguard.integration.adapter;

import com.eduguard.eduguard.integration.config.IntegrationProviderProperties;
import com.eduguard.eduguard.integration.model.IntegrationType;

public class WellbeingResourceIntegrationAdapter extends ConfigurableResourceIntegrationAdapter {
    public WellbeingResourceIntegrationAdapter(
            ExternalResourceClient externalResourceClient,
            IntegrationProviderProperties properties) {
        super(externalResourceClient, properties, IntegrationType.WELLBEING, wellbeingMockResources());
    }
}
