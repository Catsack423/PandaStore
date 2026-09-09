package project.project.Service.implement;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import project.project.Entity.order.*;
import project.project.Entity.product.Product;
import project.project.Entity.user.Address;
import project.project.Entity.user.Customer;
import project.project.Service.api.OrderDraftFactory;
import project.project.Service.api.ShippingFeeCalculator;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderDraftFactoryImp implements OrderDraftFactory {

    private final ShippingFeeCalculator shippingFeeCalculator;

    public OrderDraftFactoryImp(ShippingFeeCalculator shippingFeeCalculator) {
        this.shippingFeeCalculator = shippingFeeCalculator;
    }

    @Override
    public OrderGroup create(
            Customer customer,
            Address address,
            Map<Long, Product> products,
            Map<Long, Integer> quantities,
            Map<Long, String> shippingMethods,
            PaymentMethod paymentMethod) {
        Assert.notNull(customer, "Customer is required");
        Assert.notNull(address, "Address is required");
        Assert.notEmpty(products, "Products are required");
        Assert.notNull(quantities, "Quantities are required");
        Assert.notNull(shippingMethods, "Shipping methods are required");
        Assert.notNull(paymentMethod, "Payment method is required");
        Assert.isTrue(
                products.keySet().equals(quantities.keySet()),
                "Products and quantities must match");

        OrderGroup group = new OrderGroup();
        group.setGroupNumber(UUID.randomUUID().toString());
        group.setCustomer(customer);
        group.setShippingAddress(address);
        group.setPaymentStatus(OrderGroupPaymentStatus.PENDING);
        group.setTotalDiscount(BigDecimal.ZERO);

        Map<Long, Order> ordersBySeller = new LinkedHashMap<>();

        for (var entry : quantities.entrySet()) {
            Product product = products.get(entry.getKey());
            Integer quantity = entry.getValue();

            Assert.isTrue(
                    quantity != null && quantity > 0,
                    "Quantity must be positive");

            if (product.getPrice() == null
                    || product.getPrice().signum() < 0) {
                throw new IllegalStateException(
                        "Invalid product price: " + product.getProductId());
            }

            Long sellerId = product.getSeller().getSellerId();

            Order order = ordersBySeller.computeIfAbsent(
                    sellerId,
                    id -> createSubOrder(
                            group,
                            product,
                            shippingMethods.get(id),
                            address.getAddressId()));

            BigDecimal lineTotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(quantity));

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setUnitPrice(product.getPrice());
            item.setQuantity(quantity);
            item.setTotalPrice(lineTotal);
            item.setIsReviewed(false);

            order.getOrderItems().add(item);
            order.setSubtotal(order.getSubtotal().add(lineTotal));
        }

        BigDecimal productsAmount = BigDecimal.ZERO;
        BigDecimal shippingAmount = BigDecimal.ZERO;

        for (Order order : ordersBySeller.values()) {
            order.setTotalAmount(
                    order.getSubtotal()
                            .add(order.getShippingFee())
                            .subtract(order.getSellerDiscount()));

            group.getSubOrders().add(order);
            productsAmount = productsAmount.add(order.getSubtotal());
            shippingAmount = shippingAmount.add(order.getShippingFee());
        }

        group.setTotalProductsAmount(productsAmount);
        group.setTotalShippingFee(shippingAmount);
        group.setGrandTotal(
                productsAmount.add(shippingAmount)
                        .subtract(group.getTotalDiscount()));

        Payment payment = new Payment();
        payment.setOrderGroup(group);
        payment.setPaymentMethod(paymentMethod);
        payment.setAmount(group.getGrandTotal());
        payment.setRefundedAmount(BigDecimal.ZERO);
        payment.setStatus(PaymentStatus.PENDING);

        group.setPayment(payment);

        return group;
    }

    private Order createSubOrder(
            OrderGroup group,
            Product product,
            String shippingMethod,
            Long addressId) {
        Assert.hasText(
                shippingMethod,
                "Shipping method is required for seller "
                        + product.getSeller().getSellerId());

        BigDecimal shippingFee = shippingFeeCalculator.calculateShippingFee(
                product.getSeller().getSellerId(),
                shippingMethod,
                addressId);

        if (shippingFee == null || shippingFee.signum() < 0) {
            throw new IllegalStateException("Invalid shipping fee");
        }

        Order order = new Order();
        order.setSubOrderNumber(UUID.randomUUID().toString());
        order.setOrderGroup(group);
        order.setSeller(product.getSeller());
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        order.setSubtotal(BigDecimal.ZERO);
        order.setShippingFee(shippingFee);
        order.setSellerDiscount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.ZERO);

        return order;
    }
}