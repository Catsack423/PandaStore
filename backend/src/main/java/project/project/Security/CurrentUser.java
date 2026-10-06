package project.project.Security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.project.Entity.user.User;
import project.project.Entity.user.UserRole;
import project.project.Repository.UserRepository;
import project.project.Repository.CustomerRepository;

/**
 * Inject in controllers/services on the request thread.
 * Background/async work must receive identity explicitly; context is not
 * inherited.
 */
@Component
public class CurrentUser {
    private final UserRepository usersRepository ;
    private final CustomerRepository customersRepository;

    public CurrentUser(UserRepository users, CustomerRepository customers) {
        this.usersRepository  = users;
        this.customersRepository = customers;
    }

    public AuthenticatedUser requireIdentity() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedUser identity)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in");
        }
        return identity;
    }

    public long getCurrentUserId() {
        return requireIdentity().userId();
    }

    @Transactional(readOnly = true)
    public User requireUser() {
        return usersRepository .findById(getCurrentUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user not found"));
    }

    @Transactional(readOnly = true)
    public Long requireCustomerId() {
        var identity = requireIdentity();
        if (identity.role() != UserRole.CUSTOMER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is not a customer account");
        }
        return customersRepository.findByUser_UserId(identity.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Customer not found"))
                .getCustomerId();
    }
}
