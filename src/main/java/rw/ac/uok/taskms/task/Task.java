package rw.ac.uok.taskms.task;

import jakarta.persistence.*;
import rw.ac.uok.taskms.user.User;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A single unit of work on the shared board. Captures the user's manual
 * estimate, the model's prediction (with confidence interval), and the actual
 * effort recorded on completion, so the department can learn estimate-vs-actual
 * gaps (Objectives 2 and 3).
 */
@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "task_type_id", nullable = false)
    private TaskType taskType;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "assignee_id", nullable = false)
    private User assignee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Complexity complexity = Complexity.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TaskStatus status = TaskStatus.TODO;

    /** The user's own manual estimate, in working days (optional). */
    private Integer estimatedDurationDays;

    /** Model (or fallback) prediction of duration in days. */
    private Double predictedDurationDays;
    private Double predictedLowerDays;
    private Double predictedUpperDays;

    /** Which predictor produced the current prediction (LINEAR / RANDOM_FOREST / FALLBACK_AVERAGE). */
    @Column(length = 20)
    private String predictionModel;

    /** Actual working days the task took, recorded on completion. */
    private Integer actualDurationDays;

    private LocalDate startDate;
    private LocalDate dueDate;
    private LocalDate completedDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // --- getters / setters ---

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    public Complexity getComplexity() {
        return complexity;
    }

    public void setComplexity(Complexity complexity) {
        this.complexity = complexity;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Integer getEstimatedDurationDays() {
        return estimatedDurationDays;
    }

    public void setEstimatedDurationDays(Integer estimatedDurationDays) {
        this.estimatedDurationDays = estimatedDurationDays;
    }

    public Double getPredictedDurationDays() {
        return predictedDurationDays;
    }

    public void setPredictedDurationDays(Double predictedDurationDays) {
        this.predictedDurationDays = predictedDurationDays;
    }

    public Double getPredictedLowerDays() {
        return predictedLowerDays;
    }

    public void setPredictedLowerDays(Double predictedLowerDays) {
        this.predictedLowerDays = predictedLowerDays;
    }

    public Double getPredictedUpperDays() {
        return predictedUpperDays;
    }

    public void setPredictedUpperDays(Double predictedUpperDays) {
        this.predictedUpperDays = predictedUpperDays;
    }

    public String getPredictionModel() {
        return predictionModel;
    }

    public void setPredictionModel(String predictionModel) {
        this.predictionModel = predictionModel;
    }

    public Integer getActualDurationDays() {
        return actualDurationDays;
    }

    public void setActualDurationDays(Integer actualDurationDays) {
        this.actualDurationDays = actualDurationDays;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(LocalDate completedDate) {
        this.completedDate = completedDate;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
