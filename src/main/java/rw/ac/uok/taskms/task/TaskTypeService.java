package rw.ac.uok.taskms.task;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.uok.taskms.common.ApiException;
import rw.ac.uok.taskms.task.dto.TaskTypeDto;

import java.util.List;

@Service
public class TaskTypeService {

    private final TaskTypeRepository taskTypeRepository;
    private final TaskRepository taskRepository;

    public TaskTypeService(TaskTypeRepository taskTypeRepository, TaskRepository taskRepository) {
        this.taskTypeRepository = taskTypeRepository;
        this.taskRepository = taskRepository;
    }

    public List<TaskType> findAll() {
        return taskTypeRepository.findAll();
    }

    @Transactional
    public TaskType create(TaskTypeDto dto) {
        if (taskTypeRepository.existsByNameIgnoreCase(dto.name().trim())) {
            throw ApiException.conflict("A task type named '" + dto.name() + "' already exists");
        }
        return taskTypeRepository.save(new TaskType(dto.name().trim(), dto.description()));
    }

    @Transactional
    public TaskType update(Long id, TaskTypeDto dto) {
        TaskType type = taskTypeRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Task type " + id + " not found"));
        if (dto.name() != null && !dto.name().isBlank()) {
            String newName = dto.name().trim();
            if (!newName.equalsIgnoreCase(type.getName())
                    && taskTypeRepository.existsByNameIgnoreCase(newName)) {
                throw ApiException.conflict("A task type named '" + newName + "' already exists");
            }
            type.setName(newName);
        }
        if (dto.description() != null) {
            type.setDescription(dto.description());
        }
        return taskTypeRepository.save(type);
    }

    @Transactional
    public void delete(Long id) {
        if (!taskRepository.findByTaskTypeId(id).isEmpty()) {
            throw ApiException.conflict("Cannot delete a task type that still has tasks");
        }
        taskTypeRepository.deleteById(id);
    }
}
