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
            @Size(max = 72) String confirmPassword,
            @NotBlank @Size(max = 100) String shopName,
            @NotBlank String shopDescription,
            @NotBlank @Pattern(regexp = "[0-9]{9,15}") String shopPhone,
            @NotBlank @Email @Size(max = 100) String shopEmail,
            @NotBlank @Size(max = 255) String shopAddress,
            @NotBlank @Size(max = 100) String sellerFirstName,
            @NotBlank @Size(max = 100) String sellerLastName,
            @NotBlank @Pattern(regexp = "[0-9]{13}") String idCardNumber,
            @NotBlank @Size(max = 100) String bankName,
            @NotBlank @Size(max = 100) String bankAccountName,
            @NotBlank @Pattern(regexp = "[0-9]{10,15}") String bankAccountNumber,
            String idCardImageUrl,
            String bankBookImageUrl,
            String proofImageUrl) {}

    public record Login(@NotBlank @Size(max = 100) String usernameOrEmail,
            @NotBlank @Size(max = 72) String password) {}

    public record ResetPassword(@NotBlank @Size(max = 72) String currentPassword,
            @NotBlank @Size(min = 6, max = 72) String password,
            @NotBlank @Size(max = 72) String confirmPassword) {}
}
