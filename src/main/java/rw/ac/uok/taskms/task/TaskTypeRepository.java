package rw.ac.uok.taskms.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaskTypeRepository extends JpaRepository<TaskType, Long> {

    Optional<TaskType> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
