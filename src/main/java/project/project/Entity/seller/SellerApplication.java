package project.project.Entity.seller;

import jakarta.persistence.*;
import project.project.Entity.user.User;
import java.time.LocalDateTime;

@Entity
@Table(name = "seller_applications")
public class SellerApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id")
    private Long applicationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "shop_name", nullable = false, length = 100)
    private String shopName;

    @Column(name = "shop_description", columnDefinition = "TEXT", nullable = false)
    private String shopDescription;

    @Column(name = "shop_phone", nullable = false, length = 20)
    private String shopPhone;

    @Column(name = "shop_email", nullable = false, length = 100)
    private String shopEmail;

    @Column(name = "shop_address", nullable = false, length = 255)
    private String shopAddress;

    @Column(name = "seller_first_name", nullable = false, length = 100)
    private String sellerFirstName;

    @Column(name = "seller_last_name", nullable = false, length = 100)
    private String sellerLastName;

    @Column(name = "id_card_number", nullable = false, length = 13)
    private String idCardNumber;

    @Column(name = "id_card_image_url", nullable = false, length = 255)
    private String idCardImageUrl;

    @Column(name = "bank_account_name", nullable = false, length = 100)
    private String bankAccountName;

    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    @Column(name = "bank_account_number", nullable = false, length = 30)
    private String bankAccountNumber;

    @Column(name = "bank_book_image_url", nullable = false, length = 255)
    private String bankBookImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SellerApplicationStatus status = SellerApplicationStatus.PENDING;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public SellerApplication() {
    }

    public SellerApplication(Long applicationId, User user, String shopName, String shopDescription, String shopPhone, String shopEmail, String shopAddress, String sellerFirstName, String sellerLastName, String idCardNumber, String idCardImageUrl, String bankAccountName, String bankName, String bankAccountNumber, String bankBookImageUrl, SellerApplicationStatus status, String adminNote, User reviewedBy, LocalDateTime reviewedAt, LocalDateTime createdAt) {
        this.applicationId = applicationId;
        this.user = user;
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
        this.status = status != null ? status : SellerApplicationStatus.PENDING;
        this.adminNote = adminNote;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = SellerApplicationStatus.PENDING;
        }
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public User getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(User reviewedBy) {
        this.reviewedBy = reviewedBy;
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
