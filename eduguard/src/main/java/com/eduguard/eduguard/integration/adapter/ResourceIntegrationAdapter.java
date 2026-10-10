package com.eduguard.eduguard.integration.adapter;

import com.eduguard.eduguard.integration.model.IntegrationHealth;
import com.eduguard.eduguard.integration.model.IntegrationType;
import com.eduguard.eduguard.integration.model.ResourceQuery;
import com.eduguard.eduguard.integration.model.ResourceSearchResponse;

public interface ResourceIntegrationAdapter {
    IntegrationType type();

    ResourceSearchResponse search(ResourceQuery query);

    IntegrationHealth health();
}
