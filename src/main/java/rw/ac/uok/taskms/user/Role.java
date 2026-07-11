package rw.ac.uok.taskms.user;

/**
 * The three actors described in the conceptual framework (section 2.2.4).
 * <ul>
 *   <li>{@code HR_OFFICER} - logs tasks, records actual effort, updates status.</li>
 *   <li>{@code HR_MANAGER} - monitors the board, dashboard and workload balance.</li>
 *   <li>{@code ADMIN}      - manages users, task types and model retraining.</li>
 * </ul>
 */
public enum Role {
    HR_OFFICER,
    HR_MANAGER,
    ADMIN
}
