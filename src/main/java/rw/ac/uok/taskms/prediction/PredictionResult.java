package rw.ac.uok.taskms.prediction;

/**
 * A duration prediction with a confidence interval (section 2.1.7).
 *
 * @param predictedDays point estimate of how many working days the task will take
 * @param lowerDays     lower bound of the confidence interval (clamped at 0)
 * @param upperDays     upper bound of the confidence interval
 * @param model         which predictor produced this result
 */
public record PredictionResult(double predictedDays, double lowerDays, double upperDays, String model) {

    public static PredictionResult of(double predicted, double margin, String model) {
        double clampedPredicted = Math.max(0.5, predicted);
        double lower = Math.max(0, clampedPredicted - margin);
        double upper = clampedPredicted + margin;
        return new PredictionResult(round(clampedPredicted), round(lower), round(upper), model);
    }

    private static double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
