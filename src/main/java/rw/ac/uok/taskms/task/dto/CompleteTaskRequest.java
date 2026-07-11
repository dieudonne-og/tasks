package rw.ac.uok.taskms.task.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/** Records the actual effort spent when a task is marked complete (Objective 2). */
public record CompleteTaskRequest(
        @NotNull @Positive Integer actualDurationDays,
        LocalDate completedDate) {
}
