package com.eduguard.eduguard.integration.service;

import com.eduguard.eduguard.integration.adapter.ResourceIntegrationAdapter;
import com.eduguard.eduguard.integration.model.IntegrationHealth;
import com.eduguard.eduguard.integration.model.IntegrationType;
import com.eduguard.eduguard.integration.model.ResourceQuery;
import com.eduguard.eduguard.integration.model.ResourceSearchResponse;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class DefaultResourceIntegrationService implements ResourceIntegrationService {
    private final Map<IntegrationType, ResourceIntegrationAdapter> adapters;

    public DefaultResourceIntegrationService(List<ResourceIntegrationAdapter> adapters) {
        this.adapters = new EnumMap<>(IntegrationType.class);
        for (ResourceIntegrationAdapter adapter : adapters) {
            if (this.adapters.put(adapter.type(), adapter) != null) {
                throw new IllegalStateException("Multiple adapters configured for " + adapter.type());
            }
        }
        for (IntegrationType type : IntegrationType.values()) {
            if (!this.adapters.containsKey(type)) {
                throw new IllegalStateException("No adapter configured for " + type);
            }
        }
    }

    @Override
    public ResourceSearchResponse search(IntegrationType integration, ResourceQuery query) {
        return adapters.get(integration).search(query);
    }

    @Override
    public List<IntegrationHealth> health() {
        return adapters.values().stream().map(ResourceIntegrationAdapter::health).toList();
    }
}
