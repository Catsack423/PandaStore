package project.project.Service.implement;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import project.project.Service.api.ShippingFeeCalculator;
import project.project.Service.api.ShippingService;
import project.project.Service.strategy.shipping.ShippingFeeStrategyFactory;
import java.math.BigDecimal;

/** Order creation and checkout quotes must use the same shipping policy. */
@Service
public class ConfiguredShippingFeeCalculator implements ShippingFeeCalculator {
    private final ShippingService shipping;
    private final ShippingFeeStrategyFactory methods;

    public ConfiguredShippingFeeCalculator(ShippingService shipping, ShippingFeeStrategyFactory methods) {
        this.shipping = shipping;
        this.methods = methods;
    }

    @Override
    public BigDecimal calculateShippingFee(Long sellerId, String shippingMethod, Long addressId) {
        Assert.notNull(sellerId, "Seller ID is required");
        Assert.notNull(addressId, "Address ID is required");
        return shipping.calculateShippingFee(sellerId, methods.requireSupportedMethod(shippingMethod), addressId);
    }
}
