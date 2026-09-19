package project.project.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.seller.SellerApplication;
import project.project.Entity.seller.SellerApplicationStatus;

public interface SellerApplicationRepository extends JpaRepository<SellerApplication, Long> {

    List<SellerApplication> findByStatus(SellerApplicationStatus status);
    Optional<SellerApplication> findByUser_UserId(Long userId);
}
