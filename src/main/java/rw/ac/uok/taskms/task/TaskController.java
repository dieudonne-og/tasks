package rw.ac.uok.taskms.task;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rw.ac.uok.taskms.common.ApiException;
import rw.ac.uok.taskms.security.CurrentUser;
import rw.ac.uok.taskms.task.dto.*;
import rw.ac.uok.taskms.user.Role;
import rw.ac.uok.taskms.user.User;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final CurrentUser currentUser;

    public TaskController(TaskService taskService, CurrentUser currentUser) {
        this.taskService = taskService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<TaskDto> board() {
        return taskService.boardFor(currentUser.require()).stream().map(TaskDto::from).toList();
    }

    @GetMapping("/{id}")
    public TaskDto get(@PathVariable Long id) {
        Task task = taskService.findById(id);
        requireAccess(task);
        return TaskDto.from(task);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskDto create(@Valid @RequestBody CreateTaskRequest request) {
        return TaskDto.from(taskService.create(request, currentUser.require()));
    }

    @PutMapping("/{id}")
    public TaskDto update(@PathVariable Long id, @Valid @RequestBody UpdateTaskRequest request) {
        requireAccess(taskService.findById(id));
        return TaskDto.from(taskService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public TaskDto changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        requireAccess(taskService.findById(id));
        return TaskDto.from(taskService.changeStatus(id, request.status()));
    }

    @PatchMapping("/{id}/complete")
    public TaskDto complete(@PathVariable Long id, @Valid @RequestBody CompleteTaskRequest request) {
        requireAccess(taskService.findById(id));
        return TaskDto.from(taskService.complete(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','HR_MANAGER')")
    public void delete(@PathVariable Long id) {
        taskService.delete(id);
    }

    /** HR officers may only touch tasks assigned to them; managers/admins any. */
    private void requireAccess(Task task) {
        User user = currentUser.require();
        if (user.getRole() == Role.HR_OFFICER
                && !task.getAssignee().getId().equals(user.getId())) {
            throw ApiException.forbidden("You can only access tasks assigned to you");
        }
    }
}
