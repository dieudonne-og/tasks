package rw.ac.uok.taskms.workload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rw.ac.uok.taskms.task.Task;
import rw.ac.uok.taskms.task.TaskRepository;
import rw.ac.uok.taskms.task.TaskService;
import rw.ac.uok.taskms.task.TaskStatus;
import rw.ac.uok.taskms.user.Role;
import rw.ac.uok.taskms.user.User;
import rw.ac.uok.taskms.user.UserRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Predicted-workload balancing (research objective 3). Sums each assignee's predicted
 * remaining effort across open tasks, flags anyone above the team mean by a configurable
 * factor, and suggests moving specific tasks to the least-loaded officer.
 */
@Service
public class WorkloadService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskService taskService;
    private final double overloadFactor;

    public WorkloadService(TaskRepository taskRepository, UserRepository userRepository,
                           TaskService taskService,
                           @Value("${taskms.workload.overload-factor}") double overloadFactor) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.taskService = taskService;
        this.overloadFactor = overloadFactor;
    }

    public record AssigneeLoad(Long userId, String name, String role, long openTasks,
                               double predictedLoadDays, boolean overloaded) {
    }

    public record Suggestion(Long taskId, String taskTitle, Long fromUserId, String fromUser,
                             Long toUserId, String toUser, double predictedDays, String reason) {
    }

    public List<AssigneeLoad> currentLoads() {
        List<User> staff = activeStaff();
        double[] loads = new double[staff.size()];
        long[] counts = new long[staff.size()];

        for (int i = 0; i < staff.size(); i++) {
            List<Task> open = openTasksOf(staff.get(i));
            counts[i] = open.size();
            loads[i] = open.stream()
                    .mapToDouble(t -> t.getPredictedDurationDays() != null ? t.getPredictedDurationDays() : 0)
                    .sum();
        }

        double mean = staff.isEmpty() ? 0 : java.util.Arrays.stream(loads).average().orElse(0);
        double threshold = mean * overloadFactor;

        List<AssigneeLoad> result = new ArrayList<>();
        for (int i = 0; i < staff.size(); i++) {
            User u = staff.get(i);
            result.add(new AssigneeLoad(u.getId(), u.getFullName(), u.getRole().name(),
                    counts[i], round(loads[i]), mean > 0 && loads[i] > threshold));
        }
        result.sort(Comparator.comparingDouble(AssigneeLoad::predictedLoadDays).reversed());
        return result;
    }

    /**
     * Suggests reassigning at-risk / not-yet-started tasks from overloaded staff to the
     * least-loaded eligible officer.
     */
    public List<Suggestion> suggestions() {
        List<User> staff = activeStaff();
        Map<Long, Double> loadByUser = new LinkedHashMap<>();
        Map<Long, User> userById = new LinkedHashMap<>();
        for (User u : staff) {
            userById.put(u.getId(), u);
            double load = openTasksOf(u).stream()
                    .mapToDouble(t -> t.getPredictedDurationDays() != null ? t.getPredictedDurationDays() : 0)
                    .sum();
            loadByUser.put(u.getId(), load);
        }

        double mean = loadByUser.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double threshold = mean * overloadFactor;
        if (mean <= 0) {
            return List.of();
        }

        List<Suggestion> suggestions = new ArrayList<>();
        for (User u : staff) {
            if (loadByUser.get(u.getId()) <= threshold) {
                continue;
            }
            List<Task> movable = openTasksOf(u).stream()
                    .filter(t -> t.getStatus() == TaskStatus.TODO || taskService.isAtRisk(t))
                    .sorted(Comparator.comparingDouble(
                            (Task t) -> t.getPredictedDurationDays() != null ? t.getPredictedDurationDays() : 0)
                            .reversed())
                    .toList();

            for (Task t : movable) {
                if (loadByUser.get(u.getId()) <= threshold) {
                    break;
                }
                Long targetId = leastLoaded(loadByUser, u.getId());
                if (targetId == null) {
                    break;
                }
                double days = t.getPredictedDurationDays() != null ? t.getPredictedDurationDays() : 0;
                suggestions.add(new Suggestion(t.getId(), t.getTitle(), u.getId(), u.getFullName(),
                        targetId, userById.get(targetId).getFullName(), round(days),
                        taskService.isAtRisk(t) ? "At risk of missing deadline" : "Rebalance overloaded assignee"));
                loadByUser.put(u.getId(), loadByUser.get(u.getId()) - days);
                loadByUser.put(targetId, loadByUser.get(targetId) + days);
            }
        }
        return suggestions;
    }

    private Long leastLoaded(Map<Long, Double> loadByUser, Long exclude) {
        return loadByUser.entrySet().stream()
                .filter(e -> !e.getKey().equals(exclude))
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private List<Task> openTasksOf(User user) {
        return taskRepository.findByAssignee(user).stream()
                .filter(t -> t.getStatus() == TaskStatus.TODO || t.getStatus() == TaskStatus.IN_PROGRESS)
                .toList();
    }

    private List<User> activeStaff() {
        return userRepository.findAll().stream()
                .filter(User::isActive)
                .filter(u -> u.getRole() == Role.HR_OFFICER || u.getRole() == Role.HR_MANAGER)
                .toList();
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
