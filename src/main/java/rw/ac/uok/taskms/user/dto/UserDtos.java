package rw.ac.uok.taskms.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import rw.ac.uok.taskms.user.Role;
import rw.ac.uok.taskms.user.User;

public class UserDtos {

    public record UserResponse(Long id, String fullName, String email, Role role, boolean active) {
        public static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.isActive());
        }
    }

    public record CreateUserRequest(
            @NotBlank String fullName,
            @Email @NotBlank String email,
            @NotBlank String password,
            @NotNull Role role) {
    }

    public record UpdateUserRequest(
            @NotBlank String fullName,
            @NotNull Role role,
            boolean active,
            String password) {
    }
}
