package rw.ac.uok.taskms.task;

/**
 * Complexity rating supplied by the user when logging a task. Used as an
 * ordinal predictor feature by the machine-learning model (Objective 3).
 */
public enum Complexity {
    LOW(1),
    MEDIUM(2),
    HIGH(3);

    private final int level;

    Complexity(int level) {
        this.level = level;
    }

    /** Numeric encoding fed to the prediction model. */
    public int level() {
        return level;
    }
}
