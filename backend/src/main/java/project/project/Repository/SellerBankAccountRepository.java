package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.seller.SellerBankAccount;

import java.util.Optional;

public interface SellerBankAccountRepository extends JpaRepository<SellerBankAccount, Long> {
    Optional<SellerBankAccount> findBySeller_SellerId(Long sellerId);
}
