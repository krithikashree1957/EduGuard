package com.eduguard.risk_recommendation_service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
class RiskAssessmentCalculator {

    static final double ATTENDANCE_WEIGHT = 0.40;
    static final double ASSESSMENT_WEIGHT = 0.30;
    static final double PARTICIPATION_WEIGHT = 0.15;
    static final double ASSIGNMENT_WEIGHT = 0.15;

    RiskAssessment calculate(Long studentId, PerformanceSnapshot performance) {
        List<IndicatorResult> indicators = new ArrayList<>();
        add(indicators, "attendance", performance.attendance(),
                ATTENDANCE_WEIGHT, "Attendance data is unavailable.");
        add(indicators, "assessmentPerformance", performance.assessmentAverage(),
                ASSESSMENT_WEIGHT, "No valid assessment percentages are available.");
        add(indicators, "participation", performance.participation(),
                PARTICIPATION_WEIGHT, "Participation data is unavailable.");
        add(indicators, "assignmentCompletion", performance.assignmentCompletion(),
                ASSIGNMENT_WEIGHT, "Assignment completion data is unavailable.");

        double weightedRisk = indicators.stream()
                .filter(indicator -> "AVAILABLE".equals(indicator.status()))
                .mapToDouble(IndicatorResult::contribution)
                .sum();
        double availableWeight = indicators.stream()
                .filter(indicator -> "AVAILABLE".equals(indicator.status()))
                .mapToDouble(IndicatorResult::weight)
                .sum();

        if (availableWeight == 0) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT,
                    "No usable performance data is available for risk assessment.");
        }

        double score = round(weightedRisk / availableWeight);
        String status = indicators.stream()
                .allMatch(indicator -> "AVAILABLE".equals(indicator.status()))
                ? "COMPLETE" : "PARTIAL";
        List<String> factors = indicators.stream()
                .filter(indicator -> "AVAILABLE".equals(indicator.status()))
                .filter(indicator -> indicator.performanceScore() < threshold(indicator.name()))
                .map(indicator -> factor(indicator.name(), indicator.performanceScore()))
                .toList();

        return new RiskAssessment(
                score, score >= 60 ? "HIGH" : score >= 35 ? "MEDIUM" : "LOW",
                status, (int) indicators.stream()
                        .filter(indicator -> "AVAILABLE".equals(indicator.status()))
                        .count(),
                indicators.size(), indicators, factors);
    }

    private void add(List<IndicatorResult> indicators, String name, Double performance,
                     double weight, String missingExplanation) {
        if (performance == null) {
            indicators.add(new IndicatorResult(
                    name, 0, 0, weight, 0, "UNAVAILABLE", missingExplanation));
            return;
        }
        double normalized = clamp(performance);
        double risk = 100 - normalized;
        indicators.add(new IndicatorResult(
                name, round(normalized), round(risk), weight,
                round(risk * weight), "AVAILABLE", "Normalized to a 0-100 scale."));
    }

    private static double threshold(String name) {
        return switch (name) {
            case "attendance" -> 75;
            case "assessmentPerformance" -> 60;
            case "participation" -> 40;
            case "assignmentCompletion" -> 50;
            default -> 0;
        };
    }

    private static String factor(String name, double value) {
        return switch (name) {
            case "attendance" -> "Low attendance (" + round(value) + "%)";
            case "assessmentPerformance" ->
                    "Assessment performance needs improvement (" + round(value) + "%)";
            case "participation" -> "Low participation score (" + round(value) + "%)";
            case "assignmentCompletion" ->
                    "Low assignment completion (" + round(value) + "%)";
            default -> name;
        };
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(100, value));
    }

    static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
