package com.eduguard.eduguard.integration.adapter;

public record ProviderResourceResponse(
        String id,
        String title,
        String description,
        String category,
        String provider,
        String url) {
}
