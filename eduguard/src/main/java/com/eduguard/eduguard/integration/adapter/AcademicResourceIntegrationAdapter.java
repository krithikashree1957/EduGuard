package com.eduguard.eduguard.integration.adapter;

import com.eduguard.eduguard.integration.config.IntegrationProviderProperties;
import com.eduguard.eduguard.integration.model.IntegrationType;

public class AcademicResourceIntegrationAdapter extends ConfigurableResourceIntegrationAdapter {
    public AcademicResourceIntegrationAdapter(
            ExternalResourceClient externalResourceClient,
            IntegrationProviderProperties properties) {
        super(externalResourceClient, properties, IntegrationType.ACADEMIC, academicMockResources());
    }
}
