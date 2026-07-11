package rw.ac.uok.taskms.workload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rw.ac.uok.taskms.task.Task;
import rw.ac.uok.taskms.task.TaskRepository;
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
 * Computes predicted workload per staff member and suggests redistribution
 * (Objective 3). Each officer's load is the sum of predicted durations of their
 * open tasks; anyone whose load exceeds the team mean by the configured factor
 * is flagged as overloaded, and their at-risk / not-yet-started tasks are
 * proposed for the least-loaded officer.
 */
@Service
public class WorkloadService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final double overloadFactor;

    public WorkloadService(TaskRepository taskRepository,
                           UserRepository userRepository,
                           @Value("${taskms.workload.overload-factor}") double overloadFactor) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.overloadFactor = overloadFactor;
    }

    public record WorkloadEntry(Long userId, String name, int openTasks,
                                double predictedLoadDays, boolean overloaded) {
    }

    public record Suggestion(Long taskId, String taskTitle,
                             Long fromUserId, String fromUser,
                             Long toUserId, String toUser,
                             double predictedDays, String reason) {
    }

    public record WorkloadReport(List<WorkloadEntry> workload, double teamMeanDays,
                                 List<Suggestion> suggestions) {
    }

    public WorkloadReport report() {
        List<User> officers = userRepository.findByActiveTrue().stream()
                .filter(u -> u.getRole() == Role.HR_OFFICER || u.getRole() == Role.HR_MANAGER)
                .toList();
        List<TaskStatus> open = List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS);

        Map<Long, Double> loadByUser = new LinkedHashMap<>();
        Map<Long, Integer> countByUser = new LinkedHashMap<>();
        Map<Long, User> usersById = new LinkedHashMap<>();
        for (User u : officers) {
            usersById.put(u.getId(), u);
            loadByUser.put(u.getId(), 0.0);
            countByUser.put(u.getId(), 0);
        }

        List<Task> openTasks = taskRepository.findByStatusIn(open);
        for (Task t : openTasks) {
            Long uid = t.getAssignee().getId();
            if (!loadByUser.containsKey(uid)) {
                continue; // assignee not an active officer/manager
            }
            double days = predictedDays(t);
            loadByUser.merge(uid, days, Double::sum);
            countByUser.merge(uid, 1, Integer::sum);
        }

        double teamMean = loadByUser.isEmpty() ? 0
                : loadByUser.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);

        List<WorkloadEntry> entries = new ArrayList<>();
        for (Long uid : loadByUser.keySet()) {
            double load = round(loadByUser.get(uid));
            boolean overloaded = teamMean > 0 && load > teamMean * overloadFactor;
            entries.add(new WorkloadEntry(uid, usersById.get(uid).getFullName(),
                    countByUser.get(uid), load, overloaded));
        }
        entries.sort(Comparator.comparingDouble(WorkloadEntry::predictedLoadDays).reversed());

        List<Suggestion> suggestions = buildSuggestions(openTasks, loadByUser, usersById, teamMean);
        return new WorkloadReport(entries, round(teamMean), suggestions);
    }

    private List<Suggestion> buildSuggestions(List<Task> openTasks,
                                              Map<Long, Double> loadByUser,
                                              Map<Long, User> usersById,
                                              double teamMean) {
        List<Suggestion> suggestions = new ArrayList<>();
        if (teamMean <= 0 || loadByUser.size() < 2) {
            return suggestions;
        }
        // Work on a mutable copy so successive suggestions account for prior moves.
        Map<Long, Double> load = new LinkedHashMap<>(loadByUser);

        List<Long> overloaded = load.keySet().stream()
                .filter(uid -> load.get(uid) > teamMean * overloadFactor)
                .sorted(Comparator.comparingDouble(load::get).reversed())
                .toList();

        for (Long fromId : overloaded) {
            // Candidate tasks: open, unstarted (TODO) tasks of the overloaded user,
            // heaviest first, so moving them relieves the most load.
            List<Task> movable = openTasks.stream()
                    .filter(t -> t.getAssignee().getId().equals(fromId))
                    .filter(t -> t.getStatus() == TaskStatus.TODO)
                    .sorted(Comparator.comparingDouble(this::predictedDays).reversed())
                    .toList();

            for (Task t : movable) {
                if (load.get(fromId) <= teamMean * overloadFactor) {
                    break; // relieved enough
                }
                // Least-loaded recipient other than the sender.
                Long toId = load.keySet().stream()
                        .filter(uid -> !uid.equals(fromId))
                        .min(Comparator.comparingDouble(load::get))
                        .orElse(null);
                if (toId == null) {
                    break;
                }
                double days = predictedDays(t);
                // Only suggest if it actually improves balance.
                if (load.get(toId) + days >= load.get(fromId)) {
                    continue;
                }
                suggestions.add(new Suggestion(
                        t.getId(), t.getTitle(),
                        fromId, usersById.get(fromId).getFullName(),
                        toId, usersById.get(toId).getFullName(),
                        round(days),
                        String.format("%s is overloaded (%.1f days); %s has the lightest load",
                                usersById.get(fromId).getFullName(), load.get(fromId),
                                usersById.get(toId).getFullName())));
                load.merge(fromId, -days, Double::sum);
                load.merge(toId, days, Double::sum);
            }
        }
        return suggestions;
    }

    private double predictedDays(Task t) {
        if (t.getPredictedDurationDays() != null) {
            return t.getPredictedDurationDays();
        }
        if (t.getEstimatedDurationDays() != null) {
            return t.getEstimatedDurationDays();
        }
        return 3.0; // conservative default
    }

    private static double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
