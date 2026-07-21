package rw.ac.uok.taskms.dashboard;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rw.ac.uok.taskms.common.NotFoundException;
import rw.ac.uok.taskms.dashboard.DashboardService.DashboardResponse;
import rw.ac.uok.taskms.user.User;
import rw.ac.uok.taskms.user.UserRepository;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserRepository userRepository;

    public DashboardController(DashboardService dashboardService, UserRepository userRepository) {
        this.dashboardService = dashboardService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public DashboardResponse dashboard(Authentication auth) {
        User current = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new NotFoundException("User not found: " + auth.getName()));
        return dashboardService.build(current);
    }
}
