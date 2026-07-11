package rw.ac.uok.taskms.prediction;

import java.util.List;

/**
 * Category-average predictor used until enough history has accumulated to train
 * a model (Limitation 1.7.1). It predicts a new task's duration as the mean
 * actual duration of completed tasks of the same type, falling back to the
 * global mean when the type is unseen, and expresses uncertainty as the
 * standard deviation of that group.
 *
 * <p>Pure functions with no Spring/Weka/DB dependencies, so they are easy to
 * unit-test.
 */
public final class FallbackPredictor {

    private FallbackPredictor() {
    }

    /** Sensible default when there is no history at all. */
    private static final double DEFAULT_DURATION = 3.0;

    public static PredictionResult predict(String taskType, List<TrainingSample> history, double z) {
        List<Integer> sameType = history.stream()
                .filter(s -> s.taskType().equalsIgnoreCase(taskType))
                .map(TrainingSample::durationDays)
                .toList();

        List<Integer> basis = sameType.size() >= 2
                ? sameType
                : history.stream().map(TrainingSample::durationDays).toList();

        if (basis.isEmpty()) {
            return PredictionResult.of(DEFAULT_DURATION, DEFAULT_DURATION, ModelType.FALLBACK_AVERAGE.name());
        }

        double mean = basis.stream().mapToInt(Integer::intValue).average().orElse(DEFAULT_DURATION);
        double std = standardDeviation(basis, mean);
        // Half-width of the interval: at least one day so it is never degenerate.
        double margin = Math.max(1.0, z * std);
        return PredictionResult.of(mean, margin, ModelType.FALLBACK_AVERAGE.name());
    }

    private static double standardDeviation(List<Integer> values, double mean) {
        if (values.size() < 2) {
            return 0.0;
        }
        double sumSq = 0.0;
        for (int v : values) {
            double d = v - mean;
            sumSq += d * d;
        }
        return Math.sqrt(sumSq / (values.size() - 1));
    }
}
