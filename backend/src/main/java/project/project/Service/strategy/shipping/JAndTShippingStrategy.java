package project.project.Service.strategy.shipping;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class JAndTShippingStrategy implements ShippingFeeStrategy {

    private static final BigDecimal FEE = new BigDecimal("45.00");

    @Override
    public String getCourierCode() {
        return "J&T";
    }

    @Override
    public boolean supports(String shippingMethod) {
        if (shippingMethod == null) {
            return false;
        }
        String clean = shippingMethod.trim().toUpperCase();
        return clean.equals("J&T") || clean.equals("JT") || clean.equals("J_AND_T");
    }

    @Override
    public BigDecimal calculateFee(Long sellerId, Long addressId) {
        return FEE;
    }
}
