package rw.ac.uok.taskms.prediction;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "model_metrics")
public class ModelMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModelType modelType;

    private double mae;
    private double rmse;
    private double r2;
    private int sampleSize;

    @Column(nullable = false)
    private Instant trainedAt = Instant.now();

    private boolean active;

    public ModelMetrics() {
    }

    public ModelMetrics(ModelType modelType, double mae, double rmse, double r2, int sampleSize, boolean active) {
        this.modelType = modelType;
        this.mae = mae;
        this.rmse = rmse;
        this.r2 = r2;
        this.sampleSize = sampleSize;
        this.active = active;
        this.trainedAt = Instant.now();
    }

    public Long getId() { return id; }
    public ModelType getModelType() { return modelType; }
    public double getMae() { return mae; }
    public double getRmse() { return rmse; }
    public double getR2() { return r2; }
    public int getSampleSize() { return sampleSize; }
    public Instant getTrainedAt() { return trainedAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
