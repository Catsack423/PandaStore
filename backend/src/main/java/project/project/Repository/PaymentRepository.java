package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.order.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

}
