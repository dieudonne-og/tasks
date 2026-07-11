package rw.ac.uok.taskms.prediction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModelMetricsRepository extends JpaRepository<ModelMetrics, Long> {
    List<ModelMetrics> findAllByOrderByTrainedAtDesc();
    List<ModelMetrics> findByActiveTrue();
}
