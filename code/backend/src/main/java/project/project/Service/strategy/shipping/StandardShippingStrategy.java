package project.project.Service.strategy.shipping;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class StandardShippingStrategy implements ShippingFeeStrategy {

    private static final BigDecimal FEE = new BigDecimal("30.00");

    @Override
    public String getCourierCode() {
        return "STANDARD";
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
