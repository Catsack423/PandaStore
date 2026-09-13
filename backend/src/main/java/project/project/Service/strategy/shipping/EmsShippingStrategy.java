package project.project.Service.strategy.shipping;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class EmsShippingStrategy implements ShippingFeeStrategy {

    private static final BigDecimal FEE = new BigDecimal("60.00");

    @Override
    public String getCourierCode() {
        return "EMS";
    }

    @Override
    public boolean supports(String shippingMethod) {
        return shippingMethod != null && getCourierCode().equalsIgnoreCase(shippingMethod.trim());
    }

    @Override
    public BigDecimal calculateFee(Long sellerId, Long addressId) {
        return FEE;
    }
}
