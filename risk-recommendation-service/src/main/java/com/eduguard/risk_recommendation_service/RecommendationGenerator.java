package com.eduguard.risk_recommendation_service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
class RecommendationGenerator {

    List<Recommendation> generate(RiskAssessment assessment) {
        return generate(assessment, null);
    }

    List<Recommendation> generate(
            RiskAssessment assessment, ModelRiskPrediction modelPrediction) {
        if (modelPrediction != null) {
            return generateFromModel(modelPrediction);
        }
        return generateFromHeuristic(assessment);
    }

    private List<Recommendation> generateFromHeuristic(RiskAssessment assessment) {
        List<Recommendation> results = new ArrayList<>();
        for (IndicatorResult indicator : assessment.indicators()) {
            if (!"AVAILABLE".equals(indicator.status())
                    || indicator.performanceScore() >= threshold(indicator.name())) {
                continue;
            }
            String priority = indicator.riskScore() >= 60 ? "HIGH" : "MEDIUM";
            results.add(new Recommendation(
                    action(indicator.name()),
                    "Generated because " + indicator.name()
                            + " was " + format(indicator.performanceScore()) + "%.",
                    priority));
        }
        if (results.size() >= 2) {
            results.add(new Recommendation(
                    "Prioritize human review and coordinate appropriate student support.",
                    "Multiple indicators are below their monitoring thresholds.",
                    "HIGH"));
        }
        if (results.isEmpty()) {
            results.add(new Recommendation(
                    "Continue regular student check-ins and monitor performance trends.",
                    "No available indicator crossed a recommendation threshold.",
                    "LOW"));
        }
        return results;
    }

    private List<Recommendation> generateFromModel(ModelRiskPrediction prediction) {
        List<Recommendation> results = prediction.contributions().stream()
                .filter(contribution -> contribution.logOddsContribution() > 0)
                .limit(3)
                .map(contribution -> new Recommendation(
                        action(contribution.feature()),
                        "The fitted model associates this feature value ("
                                + format(contribution.value())
                                + "%) with a +" + formatContribution(
                                        contribution.logOddsContribution())
                                + " log-odds contribution relative to a synthetic "
                                + "training mean of "
                                + format(contribution.referenceMean())
                                + "%. This is an association, "
                                + "not a cause.",
                        prediction.tier().equals("HIGH") ? "HIGH" : "MEDIUM"))
                .toList();
        if (results.isEmpty()) {
            results = new ArrayList<>(List.of(new Recommendation(
                    "Continue regular student check-ins and monitor performance trends.",
                    "No feature has a positive model contribution relative to the "
                            + "synthetic training-set means.",
                    "LOW")));
        } else {
            results = new ArrayList<>(results);
        }
        if ("HIGH".equals(prediction.tier())) {
            results.add(new Recommendation(
                    "Review the available indicators with the student and coordinate "
                            + "appropriate supportive follow-up.",
                    "The model's synthetic-outcome probability falls in the high band; "
                            + "staff judgement is required.",
                    "HIGH"));
        }
        return results;
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

    private static String action(String name) {
        return switch (name) {
            case "attendance" -> "Discuss attendance barriers and create an attendance-improvement plan.";
            case "assessmentPerformance" -> "Recommend targeted academic support and a study plan.";
            case "participation" -> "Suggest a supportive check-in and an appropriate engagement activity.";
            case "assignmentCompletion" -> "Recommend assignment planning and milestone tracking.";
            default -> "Review the available performance indicator with the student.";
        };
    }

    private static String format(double value) {
        return String.valueOf(RiskAssessmentCalculator.round(value));
    }

    private static String formatContribution(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }
}
