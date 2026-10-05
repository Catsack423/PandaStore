package project.project.DTO.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateAddressRequest(

        @NotNull(message = "Customer ID is required")
        @Positive(message = "Customer ID must be greater than 0")
        Long customerId,

        @NotBlank(message = "Recipient name is required")
        @Size(max = 100, message = "Recipient name must not exceed 100 characters")
        String receiverName,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^[0-9]{9,15}$", message = "Phone number must contain 9-15 digits")
        String phoneNumber,

        @NotBlank(message = "Address is required")
        @Size(max = 255, message = "Address must not exceed 255 characters")
        String addressLine,

        @NotBlank(message = "District is required")
        @Size(max = 100, message = "District must not exceed 100 characters")
        String district,

        @NotBlank(message = "Province is required")
        @Size(max = 100, message = "Province must not exceed 100 characters")
        String province,

        @NotBlank(message = "Postal code is required")
        @Pattern(regexp = "^[0-9]{5}$", message = "Postal code must contain 5 digits")
        String postalCode,

        Boolean isDefault
) {
    public CreateAddressRequest {
        if (isDefault == null) {
            isDefault = false;
        }
    }
}

