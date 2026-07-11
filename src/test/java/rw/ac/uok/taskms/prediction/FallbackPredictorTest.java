package rw.ac.uok.taskms.prediction;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackPredictorTest {

    @Test
    void usesCategoryAverageForKnownType() {
        List<TrainingSample> history = List.of(
                new TrainingSample("Advertise vacancy", "1", 2, 4),
                new TrainingSample("Advertise vacancy", "2", 2, 6),
                new TrainingSample("Advertise vacancy", "1", 3, 5),
                new TrainingSample("Leave application", "1", 1, 1));

        PredictionResult result = FallbackPredictor.predict("Advertise vacancy", history, 1.96);

        // Average of 4, 6, 5 = 5
        assertThat(result.predictedDays()).isEqualTo(5.0);
        assertThat(result.model()).isEqualTo(ModelType.FALLBACK_AVERAGE.name());
        assertThat(result.lowerDays()).isLessThan(result.predictedDays());
        assertThat(result.upperDays()).isGreaterThan(result.predictedDays());
    }

    @Test
    void fallsBackToGlobalAverageForUnknownType() {
        List<TrainingSample> history = List.of(
                new TrainingSample("Advertise vacancy", "1", 2, 4),
                new TrainingSample("Advertise vacancy", "2", 2, 6));

        PredictionResult result = FallbackPredictor.predict("Brand New Type", history, 1.96);

        // Global average of 4 and 6 = 5
        assertThat(result.predictedDays()).isEqualTo(5.0);
    }

    @Test
    void handlesEmptyHistoryWithSafeDefault() {
        PredictionResult result = FallbackPredictor.predict("Anything", List.of(), 1.96);

        assertThat(result.predictedDays()).isGreaterThan(0);
        assertThat(result.lowerDays()).isGreaterThanOrEqualTo(0);
        assertThat(result.upperDays()).isGreaterThan(result.lowerDays());
    }
}
