package rw.ac.uok.taskms.task;

import jakarta.persistence.*;
import rw.ac.uok.taskms.user.User;

import java.time.Instant;
import java.time.LocalDate;

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

    @ManyToOne(optional = false)
    @JoinColumn(name = "task_type_id")
    private TaskType taskType;

    @ManyToOne(optional = false)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Complexity complexity = Complexity.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.TODO;

    private Double estimatedDurationDays;

    private Double predictedDurationDays;
    private Double predictedLowerDays;
    private Double predictedUpperDays;
    private String predictionModel;

    private Double actualDurationDays;

    private LocalDate startDate;
    private LocalDate dueDate;
    private LocalDate completedDate;

    @ManyToOne
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TaskType getTaskType() { return taskType; }
    public void setTaskType(TaskType taskType) { this.taskType = taskType; }
    public User getAssignee() { return assignee; }
    public void setAssignee(User assignee) { this.assignee = assignee; }
    public Complexity getComplexity() { return complexity; }
    public void setComplexity(Complexity complexity) { this.complexity = complexity; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public Double getEstimatedDurationDays() { return estimatedDurationDays; }
    public void setEstimatedDurationDays(Double v) { this.estimatedDurationDays = v; }
    public Double getPredictedDurationDays() { return predictedDurationDays; }
    public void setPredictedDurationDays(Double v) { this.predictedDurationDays = v; }
    public Double getPredictedLowerDays() { return predictedLowerDays; }
    public void setPredictedLowerDays(Double v) { this.predictedLowerDays = v; }
    public Double getPredictedUpperDays() { return predictedUpperDays; }
    public void setPredictedUpperDays(Double v) { this.predictedUpperDays = v; }
    public String getPredictionModel() { return predictionModel; }
    public void setPredictionModel(String v) { this.predictionModel = v; }
    public Double getActualDurationDays() { return actualDurationDays; }
    public void setActualDurationDays(Double v) { this.actualDurationDays = v; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDate getCompletedDate() { return completedDate; }
    public void setCompletedDate(LocalDate completedDate) { this.completedDate = completedDate; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
