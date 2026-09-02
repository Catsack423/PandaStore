package project.project.Service.api;

import project.project.Entity.user.User;

public interface AuthService {
    User register(String username, String email, String password, String confirmPassword);
    String login(String usernameOrEmail, String password);
    boolean validateToken(String token);
}
