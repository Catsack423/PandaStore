package project.project.Service.strategy.shipping;

import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ShippingFeeStrategyFactory {

    private final List<ShippingFeeStrategy> strategies;
    private final DefaultShippingStrategy defaultStrategy;

    public ShippingFeeStrategyFactory(List<ShippingFeeStrategy> strategies, DefaultShippingStrategy defaultStrategy) {
        this.strategies = strategies;
        this.defaultStrategy = defaultStrategy;
    }

    public ShippingFeeStrategy getStrategy(String shippingMethod) {
        if (shippingMethod == null || shippingMethod.isBlank()) {
            return defaultStrategy;
        }

        return strategies.stream()
                .filter(s -> !(s instanceof DefaultShippingStrategy))
                .filter(s -> s.supports(shippingMethod))
                .findFirst()
                .orElse(defaultStrategy);
    }

    public List<String> getAvailableMethods() {
        return strategies.stream()
                .filter(s -> !(s instanceof DefaultShippingStrategy))
                .map(ShippingFeeStrategy::getCourierCode).sorted().toList();
    }

    public String requireSupportedMethod(String method) {
        ShippingFeeStrategy strategy = getStrategy(method);
        if (strategy instanceof DefaultShippingStrategy) {
            throw new IllegalArgumentException("Unsupported shipping method: " + method);
        }
        return strategy.getCourierCode();
    }
}
