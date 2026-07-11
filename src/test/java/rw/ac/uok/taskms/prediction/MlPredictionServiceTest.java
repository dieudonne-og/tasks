package rw.ac.uok.taskms.prediction;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import rw.ac.uok.taskms.task.Complexity;
import rw.ac.uok.taskms.task.Task;
import rw.ac.uok.taskms.task.TaskType;
import rw.ac.uok.taskms.task.TaskTypeRepository;
import rw.ac.uok.taskms.user.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against the seeded H2 database (60 synthetic completed tasks &gt; training
 * threshold), so both models are trained at startup.
 */
@SpringBootTest
class MlPredictionServiceTest {

    @Autowired
    MlPredictionService predictionService;
    @Autowired
    ModelMetricsRepository metricsRepository;
    @Autowired
    TaskTypeRepository taskTypeRepository;
    @Autowired
    UserRepository userRepository;

    @Test
    void trainsBothModelsAndSelectsOneActive() {
        List<ModelMetrics> metrics = predictionService.train();

        assertThat(metrics).extracting(ModelMetrics::getModelType)
                .contains(ModelType.LINEAR_REGRESSION, ModelType.RANDOM_FOREST);

        long active = metricsRepository.findByActiveTrue().size();
        assertThat(active).isEqualTo(1);
        assertThat(predictionService.isModelActive()).isTrue();

        // Every metric should be a finite, non-negative error value.
        metrics.forEach(m -> {
            assertThat(m.getMae()).isNotNaN().isGreaterThanOrEqualTo(0);
            assertThat(m.getRmse()).isNotNaN().isGreaterThanOrEqualTo(0);
        });
    }

    @Test
    void predictionYieldsPositiveValueWithinConfidenceInterval() {
        TaskType type = taskTypeRepository.findAll().get(0);
        Task task = new Task();
        task.setTaskType(type);
        task.setAssignee(userRepository.findAll().get(2));
        task.setComplexity(Complexity.HIGH);

        PredictionResult result = predictionService.predict(task);

        assertThat(result.predictedDays()).isGreaterThan(0);
        assertThat(result.lowerDays()).isLessThanOrEqualTo(result.predictedDays());
        assertThat(result.upperDays()).isGreaterThanOrEqualTo(result.predictedDays());
        assertThat(result.model()).isIn(ModelType.LINEAR_REGRESSION, ModelType.RANDOM_FOREST);
    }
}
