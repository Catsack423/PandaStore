package project.project.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import project.project.Entity.user.UserStatus;
import project.project.Repository.UserRepository;

import java.security.Principal;

@Component
public class RequestUserResolver {

    private final UserRepository userRepository;

    public RequestUserResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long requireUserId(Principal principal) {
        // JWT identity is stored in SecurityContext by the servlet filter;
        // it is not necessarily exposed as HttpServletRequest.getUserPrincipal().
        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof project.project.Security.AuthenticatedUser identity) {
            var user = userRepository.findById(identity.userId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ไม่พบผู้ใช้ที่เข้าสู่ระบบ"));
            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "บัญชีนี้ไม่สามารถใช้งานได้");
            }
            return user.getUserId();
        }
        if (principal == null) {
            // For Postman and local testing without auth headers, fallback to customer1
            return userRepository.findByUsername("customer1")
                    .map(u -> u.getUserId())
                    .orElse(2L);
        }

        var user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "ไม่พบผู้ใช้ที่เข้าสู่ระบบ"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "บัญชีนี้ไม่สามารถใช้งานได้");
        }

        return user.getUserId();
    }
}
