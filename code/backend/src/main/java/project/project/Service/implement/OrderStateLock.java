package project.project.Service.implement;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import project.project.Entity.order.Order;
import project.project.Entity.order.OrderGroup;
import project.project.Entity.order.Payment;
import project.project.Repository.OrderGroupRepository;

/** Serialize transitions within a multi-shop checkout and refresh state loaded before the lock. */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class OrderStateLock {
    private final OrderGroupRepository groups;
    private final EntityManager entities;
    public OrderStateLock(OrderGroupRepository groups, EntityManager entities) { this.groups = groups; this.entities = entities; }

    public void lock(Order order) {
        if (alreadyLocked(order.getOrderGroup())) return;
        lockGroup(order.getOrderGroup()); entities.refresh(order);
    }
    public void lock(Payment payment) {
        if (alreadyLocked(payment.getOrderGroup())) return;
        lockGroup(payment.getOrderGroup()); entities.refresh(payment);
    }
    public void refreshLockedProduct(project.project.Entity.product.Product product) { entities.refresh(product); }
    public void lockGroup(OrderGroup group) {
        if (group == null) throw new IllegalStateException("Order group is missing");
        if (alreadyLocked(group)) return;
        groups.findByIdForUpdate(group.getOrderGroupId()).orElseThrow(() -> new IllegalStateException("Order group is missing"));
        entities.refresh(group);
    }
    private boolean alreadyLocked(OrderGroup group) {
        // Refresh cascades to orders/payment. Repeating it inside a refund would discard
        // changes made earlier in the same transaction, so refresh only on first acquisition.
        return group != null && entities.contains(group) && entities.getLockMode(group) == LockModeType.PESSIMISTIC_WRITE;
    }
}
