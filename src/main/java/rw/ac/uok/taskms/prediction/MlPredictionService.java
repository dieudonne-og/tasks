package rw.ac.uok.taskms.prediction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import weka.classifiers.Classifier;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Trains and serves the supervised duration-prediction models (Objective 3).
 *
 * <p>On each training run it builds a Weka dataset from completed tasks, sets
 * aside a test split, trains <b>linear regression</b> and <b>random forest</b>,
 * measures MAE / RMSE / R&sup2; for each (Objective 4), then refits the better
 * model (lower RMSE) on the full history for live predictions. Each prediction
 * carries a 95% confidence interval derived from the active model's RMSE
 * (section 2.1.7).
 *
 * <p>Holds trained state in memory behind a volatile reference; a training run
 * publishes a new immutable snapshot atomically.
 */
@Service
public class MlPredictionService {

    private static final Logger log = LoggerFactory.getLogger(MlPredictionService.class);
    private static final double TEST_FRACTION = 0.30;

    /** Immutable snapshot of the currently deployed model. */
    private record Deployed(Classifier classifier, ModelType type, Instances header, double rmse) {
    }

    private volatile Deployed deployed;

    public boolean isTrained() {
        return deployed != null;
    }

    public Optional<ModelType> activeModel() {
        Deployed d = deployed;
        return d == null ? Optional.empty() : Optional.of(d.type());
    }

    /**
     * Trains both algorithms and deploys the better one.
     *
     * @return metrics for each algorithm (the deployed one has {@code active = true});
     *         empty if the data was insufficient to train.
     */
    public List<ModelMetrics> trainAndDeploy(List<TrainingSample> samples,
                                             List<String> typeNames,
                                             List<String> assigneeKeys,
                                             double confidenceZ) {
        if (samples.size() < 4) {
            log.info("Not enough samples ({}) to train; keeping fallback predictor", samples.size());
            this.deployed = null;
            return List.of();
        }

        Attribute typeAttr = new Attribute("taskType", new ArrayList<>(dedupe(typeNames)));
        Attribute assigneeAttr = new Attribute("assignee", new ArrayList<>(dedupe(assigneeKeys)));
        Attribute complexityAttr = new Attribute("complexity");
        Attribute durationAttr = new Attribute("duration");

        ArrayList<Attribute> attributes = new ArrayList<>(List.of(
                typeAttr, assigneeAttr, complexityAttr, durationAttr));

        Instances full = new Instances("tasks", attributes, samples.size());
        full.setClassIndex(attributes.size() - 1);
        for (TrainingSample s : samples) {
            Instance inst = toInstance(s, typeAttr, assigneeAttr);
            if (inst != null) {
                full.add(inst);
            }
        }
        if (full.numInstances() < 4) {
            this.deployed = null;
            return List.of();
        }

        // Shuffle and split for evaluation.
        full.randomize(new java.util.Random(42));
        int testSize = Math.max(1, (int) Math.round(full.numInstances() * TEST_FRACTION));
        int trainSize = full.numInstances() - testSize;
        if (trainSize < 2) {
            trainSize = full.numInstances();
            testSize = 0;
        }
        Instances train = new Instances(full, 0, trainSize);
        Instances test = testSize > 0 ? new Instances(full, trainSize, testSize) : full;

        ModelMetrics linearMetrics = trainOne(new LinearRegression(), ModelType.LINEAR, train, test, full.numInstances());
        ModelMetrics forestMetrics = trainOne(newRandomForest(), ModelType.RANDOM_FOREST, train, test, full.numInstances());

        List<ModelMetrics> results = new ArrayList<>();
        if (linearMetrics != null) results.add(linearMetrics);
        if (forestMetrics != null) results.add(forestMetrics);
        if (results.isEmpty()) {
            this.deployed = null;
            return List.of();
        }

        // Select the model with the lower RMSE as the active one.
        ModelMetrics best = results.stream()
                .min((a, b) -> Double.compare(a.getRmse(), b.getRmse()))
                .orElseThrow();
        best.setActive(true);

        // Refit the chosen algorithm on the full history for production predictions.
        try {
            Classifier production = best.getModelType() == ModelType.LINEAR
                    ? new LinearRegression() : newRandomForest();
            production.buildClassifier(full);
            Instances header = new Instances(full, 0);
            this.deployed = new Deployed(production, best.getModelType(), header, best.getRmse());
            log.info("Deployed {} model (RMSE={} days) trained on {} completed tasks",
                    best.getModelType(), round(best.getRmse()), full.numInstances());
        } catch (Exception e) {
            log.error("Failed to refit production model", e);
            this.deployed = null;
        }
        return results;
    }

