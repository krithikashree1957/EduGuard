package com.eduguard.risk_recommendation_service;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
class LogisticRegressionRiskModel {
    static final String OUTCOME = "synthetic_support_need";
    private static final String[] FEATURES = {
            "attendance", "assessmentPerformance", "participation",
            "assignmentCompletion"
    };
    private static final int ITERATIONS = 20_000;
    private static final double LEARNING_RATE = 0.2;
    private static final double L2_PENALTY = 0.001;

    private final double intercept;
    private final double[] coefficients;
    private final double[] featureMeans;
    private final int trainingExampleCount;

    LogisticRegressionRiskModel() {
        this(train(readBundledExamples()));
    }

    private LogisticRegressionRiskModel(LogisticRegressionRiskModel trained) {
        this.intercept = trained.intercept;
        this.coefficients = trained.coefficients.clone();
        this.featureMeans = trained.featureMeans.clone();
        this.trainingExampleCount = trained.trainingExampleCount;
    }

    private LogisticRegressionRiskModel(
            double intercept, double[] coefficients, double[] featureMeans,
            int trainingExampleCount) {
        this.intercept = intercept;
        this.coefficients = coefficients;
        this.featureMeans = featureMeans;
        this.trainingExampleCount = trainingExampleCount;
    }

    ModelRiskPrediction predict(PerformanceSnapshot performance) {
        double[] values = values(performance);
        double linearScore = intercept;
        for (int index = 0; index < values.length; index++) {
            linearScore += coefficients[index] * values[index];
        }
        double probability = sigmoid(linearScore);

        List<FeatureContribution> contributions = new ArrayList<>();
        for (int index = 0; index < values.length; index++) {
            double contribution = coefficients[index]
                    * (values[index] - featureMeans[index]);
            contributions.add(new FeatureContribution(
                    FEATURES[index], values[index] * 100.0,
                    coefficients[index], featureMeans[index] * 100.0,
                    contribution));
        }
        contributions.sort((left, right) -> Double.compare(
                right.logOddsContribution(), left.logOddsContribution()));

        return new ModelRiskPrediction(
                probability, tierForProbability(probability), List.copyOf(contributions));
    }

    int trainingExampleCount() {
        return trainingExampleCount;
    }

