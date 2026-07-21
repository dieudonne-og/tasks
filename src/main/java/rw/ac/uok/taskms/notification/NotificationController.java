package rw.ac.uok.taskms.notification;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import rw.ac.uok.taskms.common.NotFoundException;
import rw.ac.uok.taskms.user.User;
import rw.ac.uok.taskms.user.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    public record NotificationResponse(Long id, String type, String message, Long taskId,
                                       boolean read, Instant createdAt) {
        static NotificationResponse from(Notification n) {
            return new NotificationResponse(n.getId(), n.getType().name(), n.getMessage(),
                    n.getTaskId(), n.isRead(), n.getCreatedAt());
        }
    }

    @GetMapping
    public List<NotificationResponse> list(Authentication auth) {
        return notificationService.list(currentUser(auth)).stream()
                .map(NotificationResponse::from).toList();
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(Authentication auth) {
        return Map.of("count", notificationService.unreadCount(currentUser(auth)));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id, Authentication auth) {
        notificationService.markRead(id, currentUser(auth));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Authentication auth) {
        notificationService.markAllRead(currentUser(auth));
        return ResponseEntity.noContent().build();
    }

    private User currentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new NotFoundException("User not found: " + auth.getName()));
    }
}
