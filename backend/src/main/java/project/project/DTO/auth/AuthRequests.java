package project.project.DTO.auth;

import jakarta.validation.constraints.*;

public final class AuthRequests {
    private AuthRequests() {}

    public record RegisterCustomer(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Email @Size(max = 100) String email,
            @NotBlank @Size(min = 6, max = 72) String password,
            @NotBlank @Size(max = 72) String confirmPassword,
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Pattern(regexp = "[0-9]{9,15}") String phoneNumber) {}

    public record RegisterSeller(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Email @Size(max = 100) String email,
            @NotBlank @Size(min = 6, max = 72) String password,
            @NotBlank @Size(max = 72) String confirmPassword) {}

    public record Login(@NotBlank @Size(max = 100) String usernameOrEmail,
            @NotBlank @Size(max = 72) String password) {}

    public record ResetPassword(@NotBlank @Size(max = 72) String currentPassword,
            @NotBlank @Size(min = 6, max = 72) String password,
            @NotBlank @Size(max = 72) String confirmPassword) {}
}
