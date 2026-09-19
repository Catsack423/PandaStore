package project.project.Service.implement;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import project.project.Service.api.ShippingFeeCalculator;

import java.math.BigDecimal;
import java.util.Locale;

@Service
public class ConfiguredShippingFeeCalculator
        implements ShippingFeeCalculator {

    private final Environment environment;

    public ConfiguredShippingFeeCalculator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public BigDecimal calculateShippingFee(
            Long sellerId,
            String shippingMethod,
            Long addressId) {
        Assert.notNull(sellerId, "Seller ID is required");
        Assert.notNull(addressId, "Address ID is required");
        Assert.hasText(shippingMethod, "Shipping method is required");

        String method = shippingMethod.trim().toLowerCase(Locale.ROOT);

        Assert.isTrue(
                method.matches("[a-z0-9_-]+"),
                "Invalid shipping method");

        String propertyName = "shipping.fees." + sellerId + "." + method;
        String configuredFee = environment.getProperty(propertyName);

        if (configuredFee == null || configuredFee.isBlank()) {
            throw new IllegalArgumentException(
                    "Shipping method is unavailable for seller: " + sellerId);
        }

        BigDecimal fee;

        try {
            fee = new BigDecimal(configuredFee.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException(
                    "Invalid shipping fee configuration",
                    ex);
        }

        if (fee.signum() < 0 || fee.stripTrailingZeros().scale() > 2) {
            throw new IllegalStateException(
                    "Shipping fee must be non-negative with at most 2 decimals");
        }

        // นโยบายนี้คิดราคาเดียวทุกพื้นที่
        // OrderOrchestrationService ตรวจเจ้าของที่อยู่ก่อนเรียกแล้ว
        return fee.setScale(2);
    }
}