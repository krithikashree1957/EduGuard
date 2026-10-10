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
import java.util.List;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/risk")
@CrossOrigin(origins = "http://localhost:8080")
public class RiskAssessmentController {

    private final StudentPerformanceClient performanceClient;
    private final RiskAssessmentCalculator calculator;
    private final RecommendationGenerator recommendationGenerator;
    private final LogisticRegressionRiskModel riskModel;

    public RiskAssessmentController(
            StudentPerformanceClient performanceClient,
            RiskAssessmentCalculator calculator,
            RecommendationGenerator recommendationGenerator,
            LogisticRegressionRiskModel riskModel) {
        this.performanceClient = performanceClient;
        this.calculator = calculator;
        this.recommendationGenerator = recommendationGenerator;
        this.riskModel = riskModel;
    }

    @GetMapping("/students/{studentId}")
    public Map<String, Object> assessRisk(@PathVariable @Positive Long studentId) {
        PerformanceSnapshot performance = performanceClient.fetch(studentId);
        RiskAssessment assessment = calculator.calculate(studentId, performance);
        ModelRiskPrediction modelPrediction = hasValidFeatures(performance)
                ? riskModel.predict(performance) : null;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", studentId);
        result.put("riskScore", assessment.score());
        result.put("riskTier", assessment.tier());
        result.put("assessmentStatus", assessment.status());
        result.put("availableIndicatorCount", assessment.availableIndicatorCount());
        result.put("totalIndicatorCount", assessment.totalIndicatorCount());
        result.put("indicators", assessment.indicators());
        result.put("contributingFactors", assessment.factors());
        result.put("riskModel", "LOGISTIC_REGRESSION");
        result.put("modelOutcome", LogisticRegressionRiskModel.OUTCOME);
        result.put("modelProbability",
                modelPrediction == null ? null : modelPrediction.probability());
        result.put("modelRiskTier",
                modelPrediction == null ? "UNAVAILABLE" : modelPrediction.tier());
        result.put("modelContributingFactors",
                modelPrediction == null ? List.of() : modelPrediction.contributions());
        var recommendations = recommendationGenerator.generate(
                assessment, modelPrediction);
        result.put("recommendedInterventions", recommendations.stream()
                .map(Recommendation::recommendation)
                .toList());
        result.put("recommendations", recommendations);
        result.put("modelNote", modelPrediction == null
                ? "The synthetic-data logistic-regression model requires all four "
                        + "performance indicators; the legacy heuristic remains available. "
                        + "Neither result is a real-world dropout prediction."
                : "The probability estimates the synthetic support-need label learned "
                        + "from bundled synthetic data, not real-world dropout. "
                        + "Feature contributions are fitted-model log-odds differences "
                        + "from synthetic training-set means. Use only for supportive "
                        + "human review, not as a decision.");
        return result;
    }

    private static boolean hasValidFeatures(PerformanceSnapshot performance) {
        return isValidPercentage(performance.attendance())
                && isValidPercentage(performance.assessmentAverage())
                && isValidPercentage(performance.participation())
                && isValidPercentage(performance.assignmentCompletion());
    }

    private static boolean isValidPercentage(Double value) {
        return value != null && Double.isFinite(value) && value >= 0 && value <= 100;
    }
}
