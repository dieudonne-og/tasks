package rw.ac.uok.taskms.task;

/** Columns of the shared task board. */
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE,
    CANCELLED;

    public boolean isOpen() {
        return this == TODO || this == IN_PROGRESS;
    }
}
