package rw.ac.uok.taskms.dashboard;

import org.springframework.stereotype.Service;
import rw.ac.uok.taskms.prediction.ModelMetrics;
import rw.ac.uok.taskms.prediction.ModelMetricsRepository;
import rw.ac.uok.taskms.task.Task;
import rw.ac.uok.taskms.task.TaskRepository;
import rw.ac.uok.taskms.task.TaskStatus;
import rw.ac.uok.taskms.workload.WorkloadService;
import rw.ac.uok.taskms.workload.WorkloadService.AssigneeLoad;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Aggregated reporting metrics (research objective 4). */
@Service
public class DashboardService {

    private final TaskRepository taskRepository;
    private final ModelMetricsRepository metricsRepository;
    private final WorkloadService workloadService;

    public DashboardService(TaskRepository taskRepository, ModelMetricsRepository metricsRepository,
                            WorkloadService workloadService) {
        this.taskRepository = taskRepository;
        this.metricsRepository = metricsRepository;
        this.workloadService = workloadService;
    }

    public record AccuracyComparison(double manualEstimateMae, double modelPredictionMae, int sampleSize) {
    }

    public record DashboardResponse(
            long totalTasks,
            Map<String, Long> countByStatus,
            Map<String, Long> countByType,
            double onTimeCompletionRate,
            AccuracyComparison accuracy,
            List<AssigneeLoad> workload,
            ModelMetrics activeModel,
            List<ModelMetrics> latestMetrics) {
    }

    public DashboardResponse build() {
        List<Task> all = taskRepository.findAll();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (TaskStatus s : TaskStatus.values()) {
            byStatus.put(s.name(), all.stream().filter(t -> t.getStatus() == s).count());
        }

        Map<String, Long> byType = new LinkedHashMap<>();
        for (Task t : all) {
            String type = t.getTaskType() != null ? t.getTaskType().getName() : "Unknown";
            byType.merge(type, 1L, Long::sum);
        }

        List<Task> completed = all.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE).toList();

        long withDeadline = completed.stream()
                .filter(t -> t.getDueDate() != null && t.getCompletedDate() != null).count();
        long onTime = completed.stream()
                .filter(t -> t.getDueDate() != null && t.getCompletedDate() != null
                        && !t.getCompletedDate().isAfter(t.getDueDate())).count();
        double onTimeRate = withDeadline == 0 ? 0 : round((double) onTime / withDeadline);

        AccuracyComparison accuracy = accuracy(completed);

        ModelMetrics active = metricsRepository.findByActiveTrue().stream().findFirst().orElse(null);

        return new DashboardResponse(
                all.size(), byStatus, byType, onTimeRate, accuracy,
                workloadService.currentLoads(), active,
                metricsRepository.findAllByOrderByTrainedAtDesc().stream().limit(4).toList());
    }

    private AccuracyComparison accuracy(List<Task> completed) {
        double estSum = 0, predSum = 0;
        int estN = 0, predN = 0;
        for (Task t : completed) {
            if (t.getActualDurationDays() == null) continue;
            if (t.getEstimatedDurationDays() != null) {
                estSum += Math.abs(t.getEstimatedDurationDays() - t.getActualDurationDays());
                estN++;
            }
            if (t.getPredictedDurationDays() != null) {
                predSum += Math.abs(t.getPredictedDurationDays() - t.getActualDurationDays());
                predN++;
            }
        }
        return new AccuracyComparison(
                estN == 0 ? 0 : round(estSum / estN),
                predN == 0 ? 0 : round(predSum / predN),
                Math.max(estN, predN));
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
