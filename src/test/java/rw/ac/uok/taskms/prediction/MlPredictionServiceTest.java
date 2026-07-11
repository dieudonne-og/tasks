package rw.ac.uok.taskms.prediction;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class MlPredictionServiceTest {

    @Test
    void trainsBothModelsSelectsActiveAndPredicts() {
        MlPredictionService service = new MlPredictionService();

        List<String> types = List.of("Advertise vacancy", "Leave application", "Performance appraisal");
        List<String> assignees = List.of("1", "2", "3");
        List<TrainingSample> samples = syntheticSamples(types, assignees, 150);

        List<ModelMetrics> metrics = service.trainAndDeploy(samples, types, assignees, 1.96);

        // Both linear regression and random forest were evaluated.
        assertThat(metrics).hasSize(2);
        assertThat(metrics).extracting(ModelMetrics::getModelType)
                .containsExactlyInAnyOrder(ModelType.LINEAR, ModelType.RANDOM_FOREST);
        // Exactly one model is deployed as active.
        assertThat(metrics.stream().filter(ModelMetrics::isActive).count()).isEqualTo(1);
        assertThat(service.isTrained()).isTrue();
        assertThat(service.activeModel()).isPresent();

        // A prediction is produced with a valid confidence interval.
        var prediction = service.predict("Advertise vacancy", "1", 2, 1.96);
        assertThat(prediction).isPresent();
        assertThat(prediction.get().predictedDays()).isGreaterThan(0);
        assertThat(prediction.get().lowerDays()).isLessThanOrEqualTo(prediction.get().predictedDays());
        assertThat(prediction.get().upperDays()).isGreaterThanOrEqualTo(prediction.get().predictedDays());
    }

    @Test
    void returnsEmptyWhenNotTrained() {
        MlPredictionService service = new MlPredictionService();
        assertThat(service.isTrained()).isFalse();
        assertThat(service.predict("Anything", "1", 2, 1.96)).isEmpty();
    }

    /** Duration driven by a learnable rule: base(type) * complexity + assignee offset + small noise. */
    private List<TrainingSample> syntheticSamples(List<String> types, List<String> assignees, int n) {
        Random random = new Random(7);
        List<TrainingSample> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String type = types.get(random.nextInt(types.size()));
            String assignee = assignees.get(random.nextInt(assignees.size()));
            int complexity = 1 + random.nextInt(3);
            int base = switch (type) {
                case "Advertise vacancy" -> 5;
                case "Leave application" -> 2;
                default -> 6;
            };
            int assigneeOffset = Integer.parseInt(assignee) - 1;
            int duration = Math.max(1, (int) Math.round(base * (0.6 + 0.3 * complexity)
                    + assigneeOffset + random.nextGaussian() * 0.5));
            out.add(new TrainingSample(type, assignee, complexity, duration));
        }
        return out;
    }
}
