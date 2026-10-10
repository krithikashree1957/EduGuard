package com.eduguard.eduguard.integration.api;

import com.eduguard.eduguard.integration.model.IntegrationHealth;
import com.eduguard.eduguard.integration.model.IntegrationType;
import com.eduguard.eduguard.integration.model.ResourceQuery;
import com.eduguard.eduguard.integration.model.ResourceSearchResponse;
import com.eduguard.eduguard.integration.service.ResourceIntegrationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/integrations")
public class IntegrationResourceController {
    private final ResourceIntegrationService integrationService;

    public IntegrationResourceController(ResourceIntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @GetMapping("/resources/academic")
    public ResourceSearchResponse academicResources(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "20") int pageSize) {
        return search(IntegrationType.ACADEMIC, category, keyword, pageSize);
    }

    @GetMapping("/resources/wellbeing")
    public ResourceSearchResponse wellbeingResources(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "20") int pageSize) {
        return search(IntegrationType.WELLBEING, category, keyword, pageSize);
    }

    @GetMapping("/health")
    public List<IntegrationHealth> health() {
        return integrationService.health();
    }

    private ResourceSearchResponse search(
            IntegrationType integration,
            String category,
            String keyword,
            int pageSize) {
        String normalizedCategory = normalize(category, 60);
        String normalizedKeyword = normalize(keyword, 100);
        if (pageSize < 1 || pageSize > 50) {
            throw new InvalidResourceQueryException();
        }
        return integrationService.search(
                integration, new ResourceQuery(normalizedCategory, normalizedKeyword, pageSize));
    }

    private String normalize(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > maxLength) {
            throw new InvalidResourceQueryException();
        }
        return normalized;
    }
}
