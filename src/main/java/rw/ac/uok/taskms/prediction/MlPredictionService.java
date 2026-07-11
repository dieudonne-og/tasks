package rw.ac.uok.taskms.prediction;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import rw.ac.uok.taskms.task.Task;
import rw.ac.uok.taskms.task.TaskRepository;
import rw.ac.uok.taskms.task.TaskStatus;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.regression.DataFrameRegression;
import smile.regression.OLS;
import smile.regression.RandomForest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Trains and serves the supervised deadline-prediction models (research objective 3).
 * Compares OLS Linear Regression against a Random Forest on the department's own
 * completed-task history, keeps the model with the lower RMSE, and produces a 95%
 * confidence interval from the active model's residual spread. Falls back to a
 * category average until enough history exists.
 *
 * This is the primary {@link DurationPredictor} — task create/update calls it.
 */
@Service
@Primary
public class MlPredictionService implements DurationPredictor {

    private static final Logger log = LoggerFactory.getLogger(MlPredictionService.class);
    private static final String[] COLUMNS = {"y", "tt", "as", "cx"};
    private static final Formula FORMULA = Formula.lhs("y");

    private final TaskRepository taskRepository;
    private final ModelMetricsRepository metricsRepository;
    private final FallbackPredictor fallbackPredictor;
    private final int minTrainingSamples;
    private final int retrainAfterNewCompleted;

    private final AtomicInteger completedSinceTrain = new AtomicInteger(0);

    private volatile DataFrameRegression activeModel;
    private volatile ModelType activeModelType;
    private volatile double activeResidualRmse;
    private volatile int lastSampleSize;

    public MlPredictionService(TaskRepository taskRepository,
                               ModelMetricsRepository metricsRepository,
                               FallbackPredictor fallbackPredictor,
                               @Value("${taskms.prediction.min-training-samples}") int minTrainingSamples,
                               @Value("${taskms.prediction.retrain-after-new-completed}") int retrainAfterNewCompleted) {
        this.taskRepository = taskRepository;
        this.metricsRepository = metricsRepository;
        this.fallbackPredictor = fallbackPredictor;
        this.minTrainingSamples = minTrainingSamples;
        this.retrainAfterNewCompleted = retrainAfterNewCompleted;
    }

    @PostConstruct
    public void init() {
        try {
            train();
        } catch (Exception ex) {
            log.warn("Initial model training skipped: {}", ex.getMessage());
        }
    }

    @Override
    public PredictionResult predict(Task task) {
        DataFrameRegression model = activeModel;
        if (model == null) {
            return fallbackPredictor.predict(task);
        }
        double[] features = featureRow(task, 0.0);
        DataFrame single = DataFrame.of(new double[][]{features}, COLUMNS);
        double predicted = model.predict(single)[0];
        double margin = 1.96 * activeResidualRmse;
        return new PredictionResult(
                round(Math.max(0, predicted)),
                round(Math.max(0, predicted - margin)),
                round(Math.max(0, predicted + margin)),
                activeModelType);
    }

    /**
     * (Re)trains both models on completed tasks, records metrics, and activates the
     * better model. Returns the persisted metrics (both models), newest first.
     */
    public synchronized List<ModelMetrics> train() {
        List<Task> done = taskRepository.findByStatus(TaskStatus.DONE).stream()
                .filter(t -> t.getActualDurationDays() != null
                        && t.getTaskType() != null && t.getAssignee() != null)
                .toList();

        lastSampleSize = done.size();
        completedSinceTrain.set(0);

        if (done.size() < minTrainingSamples) {
            activeModel = null;
            activeModelType = null;
            log.info("Not enough completed tasks to train ({} < {}); using fallback predictor",
                    done.size(), minTrainingSamples);
            return List.of();
        }

        double[][] data = new double[done.size()][];
        for (int i = 0; i < done.size(); i++) {
            Task t = done.get(i);
            data[i] = featureRow(t, t.getActualDurationDays());
        }

        // Train/test split (80/20).
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < data.length; i++) idx.add(i);
        Collections.shuffle(idx, new java.util.Random(42));
        int testSize = Math.max(1, data.length / 5);
        int trainSize = data.length - testSize;

