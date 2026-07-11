package rw.ac.uok.taskms.user.dto;

import rw.ac.uok.taskms.user.Role;
import rw.ac.uok.taskms.user.User;

/** Public representation of a user (never exposes the password hash). */
public record UserDto(Long id, String fullName, String email, Role role, boolean active) {

    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getFullName(), user.getEmail(), user.getRole(), user.isActive());
    }
}
