package project.project.DTO.seller;

import lombok.Getter;
import lombok.Setter;
import project.project.Entity.seller.Seller;
import project.project.Entity.seller.SellerStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class SellerShopResponse {

    private Long sellerId;
    private Long userId;
    private String shopName;
    private String shopDescription;
    private String shopPhone;
    private String shopEmail;
    private String shopAddress;
    private SellerStatus status;
    private BigDecimal rating;
    private LocalDateTime createdAt;

    public SellerShopResponse() {
    }

    public SellerShopResponse(Long sellerId, Long userId, String shopName, String shopDescription,
                              String shopPhone, String shopEmail, String shopAddress,
                              SellerStatus status, BigDecimal rating, LocalDateTime createdAt) {
        this.sellerId = sellerId;
        this.userId = userId;
        this.shopName = shopName;
        this.shopDescription = shopDescription;
        this.shopPhone = shopPhone;
        this.shopEmail = shopEmail;
        this.shopAddress = shopAddress;
        this.status = status;
        this.rating = rating;
        this.createdAt = createdAt;
    }

    public static SellerShopResponse fromEntity(Seller seller) {
        if (seller == null) {
            return null;
        }

        Long userId = (seller.getUser() != null) ? seller.getUser().getUserId() : null;

        return new SellerShopResponse(
                seller.getSellerId(),
                userId,
                seller.getShopName(),
                seller.getShopDescription(),
                seller.getShopPhone(),
                seller.getShopEmail(),
                seller.getShopAddress(),
                seller.getStatus(),
                seller.getRating(),
                seller.getCreatedAt()
        );
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
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

    public SellerStatus getStatus() {
        return status;
    }

    public void setStatus(SellerStatus status) {
        this.status = status;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
