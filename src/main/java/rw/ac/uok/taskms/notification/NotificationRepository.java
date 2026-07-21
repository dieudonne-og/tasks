package rw.ac.uok.taskms.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.uok.taskms.user.User;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop50ByRecipientOrderByCreatedAtDesc(User recipient);
    long countByRecipientAndReadFalse(User recipient);
    boolean existsByRecipientAndTaskIdAndTypeAndReadFalse(User recipient, Long taskId, NotificationType type);
    Optional<Notification> findByIdAndRecipient(Long id, User recipient);
    List<Notification> findByRecipientAndReadFalse(User recipient);
}
