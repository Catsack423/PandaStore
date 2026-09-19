package project.project.DTO.order;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import project.project.Entity.order.PaymentMethod;

import java.util.Map;

public record CreateOrderGroupRequest(

        @NotNull(message = "กรุณาระบุรหัสลูกค้า") @Positive(message = "รหัสลูกค้าต้องมากกว่า 0") Long customerId,

        @NotNull(message = "กรุณาระบุที่อยู่จัดส่ง") @Positive(message = "รหัสที่อยู่ต้องมากกว่า 0") Long shippingAddressId,

        @NotEmpty(message = "กรุณาระบุวิธีจัดส่งของแต่ละร้านค้า") Map<@NotNull @Positive Long, @NotBlank(message = "กรุณาระบุวิธีจัดส่ง") String> sellerShippingMethods,

        @NotNull(message = "กรุณาระบุวิธีชำระเงิน") PaymentMethod paymentMethod) {
}