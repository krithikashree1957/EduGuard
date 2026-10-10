package com.eduguard.risk_recommendation_service;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/risk")
public class RiskAssessmentController {

    private final RestClient studentService;

    public RiskAssessmentController(RestClient.Builder builder) {
        this.studentService = builder
                .baseUrl("http://localhost:8080")
                .build();
    }

    @GetMapping("/students/{studentId}")
    public Map<String, Object> assessRisk(@PathVariable Long studentId) {
        Map<String, Object> data;

        try {
            data = studentService.get()
                    .uri("/api/students/{id}/performance", studentId)
                    .retrieve()
                    .body(Map.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Student not found");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Student service unavailable. Ensure it is running on port 8080.");
        }

        if (data == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Empty response from student service");
        }

        List<String> factors = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        double weightedRisk = 0;
        double availableWeight = 0;

        Double attendance = number(data.get("attendancePercentage"));
        if (attendance != null) {
            double value = clamp(attendance);
            weightedRisk += (100 - value) * 0.40;
            availableWeight += 0.40;

            if (value < 75) {
                factors.add("Low attendance (" + round(value) + "%)");
                recommendations.add("Schedule an attendance review and contact the student supportively.");
            } else if (value < 85) {
                factors.add("Attendance needs monitoring (" + round(value) + "%)");
                recommendations.add("Monitor attendance and discuss any barriers with the student.");
            }
        }

        Object rawAssessments = data.get("assessments");
        if (rawAssessments instanceof List<?> assessments && !assessments.isEmpty()) {
            double total = 0;
            int count = 0;

            for (Object item : assessments) {
                if (item instanceof Map<?, ?> assessment) {
                    Double percentage = number(assessment.get("percentage"));
                    if (percentage != null) {
                        total += clamp(percentage);
                        count++;
                    }
                }
            }

            if (count > 0) {
                double average = total / count;
                weightedRisk += (100 - average) * 0.30;
                availableWeight += 0.30;

                if (average < 40) {
                    factors.add("Low assessment average (" + round(average) + "%)");
                    recommendations.add("Arrange academic mentoring and a targeted study plan.");
                } else if (average < 60) {
                    factors.add("Assessment performance needs improvement (" + round(average) + "%)");
                    recommendations.add("Offer subject-specific support and review progress.");
                }
            }
        }

        Double participation = number(data.get("participationScore"));
        if (participation != null) {
            participation = normalize(participation);
            weightedRisk += (100 - participation) * 0.15;
            availableWeight += 0.15;

            if (participation < 40) {
                factors.add("Low participation score (" + round(participation) + "%)");
                recommendations.add("Arrange a check-in to understand barriers to classroom participation.");
            }
        }

        Double assignments = number(data.get("assignmentCompletionRate"));
        if (assignments != null) {
            assignments = normalize(assignments);
            weightedRisk += (100 - assignments) * 0.15;
            availableWeight += 0.15;

            if (assignments < 50) {
                factors.add("Low assignment completion (" + round(assignments) + "%)");
                recommendations.add("Create a manageable assignment catch-up plan with regular follow-up.");
            }
        }

        if (availableWeight == 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "No usable performance data is available for risk assessment.");
        }

        double score = round(weightedRisk / availableWeight);
        String tier = score >= 60 ? "HIGH" : score >= 35 ? "MEDIUM" : "LOW";

        if (factors.isEmpty()) {
            factors.add("No elevated risk factors identified in the available data.");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("Continue regular student check-ins and monitor performance trends.");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", studentId);
        result.put("riskScore", score);
        result.put("riskTier", tier);
        result.put("contributingFactors", factors);
        result.put("recommendedInterventions", recommendations);
        result.put("modelNote",
                "Explainable weighted heuristic; not a trained ML model. Supports human review, not a definitive prediction.");
        return result;
    }

    private static Double number(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        return null;
    }

    private static double normalize(double value) {
        if (value >= 0 && value <= 1) {
            return value * 100;
        }
        return clamp(value);
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(100, value));
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
