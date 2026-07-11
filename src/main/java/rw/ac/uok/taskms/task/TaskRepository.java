package rw.ac.uok.taskms.task;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.uok.taskms.user.User;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByStatus(TaskStatus status);
    List<Task> findByAssignee(User assignee);
    List<Task> findByAssigneeAndStatusNot(User assignee, TaskStatus status);
    long countByStatus(TaskStatus status);
}
