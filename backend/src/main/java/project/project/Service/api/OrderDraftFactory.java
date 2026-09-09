package project.project.Service.api;

import project.project.Entity.order.OrderGroup;
import project.project.Entity.order.PaymentMethod;
import project.project.Entity.product.Product;
import project.project.Entity.user.Address;
import project.project.Entity.user.Customer;

import java.util.Map;

public interface OrderDraftFactory {

    OrderGroup create(
            Customer customer,
            Address address,
            Map<Long, Product> products,
            Map<Long, Integer> quantities,
            Map<Long, String> shippingMethods,
            PaymentMethod paymentMethod);
}