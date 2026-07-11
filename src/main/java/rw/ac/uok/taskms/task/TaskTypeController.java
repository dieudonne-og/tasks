package rw.ac.uok.taskms.task;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rw.ac.uok.taskms.common.BadRequestException;
import rw.ac.uok.taskms.common.NotFoundException;

import java.util.List;

@RestController
@RequestMapping("/api/task-types")
public class TaskTypeController {

    private final TaskTypeRepository repository;

    public TaskTypeController(TaskTypeRepository repository) {
        this.repository = repository;
    }

    public record TaskTypeRequest(@NotBlank String name, String description) {}

    @GetMapping
    public List<TaskType> list() {
        return repository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public TaskType create(@Valid @RequestBody TaskTypeRequest req) {
        if (repository.existsByName(req.name())) {
            throw new BadRequestException("Task type already exists: " + req.name());
        }
        return repository.save(new TaskType(req.name(), req.description()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public TaskType update(@PathVariable Long id, @Valid @RequestBody TaskTypeRequest req) {
        TaskType type = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task type not found: " + id));
        type.setName(req.name());
        type.setDescription(req.description());
        return repository.save(type);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Task type not found: " + id);
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
