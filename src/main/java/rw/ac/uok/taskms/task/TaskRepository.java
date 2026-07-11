package rw.ac.uok.taskms.task;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.uok.taskms.user.User;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByStatus(TaskStatus status);

    List<Task> findByStatusIn(List<TaskStatus> statuses);

    List<Task> findByAssignee(User assignee);

    List<Task> findByAssigneeAndStatusIn(User assignee, List<TaskStatus> statuses);

    long countByStatus(TaskStatus status);

    List<Task> findByTaskTypeId(Long taskTypeId);
}
