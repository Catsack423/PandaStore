package project.project.Security;

import java.security.Principal;
import project.project.Entity.user.UserRole;

/** Immutable identity only: no token, password or JPA entity. */
public record AuthenticatedUser(long userId, UserRole role) implements Principal {
    @Override
    public String getName() {
        return Long.toString(userId);
    }
}
