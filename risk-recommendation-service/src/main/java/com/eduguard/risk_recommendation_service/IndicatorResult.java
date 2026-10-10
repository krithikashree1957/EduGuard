package com.eduguard.risk_recommendation_service;

record IndicatorResult(
        String name,
        double performanceScore,
        double riskScore,
        double weight,
        double contribution,
        String status,
        String explanation) {
}
