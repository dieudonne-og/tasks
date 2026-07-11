package rw.ac.uok.taskms.prediction;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/models")
public class ModelController {

    private final MlPredictionService predictionService;
    private final ModelMetricsRepository metricsRepository;

    public ModelController(MlPredictionService predictionService,
                           ModelMetricsRepository metricsRepository) {
        this.predictionService = predictionService;
        this.metricsRepository = metricsRepository;
    }

    @GetMapping("/metrics")
    @PreAuthorize("hasAnyRole('ADMIN','HR_MANAGER')")
    public List<ModelMetrics> metrics() {
        return metricsRepository.findAllByOrderByTrainedAtDesc();
    }

    @PostMapping("/retrain")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ModelMetrics> retrain() {
        predictionService.train();
        return metricsRepository.findAllByOrderByTrainedAtDesc();
    }
}
