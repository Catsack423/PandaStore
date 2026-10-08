package project.project.Service.api;

import project.project.Entity.seller.Seller;
import project.project.Entity.seller.SellerBankAccount;

public interface SellerShopService {
    Seller getShopBySellerId(Long sellerId);
    Seller getShopByUserId(Long userId);
    Seller updateShopProfile(Long sellerId, Seller updatedInfo);
    SellerBankAccount addOrUpdateBankAccount(Long sellerId, SellerBankAccount bankAccount);
    SellerBankAccount getBankAccountBySellerId(Long sellerId);
}
