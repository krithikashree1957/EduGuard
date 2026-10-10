package com.eduguard.risk_recommendation_service;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogisticRegressionRiskModelTest {

    private final LogisticRegressionRiskModel model =
            new LogisticRegressionRiskModel();

    @Test
    void trainsBundledExamplesAndProducesBoundedProbabilityAndTier() {
        assertEquals(80, model.trainingExampleCount());

        ModelRiskPrediction lowerRisk = model.predict(
                new PerformanceSnapshot(98.0, 98.0, 96.0, 96.0, true));
        ModelRiskPrediction higherRisk = model.predict(
                new PerformanceSnapshot(40.0, 40.0, 25.0, 30.0, true));

        assertTrue(lowerRisk.probability() >= 0 && lowerRisk.probability() <= 1);
        assertTrue(higherRisk.probability() >= 0 && higherRisk.probability() <= 1);
        assertTrue(higherRisk.probability() > lowerRisk.probability());
        assertEquals(
                LogisticRegressionRiskModel.tierForProbability(
                        higherRisk.probability()),
                higherRisk.tier());
    }

    @Test
    void assignsDocumentedProbabilityBands() {
        assertEquals("LOW",
                LogisticRegressionRiskModel.tierForProbability(0.349));
        assertEquals("MEDIUM",
                LogisticRegressionRiskModel.tierForProbability(0.35));
        assertEquals("HIGH",
                LogisticRegressionRiskModel.tierForProbability(0.60));
        assertThrows(IllegalArgumentException.class,
                () -> LogisticRegressionRiskModel.tierForProbability(Double.NaN));
    }

    @Test
    void exposesFittedCoefficientsAndInputSpecificLogOddsContributions() {
        ModelRiskPrediction lowAttendance = model.predict(
                new PerformanceSnapshot(50.0, 80.0, 70.0, 80.0, true));
        ModelRiskPrediction highAttendance = model.predict(
                new PerformanceSnapshot(90.0, 80.0, 70.0, 80.0, true));

        FeatureContribution lowAttendanceContribution =
                contributionFor(lowAttendance, "attendance");
        FeatureContribution highAttendanceContribution =
                contributionFor(highAttendance, "attendance");

        assertTrue(lowAttendanceContribution.coefficient() < 0);
        assertTrue(lowAttendanceContribution.logOddsContribution()
                > highAttendanceContribution.logOddsContribution());
        assertEquals(lowAttendanceContribution.coefficient()
                        * (lowAttendanceContribution.value()
                                - lowAttendanceContribution.referenceMean()) / 100.0,
                lowAttendanceContribution.logOddsContribution(), 1e-10);
    }

    @Test
    void rejectsMissingOutOfRangeAndInvalidTrainingFeatures() {
        IllegalArgumentException missingInput = assertThrows(
                IllegalArgumentException.class,
                () -> model.predict(
                        new PerformanceSnapshot(null, 80.0, 70.0, 80.0, true)));
        assertEquals(
                "All model features must be available as finite percentages "
                        + "from 0 to 100",
                missingInput.getMessage());
        assertThrows(IllegalArgumentException.class, () -> model.predict(
                new PerformanceSnapshot(101.0, 80.0, 70.0, 80.0, true)));
        assertThrows(IllegalArgumentException.class,
                () -> LogisticRegressionRiskModel.train(List.of(
                        new LogisticRegressionRiskModel.TrainingExample(
                                new double[] {80, 70, Double.NaN, 90}, 0),
                        new LogisticRegressionRiskModel.TrainingExample(
                                new double[] {40, 30, 20, 10}, 1),
                        new LogisticRegressionRiskModel.TrainingExample(
                                new double[] {85, 75, 65, 95}, 0),
                        new LogisticRegressionRiskModel.TrainingExample(
                                new double[] {35, 25, 15, 5}, 1))));
    }

    @Test
    void trainingExampleDefensivelyCopiesItsFeatureArray() {
        double[] source = {80, 70, 60, 90};
        LogisticRegressionRiskModel.TrainingExample example =
                new LogisticRegressionRiskModel.TrainingExample(source, 0);
        source[0] = 0;

        double[] exposed = example.features();
        exposed[1] = 0;

        assertArrayEquals(new double[] {80, 70, 60, 90}, example.features());
    }

    @Test
    void rejectsMalformedCsvRowsAndInvalidCsvValues() {
        assertThrows(IllegalStateException.class,
                () -> LogisticRegressionRiskModel.parseTrainingData(
                        new StringReader(header() + "\n80,70,60,90")));
        assertThrows(IllegalStateException.class,
                () -> LogisticRegressionRiskModel.parseTrainingData(
                        new StringReader(header() + "\n80,not-a-number,60,90,0")));
        assertThrows(IllegalStateException.class,
                () -> LogisticRegressionRiskModel.parseTrainingData(
                        new StringReader(header() + "\n101,70,60,90,0")));
        assertThrows(IllegalStateException.class,
                () -> LogisticRegressionRiskModel.parseTrainingData(
                        new StringReader(header() + "\n80,70,60,90,2")));
    }

    @Test
    void rejectsInsufficientAndSingleClassTrainingSets() {
        List<LogisticRegressionRiskModel.TrainingExample> twoExamples = List.of(
                trainingExample(80, 70, 60, 90, 0),
                trainingExample(40, 30, 20, 10, 1));
        assertThrows(IllegalArgumentException.class,
                () -> LogisticRegressionRiskModel.train(twoExamples));

        List<LogisticRegressionRiskModel.TrainingExample> singleClass = List.of(
                trainingExample(80, 70, 60, 90, 0),
                trainingExample(75, 65, 55, 85, 0),
                trainingExample(70, 60, 50, 80, 0),
                trainingExample(65, 55, 45, 75, 0));
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> LogisticRegressionRiskModel.train(singleClass));
        assertTrue(exception.getMessage().contains("both outcome labels"));
    }

    private static String header() {
        return "attendance,assessment,participation,assignments,label";
    }

    private static LogisticRegressionRiskModel.TrainingExample trainingExample(
            double attendance, double assessment, double participation,
            double assignments, int label) {
        return new LogisticRegressionRiskModel.TrainingExample(
                new double[] {attendance, assessment, participation, assignments},
                label);
    }

    private static FeatureContribution contributionFor(
            ModelRiskPrediction prediction, String feature) {
        return prediction.contributions().stream()
                .filter(item -> item.feature().equals(feature))
                .findFirst()
                .orElseThrow();
    }
}
