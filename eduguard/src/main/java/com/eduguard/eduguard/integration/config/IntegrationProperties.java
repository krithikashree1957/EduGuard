package com.eduguard.eduguard.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "eduguard.integrations")
public class IntegrationProperties {
    private IntegrationProviderProperties academic = new IntegrationProviderProperties();
    private IntegrationProviderProperties wellbeing = new IntegrationProviderProperties();

    public IntegrationProviderProperties getAcademic() {
        return academic;
    }

    public void setAcademic(IntegrationProviderProperties academic) {
        this.academic = academic;
    }

    public IntegrationProviderProperties getWellbeing() {
        return wellbeing;
    }

    public void setWellbeing(IntegrationProviderProperties wellbeing) {
        this.wellbeing = wellbeing;
    }
}
