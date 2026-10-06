package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.order.Payment;
import project.project.Entity.order.PaymentStatus;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderGroup_OrderGroupId(Long orderGroupId);
    Optional<Payment> findByGatewayTransactionId(String gatewayTransactionId);
    List<Payment> findByStatus(PaymentStatus status);
}
