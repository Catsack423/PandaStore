package project.project.DTO.seller;

import project.project.Entity.seller.Seller;
import project.project.Entity.seller.SellerApplication;
import project.project.Entity.seller.SellerBankAccount;
import project.project.Entity.seller.SellerStatus;

public record SellerResponse(
        Long sellerId,
        Long userId,
        String shopName,
        String shopDescription,
        String shopPhone,
        String shopEmail,
        String shopAddress,
        SellerStatus status,
        Long applicationId,
        Long bankAccountId) {
    public static SellerResponse fromEntity(Seller seller, SellerApplication application,
            SellerBankAccount bankAccount) {
        return new SellerResponse(
                seller.getSellerId(),
                seller.getUser() != null ? seller.getUser().getUserId() : null,
                seller.getShopName(),
                seller.getShopDescription(),
                seller.getShopPhone(),
                seller.getShopEmail(),
                seller.getShopAddress(),
                seller.getStatus(),
                application != null ? application.getApplicationId() : null,
                bankAccount != null ? bankAccount.getBankAccountId() : null);
    }
}
