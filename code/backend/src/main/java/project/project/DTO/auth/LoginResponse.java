package project.project.DTO.auth;

import project.project.Entity.user.User;
import project.project.Entity.user.UserRole;

public record LoginResponse(String token, String tokenType, UserRole role,
        CurrentUserResponse user) {
    public static LoginResponse from(String token, User user) {
        return new LoginResponse(token, "Bearer", user.getRole(), CurrentUserResponse.from(user));
    }
}
