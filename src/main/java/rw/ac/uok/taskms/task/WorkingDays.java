package rw.ac.uok.taskms.task;

import java.time.DayOfWeek;
import java.time.LocalDate;

/** Working-day (Mon–Fri) arithmetic used for deadline-risk calculations. */
public final class WorkingDays {

    private WorkingDays() {
    }

    /** Working days from {@code from} (inclusive) up to {@code to} (inclusive). Zero if to < from. */
    public static long between(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            return 0;
        }
        long count = 0;
        LocalDate d = from;
        while (!d.isAfter(to)) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                count++;
            }
            d = d.plusDays(1);
        }
        return count;
    }
}
