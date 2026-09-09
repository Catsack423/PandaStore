package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.seller.SellerApplication;
import project.project.Entity.seller.SellerApplicationStatus;

import java.util.List;

public interface SellerApplicationRepository extends JpaRepository<SellerApplication, Long> {
    List<SellerApplication> findByStatus(SellerApplicationStatus status);
    List<SellerApplication> findByUser_UserId(Long userId);
}
