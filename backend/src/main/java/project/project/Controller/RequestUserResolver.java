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
        return currentUser.getCurrentUserId();
    }
}