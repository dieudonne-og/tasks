package rw.ac.uok.taskms.task;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rw.ac.uok.taskms.task.dto.TaskTypeDto;

import java.util.List;

@RestController
@RequestMapping("/api/task-types")
public class TaskTypeController {

    private final TaskTypeService taskTypeService;

    public TaskTypeController(TaskTypeService taskTypeService) {
        this.taskTypeService = taskTypeService;
    }

    /** All authenticated users need the list to create/filter tasks. */
    @GetMapping
    public List<TaskTypeDto> list() {
        return taskTypeService.findAll().stream().map(TaskTypeDto::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public TaskTypeDto create(@Valid @RequestBody TaskTypeDto dto) {
        return TaskTypeDto.from(taskTypeService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public TaskTypeDto update(@PathVariable Long id, @RequestBody TaskTypeDto dto) {
        return TaskTypeDto.from(taskTypeService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        taskTypeService.delete(id);
    }
}
