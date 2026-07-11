package rw.ac.uok.taskms.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import rw.ac.uok.taskms.common.BadRequestException;
import rw.ac.uok.taskms.common.NotFoundException;
import rw.ac.uok.taskms.user.dto.UserDtos.CreateUserRequest;
import rw.ac.uok.taskms.user.dto.UserDtos.UpdateUserRequest;

import java.util.List;

@Service
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> findAll() {
        return repository.findAll();
    }

    public User findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    public User findByEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found: " + email));
    }

    public User create(CreateUserRequest req) {
        if (repository.existsByEmail(req.email())) {
            throw new BadRequestException("Email already in use: " + req.email());
        }
        User user = new User(req.fullName(), req.email(),
                passwordEncoder.encode(req.password()), req.role());
        return repository.save(user);
    }

    public User update(Long id, UpdateUserRequest req) {
        User user = findById(id);
        user.setFullName(req.fullName());
        user.setRole(req.role());
        user.setActive(req.active());
        if (req.password() != null && !req.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(req.password()));
        }
        return repository.save(user);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("User not found: " + id);
        }
        repository.deleteById(id);
    }
}
