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

/** Inject in controllers/services on the request thread.
 * Background/async work must receive identity explicitly; context is not inherited. */
@Component
public class CurrentUser {
    private final UserRepository users;
    private final CustomerRepository customers;
    public CurrentUser(UserRepository users, CustomerRepository customers) {
        this.users = users;
        this.customers = customers;
    }
    public AuthenticatedUser requireIdentity() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedUser identity)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "กรุณาเข้าสู่ระบบ");
        }
        return identity;
    }
    public long getCurrentUserId() { return requireIdentity().userId(); }
    @Transactional(readOnly = true)
    public User requireUser() {
        return users.findById(getCurrentUserId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ไม่พบผู้ใช้ปัจจุบัน"));
    }
    @Transactional(readOnly = true)
    public Long requireCustomerId() {
        var identity = requireIdentity();
        if (identity.role() != UserRole.CUSTOMER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "บัญชีนี้ไม่ใช่ลูกค้า");
        }
        return customers.findByUser_UserId(identity.userId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.FORBIDDEN, "ไม่พบข้อมูลลูกค้า")).getCustomerId();
    }
}
