package project.project.Service.strategy.shipping;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class ThailandPostShippingStrategy implements ShippingFeeStrategy {

    private static final BigDecimal FEE = new BigDecimal("35.00");

    @Override
    public String getCourierCode() {
        return "THAILANDPOST";
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
