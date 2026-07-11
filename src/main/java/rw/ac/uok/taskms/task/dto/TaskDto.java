package rw.ac.uok.taskms.task.dto;

import rw.ac.uok.taskms.task.Complexity;
import rw.ac.uok.taskms.task.Task;
import rw.ac.uok.taskms.task.TaskStatus;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Task representation returned to the client, including the computed delay
 * warning (Objective 3): a task is "at risk" when its predicted upper bound
 * exceeds the days remaining before the deadline.
 */
public record TaskDto(
        Long id,
        String title,
        String description,
        Long taskTypeId,
        String taskTypeName,
        Long assigneeId,
        String assigneeName,
        Complexity complexity,
        TaskStatus status,
        Integer estimatedDurationDays,
        Double predictedDurationDays,
        Double predictedLowerDays,
        Double predictedUpperDays,
        String predictionModel,
        Integer actualDurationDays,
        LocalDate startDate,
        LocalDate dueDate,
        LocalDate completedDate,
        Long daysUntilDue,
        boolean atRisk,
        String riskReason) {

    public static TaskDto from(Task t) {
        LocalDate today = LocalDate.now();
        Long daysUntilDue = t.getDueDate() == null ? null
                : ChronoUnit.DAYS.between(today, t.getDueDate());

        boolean atRisk = false;
        String riskReason = null;
        if (t.getStatus().isOpen() && t.getDueDate() != null && t.getPredictedDurationDays() != null) {
            LocalDate from = t.getStartDate() != null && t.getStartDate().isAfter(today)
                    ? t.getStartDate() : today;
            long daysAvailable = ChronoUnit.DAYS.between(from, t.getDueDate());
            double needed = t.getPredictedUpperDays() != null
                    ? t.getPredictedUpperDays() : t.getPredictedDurationDays();
            if (daysAvailable < 0) {
                atRisk = true;
                riskReason = "The deadline has already passed";
            } else if (needed > daysAvailable) {
                atRisk = true;
                riskReason = String.format(
                        "Predicted up to %.1f days but only %d day(s) remain before the deadline",
                        needed, daysAvailable);
            }
        }

        return new TaskDto(
                t.getId(), t.getTitle(), t.getDescription(),
                t.getTaskType().getId(), t.getTaskType().getName(),
                t.getAssignee().getId(), t.getAssignee().getFullName(),
                t.getComplexity(), t.getStatus(),
                t.getEstimatedDurationDays(),
                t.getPredictedDurationDays(), t.getPredictedLowerDays(), t.getPredictedUpperDays(),
                t.getPredictionModel(),
                t.getActualDurationDays(),
                t.getStartDate(), t.getDueDate(), t.getCompletedDate(),
                daysUntilDue, atRisk, riskReason);
    }
}
