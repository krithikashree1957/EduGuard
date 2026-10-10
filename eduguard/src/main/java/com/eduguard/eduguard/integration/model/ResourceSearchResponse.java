package com.eduguard.eduguard.integration.model;

import java.util.List;

public record ResourceSearchResponse(
        IntegrationType integration,
        ResourceSource source,
        List<SupportResource> resources) {
}
