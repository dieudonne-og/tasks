package rw.ac.uok.taskms.task;

public enum Complexity {
    LOW(1),
    MEDIUM(2),
    HIGH(3);

    private final int ordinalValue;

    Complexity(int ordinalValue) {
        this.ordinalValue = ordinalValue;
    }

    public int getOrdinalValue() {
        return ordinalValue;
    }
}
