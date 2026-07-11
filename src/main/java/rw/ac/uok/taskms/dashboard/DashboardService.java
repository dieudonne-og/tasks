package rw.ac.uok.taskms.dashboard;

import org.springframework.stereotype.Service;
import rw.ac.uok.taskms.prediction.ModelMetrics;
import rw.ac.uok.taskms.prediction.ModelMetricsRepository;
import rw.ac.uok.taskms.task.Task;
import rw.ac.uok.taskms.task.TaskRepository;
import rw.ac.uok.taskms.task.TaskStatus;
import rw.ac.uok.taskms.task.dto.TaskDto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Aggregates the metrics presented on the reporting dashboard (Objective 4):
 * estimate-vs-actual accuracy, on-time completion, task distribution, delay
 * risk, and the active model's accuracy.
 */
@Service
public class DashboardService {

    private final TaskRepository taskRepository;
    private final ModelMetricsRepository metricsRepository;

    public DashboardService(TaskRepository taskRepository, ModelMetricsRepository metricsRepository) {
        this.taskRepository = taskRepository;
        this.metricsRepository = metricsRepository;
    }

    public Map<String, Object> summary() {
        List<Task> all = taskRepository.findAll();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalTasks", all.size());
        result.put("statusCounts", statusCounts());
        result.put("typeCounts", typeCounts(all));
        result.put("openAtRisk", openAtRiskCount(all));
        result.put("onTimeRate", onTimeRate(all));
        result.put("accuracy", estimateVsActual(all));
        result.put("model", activeModel());
        return result;
    }

    private Map<String, Long> statusCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (TaskStatus s : TaskStatus.values()) {
            counts.put(s.name(), taskRepository.countByStatus(s));
        }
        return counts;
    }

    private Map<String, Long> typeCounts(List<Task> all) {
        Map<String, Long> counts = new TreeMap<>();
        for (Task t : all) {
            counts.merge(t.getTaskType().getName(), 1L, Long::sum);
        }
        return counts;
    }

    private long openAtRiskCount(List<Task> all) {
        return all.stream()
                .filter(t -> t.getStatus().isOpen())
                .map(TaskDto::from)
                .filter(TaskDto::atRisk)
                .count();
    }

    /** Fraction of completed tasks finished on or before their deadline. */
    private Map<String, Object> onTimeRate(List<Task> all) {
        List<Task> completed = all.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE && t.getCompletedDate() != null && t.getDueDate() != null)
                .toList();
        long onTime = completed.stream()
                .filter(t -> !t.getCompletedDate().isAfter(t.getDueDate()))
                .count();
        double rate = completed.isEmpty() ? 0 : (double) onTime / completed.size();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("completed", completed.size());
        m.put("onTime", onTime);
        m.put("rate", round(rate));
        return m;
    }

    /**
     * Compares the mean absolute error of the users' manual estimates against
     * the model's predictions on completed tasks — the core evidence that
     * data-driven prediction beats subjective estimation (Objective 4).
     */
    private Map<String, Object> estimateVsActual(List<Task> all) {
        double manualErrSum = 0;
        int manualN = 0;
        double modelErrSum = 0;
        int modelN = 0;
        for (Task t : all) {
            if (t.getStatus() != TaskStatus.DONE || t.getActualDurationDays() == null) {
                continue;
            }
            int actual = t.getActualDurationDays();
            if (t.getEstimatedDurationDays() != null) {
                manualErrSum += Math.abs(t.getEstimatedDurationDays() - actual);
                manualN++;
            }
            if (t.getPredictedDurationDays() != null) {
                modelErrSum += Math.abs(t.getPredictedDurationDays() - actual);
                modelN++;
            }
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("manualMae", manualN == 0 ? null : round(manualErrSum / manualN));
        m.put("modelMae", modelN == 0 ? null : round(modelErrSum / modelN));
        m.put("manualSample", manualN);
        m.put("modelSample", modelN);
        return m;
    }

    private Map<String, Object> activeModel() {
        ModelMetrics active = metricsRepository.findFirstByActiveTrueOrderByTrainedAtDesc().orElse(null);
        Map<String, Object> m = new LinkedHashMap<>();
        if (active == null) {
            m.put("type", "FALLBACK_AVERAGE");
            m.put("trained", false);
            return m;
        }
        m.put("type", active.getModelType().name());
        m.put("trained", true);
        m.put("mae", active.getMae());
        m.put("rmse", active.getRmse());
        m.put("r2", active.getR2());
        m.put("sampleSize", active.getSampleSize());
        return m;
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
