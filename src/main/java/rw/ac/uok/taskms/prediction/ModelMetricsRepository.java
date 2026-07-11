package rw.ac.uok.taskms.prediction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ModelMetricsRepository extends JpaRepository<ModelMetrics, Long> {

    Optional<ModelMetrics> findFirstByActiveTrueOrderByTrainedAtDesc();

    List<ModelMetrics> findByOrderByTrainedAtDesc();

    List<ModelMetrics> findByActiveTrue();
}
