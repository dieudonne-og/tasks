package rw.ac.uok.taskms.task;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.uok.taskms.common.BadRequestException;
import rw.ac.uok.taskms.common.NotFoundException;
import rw.ac.uok.taskms.notification.NotificationService;
import rw.ac.uok.taskms.notification.NotificationType;
import rw.ac.uok.taskms.prediction.MlPredictionService;
import rw.ac.uok.taskms.prediction.PredictionResult;
import rw.ac.uok.taskms.task.dto.TaskDtos.CompleteRequest;
import rw.ac.uok.taskms.task.dto.TaskDtos.CreateTaskRequest;
import rw.ac.uok.taskms.task.dto.TaskDtos.UpdateTaskRequest;
import rw.ac.uok.taskms.user.User;
import rw.ac.uok.taskms.user.UserRepository;

import java.time.LocalDate;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskTypeRepository taskTypeRepository;
    private final UserRepository userRepository;
    private final MlPredictionService predictionService;
    private final NotificationService notificationService;

    public TaskService(TaskRepository taskRepository, TaskTypeRepository taskTypeRepository,
                       UserRepository userRepository, MlPredictionService predictionService,
                       NotificationService notificationService) {
        this.taskRepository = taskRepository;
        this.taskTypeRepository = taskTypeRepository;
        this.userRepository = userRepository;
        this.predictionService = predictionService;
        this.notificationService = notificationService;
    }

    public List<Task> findAll() {
        return taskRepository.findAll();
    }

    public List<Task> findByAssignee(User assignee) {
        return taskRepository.findByAssignee(assignee);
    }

    public Task findById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task not found: " + id));
    }

    @Transactional
    public Task create(CreateTaskRequest req, User creator) {
        Task task = new Task();
        applyFields(task, req.title(), req.description(), req.taskTypeId(), req.assigneeId(),
                req.complexity(), req.estimatedDurationDays(), req.startDate(), req.dueDate());
        task.setCreatedBy(creator);
        applyPrediction(task);
        Task saved = taskRepository.save(task);
        notifyPrediction(saved);
        return saved;
    }

    @Transactional
    public Task update(Long id, UpdateTaskRequest req) {
        Task task = findById(id);
        applyFields(task, req.title(), req.description(), req.taskTypeId(), req.assigneeId(),
                req.complexity(), req.estimatedDurationDays(), req.startDate(), req.dueDate());
        applyPrediction(task);
        Task saved = taskRepository.save(task);
        notifyPrediction(saved);
        return saved;
    }

    /**
     * Notifies the assignee of the model's duration prediction, and separately warns them
     * when the task is at risk of missing its deadline (deduplicated while unread).
     */
    private void notifyPrediction(Task task) {
        if (task.getAssignee() == null || task.getPredictedDurationDays() == null) {
            return;
        }
        String model = task.getPredictionModel() != null ? task.getPredictionModel() : "model";
        notificationService.notify(task.getAssignee(), NotificationType.PREDICTION,
                "AI predicts \"" + task.getTitle() + "\" will take ~" + task.getPredictedDurationDays()
                        + " days (95% CI " + task.getPredictedLowerDays() + "–"
                        + task.getPredictedUpperDays() + "d, " + model + ").", task.getId());
        if (isAtRisk(task)) {
            notificationService.notifyOnce(task.getAssignee(), NotificationType.AT_RISK,
                    "\"" + task.getTitle() + "\" is at risk of missing its deadline ("
                            + task.getDueDate() + "): predicted effort exceeds the working days left.",
                    task.getId());
        }
    }

    @Transactional
    public Task changeStatus(Long id, TaskStatus status) {
        Task task = findById(id);
        if (status == TaskStatus.DONE && task.getActualDurationDays() == null) {
            throw new BadRequestException("Record actual effort via /complete before marking DONE");
        }
        task.setStatus(status);
        if (status == TaskStatus.IN_PROGRESS && task.getStartDate() == null) {
            task.setStartDate(LocalDate.now());
        }
        return taskRepository.save(task);
    }

    @Transactional
    public Task complete(Long id, CompleteRequest req) {
        Task task = findById(id);
        task.setActualDurationDays(req.actualDurationDays());
        task.setCompletedDate(req.completedDate() != null ? req.completedDate() : LocalDate.now());
        task.setStatus(TaskStatus.DONE);
        Task saved = taskRepository.save(task);
        predictionService.onTaskCompleted();
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new NotFoundException("Task not found: " + id);
        }
        taskRepository.deleteById(id);
    }

    /** Recomputes and stores the model prediction + confidence interval on the task. */
    public void applyPrediction(Task task) {
        PredictionResult result = predictionService.predict(task);
        task.setPredictedDurationDays(result.predictedDays());
        task.setPredictedLowerDays(result.lowerDays());
        task.setPredictedUpperDays(result.upperDays());
        task.setPredictionModel(result.model().name());
    }

    /**
     * A task is at risk when its predicted worst-case duration exceeds the working
     * days remaining before its deadline (research objective 3 — delay warnings).
     */
    public boolean isAtRisk(Task task) {
        if (task.getDueDate() == null || task.getStatus() == TaskStatus.DONE
                || task.getStatus() == TaskStatus.CANCELLED) {
            return false;
        }
        Double worstCase = task.getPredictedUpperDays() != null
                ? task.getPredictedUpperDays() : task.getPredictedDurationDays();
        if (worstCase == null) {
            return false;
        }
        LocalDate start = task.getStartDate() != null && !task.getStartDate().isBefore(LocalDate.now())
                ? task.getStartDate() : LocalDate.now();
        long remaining = WorkingDays.between(start, task.getDueDate());
        return worstCase > remaining;
    }

    private void applyFields(Task task, String title, String description, Long taskTypeId,
                             Long assigneeId, Complexity complexity, Double estimated,
                             LocalDate startDate, LocalDate dueDate) {
        TaskType type = taskTypeRepository.findById(taskTypeId)
                .orElseThrow(() -> new NotFoundException("Task type not found: " + taskTypeId));
        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new NotFoundException("Assignee not found: " + assigneeId));
        task.setTitle(title);
        task.setDescription(description);
        task.setTaskType(type);
        task.setAssignee(assignee);
        task.setComplexity(complexity);
        task.setEstimatedDurationDays(estimated);
        task.setStartDate(startDate);
        task.setDueDate(dueDate);
    }
}
