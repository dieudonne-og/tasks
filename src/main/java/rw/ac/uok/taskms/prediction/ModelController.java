package rw.ac.uok.taskms.prediction;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Exposes model metrics and manual retraining (Objectives 3 and 4). */
@RestController
@RequestMapping("/api/models")
public class ModelController {

    private final DeadlinePredictionService predictionService;
    private final MlPredictionService ml;
    private final ModelMetricsRepository metricsRepository;

    public ModelController(DeadlinePredictionService predictionService,
                           MlPredictionService ml,
                           ModelMetricsRepository metricsRepository) {
        this.predictionService = predictionService;
        this.ml = ml;
        this.metricsRepository = metricsRepository;
    }

    /** Latest metrics for both algorithms, plus which model is currently active. */
    @GetMapping("/metrics")
    @PreAuthorize("hasAnyRole('ADMIN','HR_MANAGER')")
    public Map<String, Object> metrics() {
        List<ModelMetrics> history = metricsRepository.findByOrderByTrainedAtDesc();
        return Map.of(
                "trained", ml.isTrained(),
                "activeModel", ml.activeModel().map(Enum::name).orElse("FALLBACK_AVERAGE"),
                "latest", history.stream().filter(ModelMetrics::isActive).findFirst().orElse(null),
                "history", history.stream().limit(10).toList());
    }

    @PostMapping("/retrain")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> retrain() {
        List<ModelMetrics> metrics = predictionService.retrain();
        return Map.of(
                "trained", ml.isTrained(),
                "activeModel", ml.activeModel().map(Enum::name).orElse("FALLBACK_AVERAGE"),
                "metrics", metrics);
    }
}
