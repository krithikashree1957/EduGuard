package com.eduguard.eduguard.integration.config;

import com.eduguard.eduguard.integration.adapter.AcademicResourceIntegrationAdapter;
import com.eduguard.eduguard.integration.adapter.ExternalResourceClient;
import com.eduguard.eduguard.integration.adapter.ResourceIntegrationAdapter;
import com.eduguard.eduguard.integration.adapter.WellbeingResourceIntegrationAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(IntegrationProperties.class)
public class IntegrationAdapterConfiguration {
    @Bean
    @ConditionalOnMissingBean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @ConditionalOnMissingBean(name = "academicResourceIntegrationAdapter")
    ResourceIntegrationAdapter academicResourceIntegrationAdapter(
            ExternalResourceClient externalResourceClient,
            IntegrationProperties properties) {
        return new AcademicResourceIntegrationAdapter(externalResourceClient, properties.getAcademic());
    }

    @Bean
    @ConditionalOnMissingBean(name = "wellbeingResourceIntegrationAdapter")
    ResourceIntegrationAdapter wellbeingResourceIntegrationAdapter(
            ExternalResourceClient externalResourceClient,
            IntegrationProperties properties) {
        return new WellbeingResourceIntegrationAdapter(externalResourceClient, properties.getWellbeing());
    }
}
