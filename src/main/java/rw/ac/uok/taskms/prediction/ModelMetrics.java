package rw.ac.uok.taskms.prediction;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Accuracy of a trained model on the held-out test split, persisted after each
 * training run so the dashboard can compare linear regression against random
 * forest (Objective 4). {@code active = true} marks the model currently used
 * for live predictions.
 */
@Entity
@Table(name = "model_metrics")
public class ModelMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModelType modelType;

    /** Mean Absolute Error (days). */
    private double mae;

    /** Root Mean Square Error (days) - also used as the interval half-width basis. */
    private double rmse;

    /** Coefficient of determination. */
    private double r2;

    /** Number of completed tasks used for training + testing. */
    private int sampleSize;

    private boolean active;

    @Column(nullable = false)
    private Instant trainedAt = Instant.now();

    public ModelMetrics() {
    }

    public ModelMetrics(ModelType modelType, double mae, double rmse, double r2, int sampleSize) {
        this.modelType = modelType;
        this.mae = mae;
        this.rmse = rmse;
        this.r2 = r2;
        this.sampleSize = sampleSize;
    }

    public Long getId() {
        return id;
    }

    public ModelType getModelType() {
        return modelType;
    }

    public double getMae() {
        return mae;
    }

    public double getRmse() {
        return rmse;
    }

    public double getR2() {
        return r2;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getTrainedAt() {
        return trainedAt;
    }
}
