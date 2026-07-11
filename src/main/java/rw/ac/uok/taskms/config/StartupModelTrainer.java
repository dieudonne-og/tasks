package rw.ac.uok.taskms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import rw.ac.uok.taskms.prediction.DeadlinePredictionService;
import rw.ac.uok.taskms.prediction.MlPredictionService;

/**
 * Ensures a model is trained on startup when the database already contains
 * history (e.g. the persistent MySQL profile, where {@link DataSeeder} skips
 * seeding). Runs after the seeder; if the model was already trained during
 * seeding this is a no-op.
 */
@Component
@Order(2)
public class StartupModelTrainer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupModelTrainer.class);

    private final DeadlinePredictionService predictionService;
    private final MlPredictionService ml;

    public StartupModelTrainer(DeadlinePredictionService predictionService, MlPredictionService ml) {
        this.predictionService = predictionService;
        this.ml = ml;
    }

    @Override
    public void run(String... args) {
        if (!ml.isTrained()) {
            log.info("Training prediction model from existing history on startup...");
            predictionService.retrain();
        }
    }
}
