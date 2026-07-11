package rw.ac.uok.taskms.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import rw.ac.uok.taskms.task.Complexity;

import java.time.LocalDate;

public record CreateTaskRequest(
        @NotBlank String title,
        String description,
        @NotNull Long taskTypeId,
        @NotNull Long assigneeId,
        @NotNull Complexity complexity,
        @Positive Integer estimatedDurationDays,
        LocalDate startDate,
        @NotNull LocalDate dueDate) {
}
