package project.project.DTO.seller;

import project.project.Entity.seller.SellerApplication;
import project.project.Entity.seller.SellerApplicationStatus;

import java.time.LocalDateTime;

public class SellerApplicationResponse {

    private Long applicationId;
    private Long userId;
    private String shopName;
    private String shopDescription;
    private String shopPhone;
    private String shopEmail;
    private String shopAddress;
    private String sellerFirstName;
    private String sellerLastName;
    private String idCardNumber;
    private String idCardImageUrl;
    private String bankAccountName;
    private String bankName;
    private String bankAccountNumber;
    private String bankBookImageUrl;
    private SellerApplicationStatus status;
    private String adminNote;
    private Long reviewedById;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;

    public SellerApplicationResponse() {
    }

    public SellerApplicationResponse(Long applicationId, Long userId, String shopName, String shopDescription,
                                   String shopPhone, String shopEmail, String shopAddress, String sellerFirstName,
                                   String sellerLastName, String idCardNumber, String idCardImageUrl,
                                   String bankAccountName, String bankName, String bankAccountNumber,
                                   String bankBookImageUrl, SellerApplicationStatus status, String adminNote,
                                   Long reviewedById, LocalDateTime reviewedAt, LocalDateTime createdAt) {
        this.applicationId = applicationId;
        this.userId = userId;
        this.shopName = shopName;
        this.shopDescription = shopDescription;
        this.shopPhone = shopPhone;
        this.shopEmail = shopEmail;
        this.shopAddress = shopAddress;
        this.sellerFirstName = sellerFirstName;
        this.sellerLastName = sellerLastName;
        this.idCardNumber = idCardNumber;
        this.idCardImageUrl = idCardImageUrl;
        this.bankAccountName = bankAccountName;
        this.bankName = bankName;
        this.bankAccountNumber = bankAccountNumber;
        this.bankBookImageUrl = bankBookImageUrl;
        this.status = status;
        this.adminNote = adminNote;
        this.reviewedById = reviewedById;
        this.reviewedAt = reviewedAt;
        this.createdAt = createdAt;
    }

    public static SellerApplicationResponse fromEntity(SellerApplication application) {
        if (application == null) {
            return null;
        }
        return new SellerApplicationResponse(
                application.getApplicationId(),
                application.getUser() != null ? application.getUser().getUserId() : null,
                application.getShopName(),
                application.getShopDescription(),
                application.getShopPhone(),
                application.getShopEmail(),
                application.getShopAddress(),
                application.getSellerFirstName(),
                application.getSellerLastName(),
                application.getIdCardNumber(),
                application.getIdCardImageUrl(),
                application.getBankAccountName(),
                application.getBankName(),
                application.getBankAccountNumber(),
                application.getBankBookImageUrl(),
                application.getStatus(),
                application.getAdminNote(),
                application.getReviewedBy() != null ? application.getReviewedBy().getUserId() : null,
                application.getReviewedAt(),
                application.getCreatedAt()
        );
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getShopDescription() {
        return shopDescription;
    }

    public void setShopDescription(String shopDescription) {
        this.shopDescription = shopDescription;
    }

    public String getShopPhone() {
        return shopPhone;
    }

    public void setShopPhone(String shopPhone) {
        this.shopPhone = shopPhone;
    }

    public String getShopEmail() {
        return shopEmail;
    }

    public void setShopEmail(String shopEmail) {
        this.shopEmail = shopEmail;
    }

    public String getShopAddress() {
        return shopAddress;
    }

    public void setShopAddress(String shopAddress) {
        this.shopAddress = shopAddress;
    }

    public String getSellerFirstName() {
        return sellerFirstName;
    }

    public void setSellerFirstName(String sellerFirstName) {
        this.sellerFirstName = sellerFirstName;
    }

    public String getSellerLastName() {
        return sellerLastName;
    }

    public void setSellerLastName(String sellerLastName) {
        this.sellerLastName = sellerLastName;
    }

    public String getIdCardNumber() {
        return idCardNumber;
    }

    public void setIdCardNumber(String idCardNumber) {
        this.idCardNumber = idCardNumber;
    }

    public String getIdCardImageUrl() {
        return idCardImageUrl;
    }

    public void setIdCardImageUrl(String idCardImageUrl) {
        this.idCardImageUrl = idCardImageUrl;
    }

    public String getBankAccountName() {
        return bankAccountName;
    }

    public void setBankAccountName(String bankAccountName) {
        this.bankAccountName = bankAccountName;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    public void setBankAccountNumber(String bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
    }

    public String getBankBookImageUrl() {
        return bankBookImageUrl;
    }

    public void setBankBookImageUrl(String bankBookImageUrl) {
        this.bankBookImageUrl = bankBookImageUrl;
    }

    public SellerApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(SellerApplicationStatus status) {
        this.status = status;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }

    public Long getReviewedById() {
        return reviewedById;
    }

    public void setReviewedById(Long reviewedById) {
        this.reviewedById = reviewedById;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
