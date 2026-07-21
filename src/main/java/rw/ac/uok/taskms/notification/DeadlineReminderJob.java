package rw.ac.uok.taskms.notification;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rw.ac.uok.taskms.task.Task;
import rw.ac.uok.taskms.task.TaskRepository;
import rw.ac.uok.taskms.task.TaskStatus;
import rw.ac.uok.taskms.task.WorkingDays;

import java.time.LocalDate;

/**
 * Scans open tasks for approaching deadlines and notifies assignees one working day
 * before the due date and again on/after the due date (research objective 3 — delay
 * warnings). Deduplicated per task+type so it fires once until the alert is read.
 */
@Component
public class DeadlineReminderJob {

    private final TaskRepository taskRepository;
    private final NotificationService notificationService;

    public DeadlineReminderJob(TaskRepository taskRepository, NotificationService notificationService) {
        this.taskRepository = taskRepository;
        this.notificationService = notificationService;
    }

    /** Runs daily at 08:00. Also runs 30s after startup so alerts appear without waiting a day. */
    @Scheduled(cron = "0 0 8 * * *")
    @Scheduled(initialDelay = 30_000, fixedDelay = Long.MAX_VALUE)
    public void scan() {
        LocalDate today = LocalDate.now();
        for (Task t : taskRepository.findAll()) {
            if (t.getDueDate() == null || t.getAssignee() == null
                    || t.getStatus() == TaskStatus.DONE || t.getStatus() == TaskStatus.CANCELLED) {
                continue;
            }
            LocalDate due = t.getDueDate();
            if (!due.isAfter(today)) {
                notificationService.notifyOnce(t.getAssignee(), NotificationType.DEADLINE_TODAY,
                        "\"" + t.getTitle() + "\" is due " + (due.isBefore(today) ? "and overdue (" + due + ")" : "today")
                                + " and is not yet complete.", t.getId());
            } else if (WorkingDays.between(today, due) == 2) {
                notificationService.notifyOnce(t.getAssignee(), NotificationType.DEADLINE_APPROACHING,
                        "\"" + t.getTitle() + "\" is due on " + due + " (1 working day left).", t.getId());
            }
        }
    }
}
