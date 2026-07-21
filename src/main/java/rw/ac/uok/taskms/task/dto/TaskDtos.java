package rw.ac.uok.taskms.task.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import rw.ac.uok.taskms.task.Complexity;
import rw.ac.uok.taskms.task.Task;
import rw.ac.uok.taskms.task.TaskStatus;

import java.time.LocalDate;

public class TaskDtos {

    public record CreateTaskRequest(
            @NotBlank String title,
            String description,
            @NotNull Long taskTypeId,
            @NotNull Long assigneeId,
            @NotNull Complexity complexity,
            Double estimatedDurationDays,
            LocalDate startDate,
            @NotNull @FutureOrPresent(message = "must be today or a future date") LocalDate dueDate) {
    }

    public record UpdateTaskRequest(
            @NotBlank String title,
            String description,
            @NotNull Long taskTypeId,
            @NotNull Long assigneeId,
            @NotNull Complexity complexity,
            Double estimatedDurationDays,
            LocalDate startDate,
            @NotNull @FutureOrPresent(message = "must be today or a future date") LocalDate dueDate) {
    }

    public record StatusRequest(@NotNull TaskStatus status) {
    }

    public record CompleteRequest(
            @NotNull @Positive Double actualDurationDays,
            LocalDate completedDate) {
    }

    public record TaskResponse(
            Long id,
            String title,
            String description,
            Long taskTypeId,
            String taskTypeName,
            Long assigneeId,
            String assigneeName,
            Complexity complexity,
            TaskStatus status,
            Double estimatedDurationDays,
            Double predictedDurationDays,
            Double predictedLowerDays,
            Double predictedUpperDays,
            String predictionModel,
            Double actualDurationDays,
            LocalDate startDate,
            LocalDate dueDate,
            LocalDate completedDate,
            boolean atRisk) {

        public static TaskResponse from(Task t, boolean atRisk) {
            return new TaskResponse(
                    t.getId(), t.getTitle(), t.getDescription(),
                    t.getTaskType() != null ? t.getTaskType().getId() : null,
                    t.getTaskType() != null ? t.getTaskType().getName() : null,
                    t.getAssignee() != null ? t.getAssignee().getId() : null,
                    t.getAssignee() != null ? t.getAssignee().getFullName() : null,
                    t.getComplexity(), t.getStatus(),
                    t.getEstimatedDurationDays(),
                    t.getPredictedDurationDays(), t.getPredictedLowerDays(),
                    t.getPredictedUpperDays(), t.getPredictionModel(),
                    t.getActualDurationDays(),
                    t.getStartDate(), t.getDueDate(), t.getCompletedDate(),
                    atRisk);
        }
    }
}
