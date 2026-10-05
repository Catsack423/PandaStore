package project.project.Security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.project.Entity.order.Order;
import project.project.Entity.seller.SellerStatus;
import project.project.Entity.user.UserRole;
import project.project.Repository.OrderGroupRepository;
import project.project.Repository.OrderRepository;
import project.project.Repository.SellerRepository;

/**
 * Request-thread authorization shared by the existing order and shipment
 * endpoints.
 */
@Component
@Transactional(readOnly = true)
public class OrderAccess {
    private final CurrentUser currentUser;
    private final SellerRepository sellers;
    private final OrderRepository orders;
    private final OrderGroupRepository groups;

    public OrderAccess(CurrentUser currentUser, SellerRepository sellers, OrderRepository orders,
            OrderGroupRepository groups) {
        this.currentUser = currentUser;
        this.sellers = sellers;
        this.orders = orders;
        this.groups = groups;
    }

    public void requireSeller(Long sellerId) {
        var identity = currentUser.requireIdentity();
        if (identity.role() != UserRole.SELLER)
            throw denied();
        var shop = sellers.findByUser_UserId(identity.userId()).orElseThrow(OrderAccess::denied);
        if (shop.getStatus() != SellerStatus.ACTIVE || !shop.getSellerId().equals(sellerId))
            throw denied();
    }

    public void requireShopRead(Long sellerId) {
        if (currentUser.requireIdentity().role() != UserRole.ADMIN)
            requireSeller(sellerId);
    }

    public void requireOrderRead(Order order) {
        var identity = currentUser.requireIdentity();
        if (identity.role() == UserRole.ADMIN)
            return;
        if (identity.role() == UserRole.SELLER) {
            requireSeller(order.getSeller().getSellerId());
            return;
        }
        var customer = order.getOrderGroup().getCustomer();
        if (identity.role() != UserRole.CUSTOMER || !customer.getUser().getUserId().equals(identity.userId()))
            throw denied();
    }

    public void requireOrderRead(Long orderId) {
        currentUser.requireIdentity();
        requireOrderRead(order(orderId));
    }

    public void requireSellerOrder(Long sellerId, Long orderId) {
        requireSeller(sellerId);
        if (!order(orderId).getSeller().getSellerId().equals(sellerId))
            throw denied();
    }

    public void requireGroupRead(Long groupId) {
        var identity = currentUser.requireIdentity();
        if (identity.role() == UserRole.ADMIN)
            return;
        if (identity.role() != UserRole.CUSTOMER)
            throw denied();
        var group = groups.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!group.getCustomer().getUser().getUserId().equals(identity.userId()))
            throw denied();
    }

    public Long resolveCustomerId(Long customerId) {
        return customerId != null ? customerId : currentUser.requireCustomerId();
    }

    public void requireCustomerOrder(Long customerId, Long orderId) {
        if (!currentUser.requireCustomerId().equals(customerId))
            throw denied();
        if (!order(orderId).getOrderGroup().getCustomer().getCustomerId().equals(customerId))
            throw denied();
    }

    public void requireAdmin() {
        if (currentUser.requireIdentity().role() != UserRole.ADMIN)
            throw denied();
    }

    private Order order(Long orderId) {
        return orders.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private static ResponseStatusException denied() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "This order is not available to your account");
    }
}
