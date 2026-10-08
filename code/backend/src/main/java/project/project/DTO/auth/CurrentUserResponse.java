package project.project.DTO.auth;

import project.project.Entity.user.User;
import project.project.Entity.user.UserRole;
import project.project.Entity.user.UserStatus;

/** Account fields safe to return to the authenticated owner. */
public record CurrentUserResponse(Long userId, String username, String email,
        UserRole role, UserStatus status, String fullName, String phoneNumber) {
    public static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(user.getUserId(), user.getUsername(), user.getEmail(),
                user.getRole(), user.getStatus(), null, null);
    }

    public static CurrentUserResponse from(User user, String fullName, String phoneNumber) {
        return new CurrentUserResponse(user.getUserId(), user.getUsername(), user.getEmail(),
                user.getRole(), user.getStatus(), fullName, phoneNumber);
    }
}
