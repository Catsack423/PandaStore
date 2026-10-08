package project.project.Service.api;

import java.math.BigDecimal;

public interface ShippingFeeCalculator {

    BigDecimal calculateShippingFee(
            Long sellerId,
            String shippingMethod,
            Long addressId);
}