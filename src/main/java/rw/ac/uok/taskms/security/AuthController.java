package rw.ac.uok.taskms.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import rw.ac.uok.taskms.user.User;
import rw.ac.uok.taskms.user.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
                         UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
    public record LoginResponse(String token, Long userId, String fullName, String email, String role) {}
    public record MeResponse(Long userId, String fullName, String email, String role) {}

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        User user = userService.findByEmail(auth.getName());
        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return new LoginResponse(token, user.getId(), user.getFullName(),
                user.getEmail(), user.getRole().name());
    }

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        return new MeResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole().name());
    }
}
