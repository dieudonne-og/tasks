package rw.ac.uok.taskms.prediction;

/** The predictors compared in this study (section 1.6.1, Objective 3). */
public enum ModelType {
    LINEAR,          // Linear regression - interpretable baseline (section 2.1.5)
    RANDOM_FOREST,   // Random forest - captures non-linear interactions (section 2.1.6)
    FALLBACK_AVERAGE // Category-level average used until enough history exists (Limitation 1.7.1)
}
