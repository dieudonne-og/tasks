package rw.ac.uok.taskms.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import rw.ac.uok.taskms.common.ApiException;
import rw.ac.uok.taskms.user.User;

/** Convenience accessor for the authenticated user in the current request. */
@Component
public class CurrentUser {

    public User require() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AppUserPrincipal principal) {
            return principal.getUser();
        }
        throw ApiException.forbidden("Not authenticated");
    }
}
