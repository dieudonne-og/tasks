package rw.ac.uok.taskms.prediction;

import rw.ac.uok.taskms.task.Task;

public interface DurationPredictor {
    PredictionResult predict(Task task);
}
