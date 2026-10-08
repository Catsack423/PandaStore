package project.project.DTO.order;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import project.project.Entity.order.PaymentMethod;

import java.util.Map;

public record CreateOrderGroupRequest(

        @NotNull(message = "Please provide a customer ID") @Positive(message = "Customer ID must be greater than 0") Long customerId,

        @NotNull(message = "Please specify a shipping address") @Positive(message = "Address ID must be greater than 0") Long shippingAddressId,

        @NotEmpty(message = "Please specify a shipping method for each shop") Map<@NotNull @Positive Long, @NotBlank(message = "Please specify a shipping method") String> sellerShippingMethods,

        @NotNull(message = "Please specify a payment method") PaymentMethod paymentMethod) {
}