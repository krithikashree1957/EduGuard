package com.eduguard.risk_recommendation_service;

import java.util.List;
import java.util.Map;

record PerformanceSnapshot(
        Double attendance,
        Double assessmentAverage,
        Double participation,
        Double assignmentCompletion,
        boolean hasAssessments) {

    static PerformanceSnapshot from(Map<?, ?> data) {
        Double attendance = percentage(data.get("attendancePercentage"));
        Double participation = percentage(data.get("participationScore"));
        Double assignments = percentage(data.get("assignmentCompletionRate"));

        Object raw = data.get("assessments");
        double total = 0;
        int count = 0;
        if (raw instanceof List<?> assessments) {
            for (Object item : assessments) {
                if (item instanceof Map<?, ?> assessment) {
                    Double percentage = percentage(assessment.get("percentage"));
                    if (percentage != null) {
                        total += percentage;
                        count++;
                    }
                }
            }
        }

        return new PerformanceSnapshot(
                attendance,
                count == 0 ? null : total / count,
                participation,
                assignments,
                raw instanceof List<?> && !((List<?>) raw).isEmpty());
    }

    private static Double percentage(Object value) {
        if (!(value instanceof Number number)) {
            return null;
        }
        double numeric = number.doubleValue();
        if (!Double.isFinite(numeric) || numeric < 0 || numeric > 100) {
            return null;
        }
        return numeric <= 1 ? numeric * 100 : numeric;
    }
}
