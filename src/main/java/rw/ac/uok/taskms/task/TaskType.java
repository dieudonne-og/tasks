package rw.ac.uok.taskms.task;

import jakarta.persistence.*;

/**
 * A category of recurring departmental work (e.g. an individual recruitment
 * step, a leave application, a performance appraisal). Task type is one of the
 * predictor features for duration (section 1.6.1, Objective 3).
 */
@Entity
@Table(name = "task_types", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class TaskType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 500)
    private String description;

    public TaskType() {
    }

    public TaskType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
