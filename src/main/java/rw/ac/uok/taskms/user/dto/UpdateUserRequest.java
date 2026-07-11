package rw.ac.uok.taskms.user.dto;

import jakarta.validation.constraints.Email;
import rw.ac.uok.taskms.user.Role;

/** All fields optional; only non-null values are applied. */
public record UpdateUserRequest(
        String fullName,
        @Email String email,
        Role role,
        Boolean active,
        String password) {
}
