package project.project.Service.strategy.shipping;

import java.math.BigDecimal;

public interface ShippingFeeStrategy {
    String getCourierCode();
    boolean supports(String shippingMethod);
    BigDecimal calculateFee(Long sellerId, Long addressId);
}
