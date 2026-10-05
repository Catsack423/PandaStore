package project.project.Controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.project.ApiResponse.ApiResponse;
import project.project.DTO.seller.SellerBankAccountRequest;
import project.project.DTO.seller.SellerBankAccountResponse;
import project.project.DTO.seller.SellerShopResponse;
import project.project.DTO.seller.UpdateShopProfileRequest;
import project.project.Entity.seller.Seller;
import project.project.Entity.seller.SellerBankAccount;
import project.project.Service.api.SellerShopService;

@RestController
@RequestMapping("/api/seller/shops")
public class SellerShopController {

    private final SellerShopService sellerShopService;

    public SellerShopController(SellerShopService sellerShopService) {
        this.sellerShopService = sellerShopService;
    }

    @GetMapping("/{sellerId}")
    public ResponseEntity<ApiResponse<SellerShopResponse>> getShopBySellerId(@PathVariable Long sellerId) {
        Seller seller = sellerShopService.getShopBySellerId(sellerId);
        return ResponseEntity.ok(ApiResponse.success("ดึงข้อมูลร้านค้าสำเร็จ", SellerShopResponse.fromEntity(seller)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<SellerShopResponse>> getShopByUserId(@PathVariable Long userId) {
        Seller seller = sellerShopService.getShopByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("ดึงข้อมูลร้านค้าสำเร็จ", SellerShopResponse.fromEntity(seller)));
    }

    @PutMapping("/{sellerId}")
    public ResponseEntity<ApiResponse<SellerShopResponse>> updateShopProfile(
            @PathVariable Long sellerId,
            @Valid @RequestBody UpdateShopProfileRequest request) {

        Seller updatedInfo = new Seller();
        updatedInfo.setShopName(request.getShopName());
        updatedInfo.setShopDescription(request.getShopDescription());
        updatedInfo.setShopPhone(request.getShopPhone());
        updatedInfo.setShopEmail(request.getShopEmail());
        updatedInfo.setShopAddress(request.getShopAddress());

        Seller saved = sellerShopService.updateShopProfile(sellerId, updatedInfo);
        return ResponseEntity.ok(ApiResponse.success("อัปเดตข้อมูลร้านค้าสำเร็จ", SellerShopResponse.fromEntity(saved)));
    }

    @PutMapping("/{sellerId}/bank-account")
    public ResponseEntity<ApiResponse<SellerBankAccountResponse>> updateBankAccount(
            @PathVariable Long sellerId,
            @Valid @RequestBody SellerBankAccountRequest request) {

        SellerBankAccount bankAccount = new SellerBankAccount();
        bankAccount.setBankName(request.getBankName());
        bankAccount.setAccountNumber(request.getAccountNumber());
        bankAccount.setAccountName(request.getAccountName());
        bankAccount.setProofImageUrl(request.getProofImageUrl());

        SellerBankAccount saved = sellerShopService.addOrUpdateBankAccount(sellerId, bankAccount);
        return ResponseEntity.ok(ApiResponse.success("บันทึกข้อมูลบัญชีธนาคารสำเร็จ", SellerBankAccountResponse.fromEntity(saved)));
    }

    @PostMapping("/{sellerId}/bank-account")
    public ResponseEntity<ApiResponse<SellerBankAccountResponse>> addBankAccount(
            @PathVariable Long sellerId,
            @Valid @RequestBody SellerBankAccountRequest request) {
        return updateBankAccount(sellerId, request);
    }

    @GetMapping("/{sellerId}/bank-account")
    public ResponseEntity<ApiResponse<SellerBankAccountResponse>> getBankAccountBySellerId(@PathVariable Long sellerId) {
        SellerBankAccount account = sellerShopService.getBankAccountBySellerId(sellerId);
        return ResponseEntity.ok(ApiResponse.success("ดึงข้อมูลบัญชีธนาคารสำเร็จ", SellerBankAccountResponse.fromEntity(account)));
    }
}
