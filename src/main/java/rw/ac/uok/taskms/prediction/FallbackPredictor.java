package rw.ac.uok.taskms.prediction;

import org.springframework.stereotype.Component;
import rw.ac.uok.taskms.task.Task;
import rw.ac.uok.taskms.task.TaskRepository;
import rw.ac.uok.taskms.task.TaskStatus;

import java.util.DoubleSummaryStatistics;
import java.util.List;

/**
 * Category-average baseline used when there is not enough completed-task history
 * to train the ML models (research limitation 1.7.1 — cold start).
 * Returns the average actual duration for the task's type, with a confidence
 * interval derived from that group's standard deviation.
 */
@Component
public class FallbackPredictor implements DurationPredictor {

    private static final double DEFAULT_DURATION_DAYS = 3.0;

    private final TaskRepository taskRepository;

    public FallbackPredictor(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public PredictionResult predict(Task task) {
        List<Task> done = taskRepository.findByStatus(TaskStatus.DONE).stream()
                .filter(t -> t.getActualDurationDays() != null)
                .toList();

        Long typeId = task.getTaskType() != null ? task.getTaskType().getId() : null;

        List<Double> sameType = done.stream()
                .filter(t -> typeId != null && t.getTaskType() != null
                        && typeId.equals(t.getTaskType().getId()))
                .map(Task::getActualDurationDays)
                .toList();

        List<Double> sample = sameType.isEmpty()
                ? done.stream().map(Task::getActualDurationDays).toList()
                : sameType;

        if (sample.isEmpty()) {
            return new PredictionResult(DEFAULT_DURATION_DAYS,
                    Math.max(0, DEFAULT_DURATION_DAYS - 1),
                    DEFAULT_DURATION_DAYS + 1, ModelType.CATEGORY_AVERAGE);
        }

        DoubleSummaryStatistics stats = sample.stream().mapToDouble(Double::doubleValue).summaryStatistics();
        double mean = stats.getAverage();
        double std = standardDeviation(sample, mean);
        double margin = 1.96 * std;
        return new PredictionResult(
                round(mean),
                round(Math.max(0, mean - margin)),
                round(mean + margin),
                ModelType.CATEGORY_AVERAGE);
    }

    private double standardDeviation(List<Double> values, double mean) {
        if (values.size() < 2) {
            return 0.0;
        }
        double sumSq = values.stream().mapToDouble(v -> (v - mean) * (v - mean)).sum();
        return Math.sqrt(sumSq / (values.size() - 1));
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
