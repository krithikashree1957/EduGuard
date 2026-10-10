package com.eduguard.eduguard.integration.service;

import com.eduguard.eduguard.integration.model.IntegrationHealth;
import com.eduguard.eduguard.integration.model.IntegrationType;
import com.eduguard.eduguard.integration.model.ResourceQuery;
import com.eduguard.eduguard.integration.model.ResourceSearchResponse;

import java.util.List;

public interface ResourceIntegrationService {
    ResourceSearchResponse search(IntegrationType integration, ResourceQuery query);

    List<IntegrationHealth> health();
}
