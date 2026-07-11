package rw.ac.uok.taskms.task;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.uok.taskms.common.ApiException;
import rw.ac.uok.taskms.prediction.DeadlinePredictionService;
import rw.ac.uok.taskms.task.dto.CompleteTaskRequest;
import rw.ac.uok.taskms.task.dto.CreateTaskRequest;
import rw.ac.uok.taskms.task.dto.UpdateTaskRequest;
import rw.ac.uok.taskms.user.Role;
import rw.ac.uok.taskms.user.User;
import rw.ac.uok.taskms.user.UserRepository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskTypeRepository taskTypeRepository;
    private final UserRepository userRepository;
    private final DeadlinePredictionService predictionService;

    public TaskService(TaskRepository taskRepository,
                       TaskTypeRepository taskTypeRepository,
                       UserRepository userRepository,
                       DeadlinePredictionService predictionService) {
        this.taskRepository = taskRepository;
        this.taskTypeRepository = taskTypeRepository;
        this.userRepository = userRepository;
        this.predictionService = predictionService;
    }

    /**
     * Board view. HR officers see the tasks assigned to them; managers and
     * admins see the whole board.
     */
    public List<Task> boardFor(User viewer) {
        List<Task> tasks = (viewer.getRole() == Role.HR_OFFICER)
                ? taskRepository.findByAssignee(viewer)
                : taskRepository.findAll();
        return tasks.stream()
                .sorted(Comparator.comparing(Task::getDueDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public Task findById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Task " + id + " not found"));
    }

    @Transactional
    public Task create(CreateTaskRequest request, User creator) {
        TaskType type = requireType(request.taskTypeId());
        User assignee = requireUser(request.assigneeId());

        Task task = new Task();
        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setTaskType(type);
        task.setAssignee(assignee);
        task.setComplexity(request.complexity());
        task.setEstimatedDurationDays(request.estimatedDurationDays());
        task.setStartDate(request.startDate());
        task.setDueDate(request.dueDate());
        task.setStatus(TaskStatus.TODO);
        task.setCreatedBy(creator);

        // Objective 3: predict duration with a confidence interval on creation.
        predictionService.applyPrediction(task);
        return taskRepository.save(task);
    }

    @Transactional
    public Task update(Long id, UpdateTaskRequest request) {
        Task task = findById(id);
        boolean predictorInputsChanged = false;

        if (request.title() != null && !request.title().isBlank()) {
            task.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }
        if (request.taskTypeId() != null) {
            task.setTaskType(requireType(request.taskTypeId()));
            predictorInputsChanged = true;
        }
        if (request.assigneeId() != null) {
            task.setAssignee(requireUser(request.assigneeId()));
            predictorInputsChanged = true;
        }
        if (request.complexity() != null) {
            task.setComplexity(request.complexity());
            predictorInputsChanged = true;
        }
        if (request.estimatedDurationDays() != null) {
            task.setEstimatedDurationDays(request.estimatedDurationDays());
        }
        if (request.startDate() != null) {
            task.setStartDate(request.startDate());
        }
        if (request.dueDate() != null) {
            task.setDueDate(request.dueDate());
        }

        // Re-predict while the task is still open and its predictor inputs changed.
        if (predictorInputsChanged && task.getStatus().isOpen()) {
            predictionService.applyPrediction(task);
        }
        return taskRepository.save(task);
    }

    @Transactional
    public Task changeStatus(Long id, TaskStatus status) {
        Task task = findById(id);
        task.setStatus(status);
        if (status == TaskStatus.IN_PROGRESS && task.getStartDate() == null) {
            task.setStartDate(LocalDate.now());
        }
        return taskRepository.save(task);
    }

    /**
     * Marks a task complete and records the actual effort (Objective 2). This
     * new labelled example feeds the model, so a retrain may be triggered.
     */
    @Transactional
    public Task complete(Long id, CompleteTaskRequest request) {
        Task task = findById(id);
        task.setActualDurationDays(request.actualDurationDays());
        task.setStatus(TaskStatus.DONE);
        task.setCompletedDate(request.completedDate() != null ? request.completedDate() : LocalDate.now());
        Task saved = taskRepository.save(task);
        predictionService.onTaskCompleted();
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Task task = findById(id);
        taskRepository.delete(task);
    }

    private TaskType requireType(Long id) {
        return taskTypeRepository.findById(id)
                .orElseThrow(() -> ApiException.badRequest("Task type " + id + " not found"));
    }

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ApiException.badRequest("User " + id + " not found"));
    }
}
