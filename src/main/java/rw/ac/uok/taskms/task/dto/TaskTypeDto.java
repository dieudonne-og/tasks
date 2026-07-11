package rw.ac.uok.taskms.task.dto;

import jakarta.validation.constraints.NotBlank;
import rw.ac.uok.taskms.task.TaskType;

public record TaskTypeDto(Long id, @NotBlank String name, String description) {

    public static TaskTypeDto from(TaskType type) {
        return new TaskTypeDto(type.getId(), type.getName(), type.getDescription());
    }
}
