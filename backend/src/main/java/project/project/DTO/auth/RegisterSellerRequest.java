package project.project.DTO.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterSellerRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(min = 6) String password,
        @NotBlank String confirmPassword,
        @NotBlank @Size(max = 100) String shopName,
        @NotBlank String shopDescription,
        @NotBlank @Pattern(regexp = "^[0-9]{9,15}$") String shopPhone,
        @NotBlank @Email @Size(max = 100) String shopEmail,
        @NotBlank @Size(max = 255) String shopAddress,
        @NotBlank @Size(max = 100) String sellerFirstName,
        @NotBlank @Size(max = 100) String sellerLastName,
        @NotBlank @Pattern(regexp = "^[0-9]{13}$") String idCardNumber,
        @NotBlank @Size(max = 255) String idCardImageUrl,
        @NotBlank @Size(max = 100) String bankAccountName,
        @NotBlank @Size(max = 100) String bankName,
        @NotBlank @Size(max = 30) String bankAccountNumber,
        @NotBlank @Size(max = 255) String bankBookImageUrl) {
}
