package project.project.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.seller.SellerBankAccount;

import java.util.Optional;

public interface SellerBankAccountRepository extends JpaRepository<SellerBankAccount, Long> {
    Optional<SellerBankAccount> findBySeller_SellerId(Long sellerId);
}
