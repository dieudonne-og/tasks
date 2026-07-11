package rw.ac.uok.taskms.user;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rw.ac.uok.taskms.user.dto.UserDto;

import java.util.List;

/**
 * Read-only list of staff that tasks can be assigned to (active officers and
 * managers). Available to any authenticated user so the task form can populate
 * its assignee dropdown, without exposing full admin user management.
 */
@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final UserRepository userRepository;

    public StaffController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<UserDto> assignable() {
        return userRepository.findByActiveTrue().stream()
                .filter(u -> u.getRole() == Role.HR_OFFICER || u.getRole() == Role.HR_MANAGER)
                .map(UserDto::from)
                .toList();
    }
}
