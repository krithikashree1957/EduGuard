package com.eduguard.risk_recommendation_service;

import java.util.List;

record RiskAssessment(
        double score,
        String tier,
        String status,
        int availableIndicatorCount,
        int totalIndicatorCount,
        List<IndicatorResult> indicators,
        List<String> factors) {
}