        double[][] train = new double[trainSize][];
        double[][] test = new double[testSize][];
        for (int i = 0; i < trainSize; i++) train[i] = data[idx.get(i)];
        for (int i = 0; i < testSize; i++) test[i] = data[idx.get(trainSize + i)];

        DataFrame trainFrame = DataFrame.of(train, COLUMNS);

        DataFrameRegression ols = OLS.fit(FORMULA, trainFrame);
        DataFrameRegression rf = RandomForest.fit(FORMULA, trainFrame);

        Eval olsEval = evaluate(ols, test);
        Eval rfEval = evaluate(rf, test);

        boolean rfWins = rfEval.rmse <= olsEval.rmse;

        // Retire any previously active model records before recording this run.
        List<ModelMetrics> previouslyActive = metricsRepository.findByActiveTrue();
        previouslyActive.forEach(m -> m.setActive(false));
        metricsRepository.saveAll(previouslyActive);

        metricsRepository.saveAll(List.of(
                new ModelMetrics(ModelType.LINEAR_REGRESSION, olsEval.mae, olsEval.rmse,
                        olsEval.r2, done.size(), !rfWins),
                new ModelMetrics(ModelType.RANDOM_FOREST, rfEval.mae, rfEval.rmse,
                        rfEval.r2, done.size(), rfWins)));

        if (rfWins) {
            activeModel = rf;
            activeModelType = ModelType.RANDOM_FOREST;
            activeResidualRmse = rfEval.rmse;
        } else {
            activeModel = ols;
            activeModelType = ModelType.LINEAR_REGRESSION;
            activeResidualRmse = olsEval.rmse;
        }

        log.info("Trained models on {} tasks. Active={} (LR rmse={}, RF rmse={})",
                done.size(), activeModelType, round(olsEval.rmse), round(rfEval.rmse));

        return metricsRepository.findAllByOrderByTrainedAtDesc();
    }

    /** Signals that a task was completed; triggers a retrain once enough have accrued. */
    public void onTaskCompleted() {
        if (completedSinceTrain.incrementAndGet() >= retrainAfterNewCompleted) {
            try {
                train();
            } catch (Exception ex) {
                log.warn("Auto-retrain failed: {}", ex.getMessage());
            }
        }
    }

    public boolean isModelActive() {
        return activeModel != null;
    }

    public ModelType getActiveModelType() {
        return activeModelType;
    }

    public double getActiveResidualRmse() {
        return activeResidualRmse;
    }

    public int getLastSampleSize() {
        return lastSampleSize;
    }

    private double[] featureRow(Task task, double target) {
        double tt = task.getTaskType() != null ? task.getTaskType().getId() : 0;
        double as = task.getAssignee() != null ? task.getAssignee().getId() : 0;
        double cx = task.getComplexity() != null ? task.getComplexity().getOrdinalValue() : 2;
        return new double[]{target, tt, as, cx};
    }

    private Eval evaluate(DataFrameRegression model, double[][] test) {
        DataFrame testFrame = DataFrame.of(test, COLUMNS);
        double[] predicted = model.predict(testFrame);
        double n = test.length;
        double sumAbs = 0, sumSq = 0, sumY = 0;
        for (int i = 0; i < test.length; i++) {
            double err = predicted[i] - test[i][0];
            sumAbs += Math.abs(err);
            sumSq += err * err;
            sumY += test[i][0];
        }
        double mae = sumAbs / n;
        double rmse = Math.sqrt(sumSq / n);
        double meanY = sumY / n;
        double ssTot = 0;
        for (double[] row : test) {
            ssTot += (row[0] - meanY) * (row[0] - meanY);
        }
        double r2 = ssTot == 0 ? 0 : 1 - (sumSq / ssTot);
        return new Eval(mae, rmse, r2);
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private record Eval(double mae, double rmse, double r2) {
    }
}
