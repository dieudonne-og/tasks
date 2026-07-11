package rw.ac.uok.taskms.task;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import rw.ac.uok.taskms.common.NotFoundException;
import rw.ac.uok.taskms.prediction.MlPredictionService;
import rw.ac.uok.taskms.prediction.PredictionResult;
import rw.ac.uok.taskms.task.dto.TaskDtos.*;
import rw.ac.uok.taskms.user.Role;
import rw.ac.uok.taskms.user.User;
import rw.ac.uok.taskms.user.UserRepository;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final UserRepository userRepository;
    private final MlPredictionService predictionService;

    public TaskController(TaskService taskService, UserRepository userRepository,
                          MlPredictionService predictionService) {
        this.taskService = taskService;
        this.userRepository = userRepository;
        this.predictionService = predictionService;
    }

    @GetMapping
    public List<TaskResponse> list(Authentication auth) {
        User current = currentUser(auth);
        List<Task> tasks = current.getRole() == Role.HR_OFFICER
                ? taskService.findByAssignee(current)
                : taskService.findAll();
        return tasks.stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable Long id, Authentication auth) {
        Task task = taskService.findById(id);
        enforceAccess(task, auth);
        return toResponse(task);
    }

    @PostMapping
    public TaskResponse create(@Valid @RequestBody CreateTaskRequest req, Authentication auth) {
        User current = currentUser(auth);
        if (current.getRole() == Role.HR_OFFICER && !current.getId().equals(req.assigneeId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Officers may only create tasks assigned to themselves");
        }
        return toResponse(taskService.create(req, current));
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTaskRequest req,
                               Authentication auth) {
        enforceAccess(taskService.findById(id), auth);
        return toResponse(taskService.update(id, req));
    }

    @PatchMapping("/{id}/status")
    public TaskResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req,
                                     Authentication auth) {
        enforceAccess(taskService.findById(id), auth);
        return toResponse(taskService.changeStatus(id, req.status()));
    }

    @PatchMapping("/{id}/complete")
    public TaskResponse complete(@PathVariable Long id, @Valid @RequestBody CompleteRequest req,
                                 Authentication auth) {
        enforceAccess(taskService.findById(id), auth);
        return toResponse(taskService.complete(id, req));
    }

    @GetMapping("/{id}/prediction")
    public PredictionResult prediction(@PathVariable Long id, Authentication auth) {
        Task task = taskService.findById(id);
        enforceAccess(task, auth);
        return predictionService.predict(task);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        enforceAccess(taskService.findById(id), auth);
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private TaskResponse toResponse(Task task) {
        return TaskResponse.from(task, taskService.isAtRisk(task));
    }

    private User currentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new NotFoundException("User not found: " + auth.getName()));
    }

    private void enforceAccess(Task task, Authentication auth) {
        User current = currentUser(auth);
        if (current.getRole() == Role.HR_OFFICER
                && (task.getAssignee() == null || !current.getId().equals(task.getAssignee().getId()))) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Officers may only access their own tasks");
        }
    }
}
