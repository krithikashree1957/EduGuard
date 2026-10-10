package com.eduguard.eduguard.integration.model;

public record SupportResource(
        String resourceId,
        String title,
        String description,
        String category,
        String provider,
        String url,
        ResourceSource source) {
}
