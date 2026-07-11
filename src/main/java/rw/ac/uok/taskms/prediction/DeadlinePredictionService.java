package rw.ac.uok.taskms.prediction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.uok.taskms.task.Complexity;
import rw.ac.uok.taskms.task.Task;
import rw.ac.uok.taskms.task.TaskRepository;
import rw.ac.uok.taskms.task.TaskStatus;
import rw.ac.uok.taskms.task.TaskType;
import rw.ac.uok.taskms.task.TaskTypeRepository;
import rw.ac.uok.taskms.user.User;
import rw.ac.uok.taskms.user.UserRepository;

import java.util.List;

/**
 * Facade over the prediction subsystem. Decides between the trained model and
 * the category-average fallback, applies predictions to tasks, and manages
 * (re)training as history accumulates (Objective 3, Limitation 1.7.1).
 */
@Service
public class DeadlinePredictionService {

    private static final Logger log = LoggerFactory.getLogger(DeadlinePredictionService.class);

    private final TaskRepository taskRepository;
    private final TaskTypeRepository taskTypeRepository;
    private final UserRepository userRepository;
    private final ModelMetricsRepository metricsRepository;
    private final MlPredictionService ml;

    private final int minTrainingSamples;
    private final int retrainEveryCompleted;
    private final double confidenceZ;

    private volatile int completedAtLastTraining = 0;

    public DeadlinePredictionService(TaskRepository taskRepository,
                                     TaskTypeRepository taskTypeRepository,
                                     UserRepository userRepository,
                                     ModelMetricsRepository metricsRepository,
                                     MlPredictionService ml,
                                     @Value("${taskms.prediction.min-training-samples}") int minTrainingSamples,
                                     @Value("${taskms.prediction.retrain-every-completed}") int retrainEveryCompleted,
                                     @Value("${taskms.prediction.confidence-z}") double confidenceZ) {
        this.taskRepository = taskRepository;
        this.taskTypeRepository = taskTypeRepository;
        this.userRepository = userRepository;
        this.metricsRepository = metricsRepository;
        this.ml = ml;
        this.minTrainingSamples = minTrainingSamples;
        this.retrainEveryCompleted = retrainEveryCompleted;
        this.confidenceZ = confidenceZ;
    }

    /** Predicts duration for the given attributes, using the model when available. */
    public PredictionResult predictFor(TaskType type, User assignee, Complexity complexity) {
        String typeName = type.getName();
        String assigneeKey = String.valueOf(assignee.getId());
        if (ml.isTrained()) {
            var mlResult = ml.predict(typeName, assigneeKey, complexity.level(), confidenceZ);
            if (mlResult.isPresent()) {
                return mlResult.get();
            }
        }
        return FallbackPredictor.predict(typeName, loadSamples(), confidenceZ);
    }

    /** Computes and stores the prediction fields on a task. */
    public void applyPrediction(Task task) {
        PredictionResult result = predictFor(task.getTaskType(), task.getAssignee(), task.getComplexity());
        task.setPredictedDurationDays(result.predictedDays());
        task.setPredictedLowerDays(result.lowerDays());
        task.setPredictedUpperDays(result.upperDays());
        task.setPredictionModel(result.model());
    }

    /**
     * Retrains the models from the current completed-task history and persists
     * fresh metrics. When history is below the configured minimum it clears the
     * model so predictions use the fallback.
     */
    @Transactional
    public List<ModelMetrics> retrain() {
        List<TrainingSample> samples = loadSamples();
        completedAtLastTraining = samples.size();

        if (samples.size() < minTrainingSamples) {
            log.info("Only {} completed tasks (< {} required); using category-average fallback",
                    samples.size(), minTrainingSamples);
            // Clear any previously deployed model by training on an empty set.
            ml.trainAndDeploy(List.of(), List.of(), List.of(), confidenceZ);
            return List.of();
        }

        List<String> typeNames = taskTypeRepository.findAll().stream().map(TaskType::getName).toList();
        List<String> assigneeKeys = userRepository.findAll().stream()
                .map(u -> String.valueOf(u.getId())).toList();

        List<ModelMetrics> metrics = ml.trainAndDeploy(samples, typeNames, assigneeKeys, confidenceZ);
        if (!metrics.isEmpty()) {
            persistMetrics(metrics);
        }
        return metrics;
    }

    /** Called after a task is marked complete; retrains periodically. */
    public void onTaskCompleted() {
        long completed = taskRepository.countByStatus(TaskStatus.DONE);
        if (!ml.isTrained() && completed >= minTrainingSamples) {
            retrain();
        } else if (completed - completedAtLastTraining >= retrainEveryCompleted) {
            retrain();
        }
    }

    private void persistMetrics(List<ModelMetrics> metrics) {
        metricsRepository.findByActiveTrue().forEach(m -> {
            m.setActive(false);
            metricsRepository.save(m);
        });
        metricsRepository.saveAll(metrics);
    }

    private List<TrainingSample> loadSamples() {
        return taskRepository.findByStatus(TaskStatus.DONE).stream()
                .filter(t -> t.getActualDurationDays() != null && t.getActualDurationDays() > 0)
                .map(t -> new TrainingSample(
                        t.getTaskType().getName(),
                        String.valueOf(t.getAssignee().getId()),
                        t.getComplexity().level(),
                        t.getActualDurationDays()))
                .toList();
    }
}
