package rw.ac.uok.taskms.task.dto;

import jakarta.validation.constraints.NotNull;
import rw.ac.uok.taskms.task.TaskStatus;

public record StatusRequest(@NotNull TaskStatus status) {
}
