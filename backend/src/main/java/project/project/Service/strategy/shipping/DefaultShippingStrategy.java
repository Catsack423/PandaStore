package project.project.Service.strategy.shipping;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class DefaultShippingStrategy implements ShippingFeeStrategy {

    private static final BigDecimal DEFAULT_FEE = new BigDecimal("50.00");

    @Override
    public String getCourierCode() {
        return "DEFAULT";
    }

    @Override
    public boolean supports(String shippingMethod) {
        return true; // Fallback for any unknown courier
    }

    @Override
    public BigDecimal calculateFee(Long sellerId, Long addressId) {
        return DEFAULT_FEE;
    }
}
