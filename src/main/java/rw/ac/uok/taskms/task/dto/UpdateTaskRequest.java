package rw.ac.uok.taskms.task.dto;

import jakarta.validation.constraints.Positive;
import rw.ac.uok.taskms.task.Complexity;

import java.time.LocalDate;

/** Partial update; only non-null fields are applied. */
public record UpdateTaskRequest(
        String title,
        String description,
        Long taskTypeId,
        Long assigneeId,
        Complexity complexity,
        @Positive Integer estimatedDurationDays,
        LocalDate startDate,
        LocalDate dueDate) {
}
