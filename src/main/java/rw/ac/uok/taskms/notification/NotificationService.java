package rw.ac.uok.taskms.notification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.uok.taskms.common.NotFoundException;
import rw.ac.uok.taskms.user.User;

import java.util.List;

/** Creates and serves in-app notifications (prediction, risk and deadline alerts). */
@Service
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    /** Always creates a notification for {@code recipient}. */
    @Transactional
    public void notify(User recipient, NotificationType type, String message, Long taskId) {
        if (recipient == null) {
            return;
        }
        repository.save(new Notification(recipient, type, message, taskId));
    }

    /**
     * Creates a notification only if the recipient has no unread notification of the same
     * type for the same task — prevents deadline/risk alerts from repeating on every scan.
     */
    @Transactional
    public void notifyOnce(User recipient, NotificationType type, String message, Long taskId) {
        if (recipient == null) {
            return;
        }
        if (taskId != null && repository.existsByRecipientAndTaskIdAndTypeAndReadFalse(recipient, taskId, type)) {
            return;
        }
        repository.save(new Notification(recipient, type, message, taskId));
    }

    public List<Notification> list(User recipient) {
        return repository.findTop50ByRecipientOrderByCreatedAtDesc(recipient);
    }

    public long unreadCount(User recipient) {
        return repository.countByRecipientAndReadFalse(recipient);
    }

    @Transactional
    public void markRead(Long id, User recipient) {
        Notification n = repository.findByIdAndRecipient(id, recipient)
                .orElseThrow(() -> new NotFoundException("Notification not found: " + id));
        n.setRead(true);
        repository.save(n);
    }

    @Transactional
    public void markAllRead(User recipient) {
        List<Notification> unread = repository.findByRecipientAndReadFalse(recipient);
        unread.forEach(n -> n.setRead(true));
        repository.saveAll(unread);
    }
}