    static LogisticRegressionRiskModel train(List<TrainingExample> examples) {
        if (examples == null || examples.size() < 4) {
            throw new IllegalArgumentException(
                    "At least four labelled training examples are required");
        }

        double[][] normalizedFeatures =
                new double[examples.size()][FEATURES.length];
        int[] labels = new int[examples.size()];
        double positiveCount = 0;
        double[] means = new double[FEATURES.length];
        for (int row = 0; row < examples.size(); row++) {
            TrainingExample example = examples.get(row);
            double[] features = validatedFeatures(example);
            labels[row] = example.label();
            positiveCount += example.label();
            for (int feature = 0; feature < FEATURES.length; feature++) {
                normalizedFeatures[row][feature] = features[feature] / 100.0;
                means[feature] += normalizedFeatures[row][feature];
            }
        }
        if (positiveCount == 0 || positiveCount == examples.size()) {
            throw new IllegalArgumentException(
                    "Training data must contain both outcome labels");
        }
        for (int feature = 0; feature < means.length; feature++) {
            means[feature] /= examples.size();
        }

        double intercept = 0;
        double[] coefficients = new double[FEATURES.length];
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            double interceptGradient = 0;
            double[] coefficientGradients = new double[FEATURES.length];
            for (int row = 0; row < normalizedFeatures.length; row++) {
                double linearScore = intercept;
                for (int feature = 0; feature < FEATURES.length; feature++) {
                    linearScore += coefficients[feature]
                            * normalizedFeatures[row][feature];
                }
                double error = sigmoid(linearScore) - labels[row];
                interceptGradient += error;
                for (int feature = 0; feature < FEATURES.length; feature++) {
                    coefficientGradients[feature] += error
                            * normalizedFeatures[row][feature];
                }
            }

            double divisor = examples.size();
            intercept -= LEARNING_RATE * interceptGradient / divisor;
            for (int feature = 0; feature < FEATURES.length; feature++) {
                double gradient = coefficientGradients[feature] / divisor
                        + L2_PENALTY * coefficients[feature];
                coefficients[feature] -= LEARNING_RATE * gradient;
            }
        }
        return new LogisticRegressionRiskModel(
                intercept, coefficients, means, examples.size());
    }

    static String tierForProbability(double probability) {
        if (!Double.isFinite(probability) || probability < 0 || probability > 1) {
            throw new IllegalArgumentException("Probability must be between 0 and 1");
        }
        return probability >= 0.60 ? "HIGH"
                : probability >= 0.35 ? "MEDIUM" : "LOW";
    }

    private static List<TrainingExample> readBundledExamples() {
        InputStream resource = LogisticRegressionRiskModel.class
                .getResourceAsStream("/ml/risk-training-data.csv");
        if (resource == null) {
            throw new IllegalStateException(
                    "Bundled logistic-regression training data is missing");
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource, StandardCharsets.UTF_8))) {
            return parseTrainingData(reader);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to read bundled logistic-regression training data", exception);
        }
    }

    static List<TrainingExample> parseTrainingData(Reader source) {
        if (source == null) {
            throw new IllegalArgumentException("Training data source is required");
        }
        List<TrainingExample> examples = new ArrayList<>();
        boolean headerFound = false;
        try (BufferedReader reader = source instanceof BufferedReader bufferedReader
                ? bufferedReader : new BufferedReader(source)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                if (!headerFound) {
                    if (!"attendance,assessment,participation,assignments,label"
                            .equals(line.trim())) {
                        throw new IllegalStateException(
                                "Training data has an unexpected CSV header");
                    }
                    headerFound = true;
                    continue;
                }

                String[] fields = line.split(",", -1);
                if (fields.length != FEATURES.length + 1) {
                    throw new IllegalStateException(
                            "Training data contains a malformed row at line "
                                    + lineNumber);
                }
                try {
                    TrainingExample example = new TrainingExample(
                            new double[] {
                                    Double.parseDouble(fields[0].trim()),
                                    Double.parseDouble(fields[1].trim()),
                                    Double.parseDouble(fields[2].trim()),
                                    Double.parseDouble(fields[3].trim())
                            },
                            Integer.parseInt(fields[4].trim()));
                    try {
                        validatedFeatures(example);
                    } catch (IllegalArgumentException exception) {
                        throw new IllegalStateException(
                                "Training data contains an invalid feature or label "
                                        + "at line " + lineNumber,
                                exception);
                    }
                    examples.add(example);
                } catch (NumberFormatException exception) {
                    throw new IllegalStateException(
                            "Training data contains a non-numeric value at line "
                                    + lineNumber,
                            exception);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to read logistic-regression training data", exception);
        }
        if (!headerFound) {
            throw new IllegalStateException("Training data header is missing");
        }
        return List.copyOf(examples);
    }

    private static double[] validatedFeatures(TrainingExample example) {
        if (example == null) {
            throw new IllegalArgumentException("Training example is malformed");
        }
        double[] features = example.features();
        if (features == null || features.length != FEATURES.length
                || (example.label() != 0 && example.label() != 1)) {
            throw new IllegalArgumentException("Training example is malformed");
        }
        for (double feature : features) {
            if (!Double.isFinite(feature) || feature < 0 || feature > 100) {
                throw new IllegalArgumentException(
                        "Training features must be finite percentages from 0 to 100");
            }
        }
        return features;
    }

    private static double[] values(PerformanceSnapshot performance) {
        if (performance == null) {
            throw new IllegalArgumentException("Performance data is required");
        }
        Double[] raw = {
                performance.attendance(),
                performance.assessmentAverage(),
                performance.participation(),
                performance.assignmentCompletion()
        };
        double[] values = new double[raw.length];
        for (int index = 0; index < raw.length; index++) {
            if (raw[index] == null || !Double.isFinite(raw[index])
                    || raw[index] < 0 || raw[index] > 100) {
                throw new IllegalArgumentException(
                    "All model features must be available as finite percentages "
                            + "from 0 to 100");
            }
            values[index] = raw[index] / 100.0;
        }
        return values;
    }

    private static double sigmoid(double value) {
        double bounded = Math.max(-35, Math.min(35, value));
        return 1.0 / (1.0 + Math.exp(-bounded));
    }

    record TrainingExample(double[] features, int label) {
        TrainingExample {
            features = features == null ? null : features.clone();
        }

        @Override
        public double[] features() {
            return features == null ? null : features.clone();
        }
    }
}

record ModelRiskPrediction(
        double probability,
        String tier,
        List<FeatureContribution> contributions) {
}

record FeatureContribution(
        String feature,
        double value,
        double coefficient,
        double referenceMean,
        double logOddsContribution) {
}
