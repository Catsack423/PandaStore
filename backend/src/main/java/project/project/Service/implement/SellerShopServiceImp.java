package project.project.Service.implement;

import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import project.project.Entity.seller.Seller;
import project.project.Entity.seller.SellerBankAccount;
import project.project.Entity.seller.SellerStatus;
import project.project.Repository.SellerRepository;
import project.project.Repository.SellerBankAccountRepository;
import project.project.Service.api.SellerShopService;

import java.util.Optional;

@Service
public class SellerShopServiceImp implements SellerShopService {

    private final SellerRepository sellerRepository;
    private final SellerBankAccountRepository sellerBankAccountRepository;

    public SellerShopServiceImp(SellerRepository sellerRepository,
                                 SellerBankAccountRepository sellerBankAccountRepository) {
        this.sellerRepository = sellerRepository;
        this.sellerBankAccountRepository = sellerBankAccountRepository;
    }


     @param sellerId 
     @return Seller entity
     @throws RuntimeException 

    @Override
    public Seller getShopBySellerId(Long sellerId) {
        return sellerRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException(
                        "ไม่พบร้านค้า sellerId: " + sellerId));
    }


    userId (ใช้กรณี User ล็อกอินแล้วต้องการดูร้านค้าของตัวเอง)
     @param userId 
     @return Seller entity
     @throws RuntimeException 

    @Override
    public Seller getShopByUserId(Long userId) {
        return sellerRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new RuntimeException(
                        "ไม่พบร้านค้าสำหรับ userId: " + userId));
    }

    /**
     * อัปเดตข้อมูลโปรไฟล์ร้านค้า
     * ตรวจสอบว่า Seller ยังมีสถานะ ACTIVE ก่อนอนุญาตให้แก้ไข
     *
     * @param sellerId    รหัสผู้ขาย
     * @param updatedInfo ข้อมูลร้านค้าที่ต้องการอัปเดต
     * @return Seller ที่อัปเดตแล้ว
     * @throws RuntimeException ถ้า Seller ไม่พบ หรือสถานะไม่ใช่ ACTIVE
     */
    @Override
    @Transactional
    public Seller updateShopProfile(Long sellerId, Seller updatedInfo) {
        Seller existingSeller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException(
                        "ไม่พบร้านค้า sellerId: " + sellerId));

        // ตรวจสอบสถานะร้านค้า — ต้อง ACTIVE เท่านั้นจึงจะแก้ไขได้
        if (existingSeller.getStatus() != SellerStatus.ACTIVE) {
            throw new RuntimeException(
                    "ไม่สามารถแก้ไขร้านค้าที่มีสถานะ: " + existingSeller.getStatus());
        }

        // อัปเดตเฉพาะฟิลด์ที่มีค่า (Partial Update)
        if (updatedInfo.getShopName() != null && !updatedInfo.getShopName().isBlank()) {
            existingSeller.setShopName(updatedInfo.getShopName());
        }
        if (updatedInfo.getShopDescription() != null) {
            existingSeller.setShopDescription(updatedInfo.getShopDescription());
        }
        if (updatedInfo.getShopPhone() != null && !updatedInfo.getShopPhone().isBlank()) {
            existingSeller.setShopPhone(updatedInfo.getShopPhone());
        }
        if (updatedInfo.getShopEmail() != null && !updatedInfo.getShopEmail().isBlank()) {
            existingSeller.setShopEmail(updatedInfo.getShopEmail());
        }
        if (updatedInfo.getShopAddress() != null && !updatedInfo.getShopAddress().isBlank()) {
            existingSeller.setShopAddress(updatedInfo.getShopAddress());
        }

        return sellerRepository.save(existingSeller);
    }

    /**
     * เพิ่มหรืออัปเดตบัญชีธนาคารของ Seller
     * - ถ้ามีบัญชีอยู่แล้ว → อัปเดตข้อมูล
     * - ถ้าไม่มี → สร้างบัญชีใหม่
     *
     * @param sellerId    รหัสผู้ขาย
     * @param bankAccount ข้อมูลบัญชีธนาคาร
     * @return SellerBankAccount ที่บันทึกแล้ว
     */
    @Override
    @Transactional
    public SellerBankAccount addOrUpdateBankAccount(Long sellerId, SellerBankAccount bankAccount) {
        // ตรวจว่า Seller มีอยู่จริง
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException(
                        "ไม่พบร้านค้า sellerId: " + sellerId));

        // ค้นหาบัญชีธนาคารที่มีอยู่
        Optional<SellerBankAccount> existingAccount =
                sellerBankAccountRepository.findBySeller_SellerId(sellerId);

        if (existingAccount.isPresent()) {
            // อัปเดตบัญชีที่มีอยู่
            SellerBankAccount existing = existingAccount.get();
            existing.setBankName(bankAccount.getBankName());
            existing.setAccountNumber(bankAccount.getAccountNumber());
            existing.setAccountName(bankAccount.getAccountName());
            existing.setProofImageUrl(bankAccount.getProofImageUrl());
            return sellerBankAccountRepository.save(existing);
        } else {
            // สร้างบัญชีใหม่
            bankAccount.setSeller(seller);
            return sellerBankAccountRepository.save(bankAccount);
        }
    }

    /**
     * ค้นหาบัญชีธนาคารของ Seller
     * @param sellerId รหัสผู้ขาย
     * @return SellerBankAccount entity
     * @throws RuntimeException ถ้าไม่พบบัญชีธนาคาร
     */
    @Override
    public SellerBankAccount getBankAccountBySellerId(Long sellerId) {
        return sellerBankAccountRepository.findBySeller_SellerId(sellerId)
                .orElseThrow(() -> new RuntimeException(
                        "ไม่พบบัญชีธนาคารสำหรับ sellerId: " + sellerId));
    }
}
