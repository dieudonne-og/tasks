package rw.ac.uok.taskms.prediction;

public record PredictionResult(
        double predictedDays,
        double lowerDays,
        double upperDays,
        ModelType model) {
}
