package project.project.Controller;

import java.security.Principal;

import org.springframework.stereotype.Component;

import project.project.Security.CurrentUser;

@Component
public class RequestUserResolver {

    private final CurrentUser currentUser;

    public RequestUserResolver(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    public Long requireUserId(Principal principal) {
<<<<<<< HEAD
        return currentUser.getCurrentUserId();
=======
        // JWT identity is stored in SecurityContext by the servlet filter;
        // it is not necessarily exposed as HttpServletRequest.getUserPrincipal().
        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof project.project.Security.AuthenticatedUser identity) {
            var user = userRepository.findById(identity.userId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Logged-in user not found"));
            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is unavailable");
            }
            return user.getUserId();
        }
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in");
        }

        var user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Logged-in user not found"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This account is unavailable");
        }

        return user.getUserId();
>>>>>>> develop
    }
}
