package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.seller.Seller;

import java.util.Optional;

public interface SellerRepository extends JpaRepository<Seller, Long> {
    Optional<Seller> findByUser_UserId(Long userId);
    boolean existsByShopName(String shopName);
}
