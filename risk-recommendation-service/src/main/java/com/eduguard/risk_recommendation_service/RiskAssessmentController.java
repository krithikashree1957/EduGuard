package com.eduguard.risk_recommendation_service;

import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/risk")
@CrossOrigin(origins = "http://localhost:8080")
public class RiskAssessmentController {

    private final StudentPerformanceClient performanceClient;
    private final RiskAssessmentCalculator calculator;
    private final RecommendationGenerator recommendationGenerator;

    public RiskAssessmentController(
            StudentPerformanceClient performanceClient,
            RiskAssessmentCalculator calculator,
            RecommendationGenerator recommendationGenerator) {
        this.performanceClient = performanceClient;
        this.calculator = calculator;
        this.recommendationGenerator = recommendationGenerator;
    }

    @GetMapping("/students/{studentId}")
    public Map<String, Object> assessRisk(@PathVariable @Positive Long studentId) {
        PerformanceSnapshot performance = performanceClient.fetch(studentId);
        RiskAssessment assessment = calculator.calculate(studentId, performance);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", studentId);
        result.put("riskScore", assessment.score());
        result.put("riskTier", assessment.tier());
        result.put("assessmentStatus", assessment.status());
        result.put("availableIndicatorCount", assessment.availableIndicatorCount());
        result.put("totalIndicatorCount", assessment.totalIndicatorCount());
        result.put("indicators", assessment.indicators());
        result.put("contributingFactors", assessment.factors());
        var recommendations = recommendationGenerator.generate(assessment);
        result.put("recommendedInterventions", recommendations.stream()
                .map(Recommendation::recommendation)
                .toList());
        result.put("recommendations", recommendations);
        result.put("modelNote",
                "Explainable weighted heuristic; not a trained ML model. "
                        + "Supports human review, not a definitive prediction.");
        return result;
    }
}
