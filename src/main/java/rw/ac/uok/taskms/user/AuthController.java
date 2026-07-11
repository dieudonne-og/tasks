package rw.ac.uok.taskms.user;

import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import rw.ac.uok.taskms.common.ApiException;
import rw.ac.uok.taskms.security.AppUserPrincipal;
import rw.ac.uok.taskms.security.CurrentUser;
import rw.ac.uok.taskms.security.JwtService;
import rw.ac.uok.taskms.user.dto.LoginRequest;
import rw.ac.uok.taskms.user.dto.LoginResponse;
import rw.ac.uok.taskms.user.dto.UserDto;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CurrentUser currentUser;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          CurrentUser currentUser) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.currentUser = currentUser;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = ((AppUserPrincipal) authentication.getPrincipal()).getUser();
        if (!user.isActive()) {
            throw ApiException.forbidden("This account has been deactivated");
        }
        String token = jwtService.generateToken(user);
        return new LoginResponse(token, UserDto.from(user));
    }

    @GetMapping("/me")
    public UserDto me() {
        return UserDto.from(currentUser.require());
    }
}