    /**
     * Predicts a task's duration using the deployed model.
     *
     * @return the prediction, or empty when no model is deployed or the
     *         type/assignee was never seen during training (caller falls back).
     */
    public Optional<PredictionResult> predict(String taskType, String assigneeKey, int complexity, double confidenceZ) {
        Deployed d = deployed;
        if (d == null) {
            return Optional.empty();
        }
        Attribute typeAttr = d.header().attribute("taskType");
        Attribute assigneeAttr = d.header().attribute("assignee");
        if (typeAttr.indexOfValue(taskType) < 0) {
            return Optional.empty();
        }
        double assigneeIndex = assigneeAttr.indexOfValue(assigneeKey);
        double[] values = new double[d.header().numAttributes()];
        values[0] = typeAttr.indexOfValue(taskType);
        // Unseen assignee -> use a missing value so the model relies on the other features.
        values[1] = assigneeIndex < 0 ? weka.core.Utils.missingValue() : assigneeIndex;
        values[2] = complexity;
        values[3] = weka.core.Utils.missingValue();

        Instance inst = new DenseInstance(1.0, values);
        inst.setDataset(d.header());
        try {
            double predicted = d.classifier().classifyInstance(inst);
            double margin = confidenceZ * d.rmse();
            return Optional.of(PredictionResult.of(predicted, margin, d.type().name()));
        } catch (Exception e) {
            log.warn("Prediction failed, will fall back: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private ModelMetrics trainOne(Classifier classifier, ModelType type,
                                  Instances train, Instances test, int sampleSize) {
        try {
            classifier.buildClassifier(train);
            double sumAbs = 0, sumSq = 0, sumActual = 0, sumActualSq = 0;
            int n = test.numInstances();
            double[] actuals = new double[n];
            double[] preds = new double[n];
            for (int i = 0; i < n; i++) {
                Instance inst = test.instance(i);
                double actual = inst.classValue();
                double pred = classifier.classifyInstance(inst);
                actuals[i] = actual;
                preds[i] = pred;
                double err = pred - actual;
                sumAbs += Math.abs(err);
                sumSq += err * err;
                sumActual += actual;
                sumActualSq += actual * actual;
            }
            double mae = n == 0 ? 0 : sumAbs / n;
            double rmse = n == 0 ? 0 : Math.sqrt(sumSq / n);
            double r2 = computeR2(actuals, preds, sumActual, n);
            return new ModelMetrics(type, round(mae), round(rmse), round(r2), sampleSize);
        } catch (Exception e) {
            log.error("Failed to train {} model", type, e);
            return null;
        }
    }

    private static double computeR2(double[] actuals, double[] preds, double sumActual, int n) {
        if (n == 0) {
            return 0;
        }
        double mean = sumActual / n;
        double ssTot = 0, ssRes = 0;
        for (int i = 0; i < n; i++) {
            ssTot += Math.pow(actuals[i] - mean, 2);
            ssRes += Math.pow(actuals[i] - preds[i], 2);
        }
        if (ssTot == 0) {
            return 0;
        }
        return 1.0 - (ssRes / ssTot);
    }

    private RandomForest newRandomForest() {
        RandomForest rf = new RandomForest();
        rf.setNumIterations(100); // number of trees
        rf.setSeed(42);
        return rf;
    }

    private Instance toInstance(TrainingSample s, Attribute typeAttr, Attribute assigneeAttr) {
        int typeIdx = typeAttr.indexOfValue(s.taskType());
        if (typeIdx < 0) {
            return null; // type not in domain; skip
        }
        int assigneeIdx = assigneeAttr.indexOfValue(s.assignee());
        double[] values = new double[4];
        values[0] = typeIdx;
        values[1] = assigneeIdx < 0 ? weka.core.Utils.missingValue() : assigneeIdx;
        values[2] = s.complexity();
        values[3] = s.durationDays();
        return new DenseInstance(1.0, values);
    }

    private static List<String> dedupe(List<String> values) {
        List<String> out = new ArrayList<>();
        for (String v : values) {
            if (v != null && !out.contains(v)) {
                out.add(v);
            }
        }
        if (out.isEmpty()) {
            out.add("__none__");
        }
        Collections.sort(out);
        return out;
    }

    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
