package project.project.DTO.product;

import project.project.Entity.product.Category;
import project.project.Entity.product.Product;
import project.project.Entity.product.ProductImage;
import project.project.Entity.product.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProductResponse {

    private Long productId;
    private Long sellerId;
    private String sellerShopName;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private ProductStatus status;
    private BigDecimal averageRating;
    private Integer reviewCount;
    private String shippingInfo;
    private List<String> imageUrls = new ArrayList<>();
    private List<Long> categoryIds = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProductResponse() {
    }

    public ProductResponse(Long productId, Long sellerId, String sellerShopName, String name,
                           String description, BigDecimal price, Integer stock, ProductStatus status,
                           BigDecimal averageRating, Integer reviewCount, String shippingInfo,
                           List<String> imageUrls, List<Long> categoryIds,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.productId = productId;
        this.sellerId = sellerId;
        this.sellerShopName = sellerShopName;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.status = status;
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
        this.shippingInfo = shippingInfo;
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
        this.categoryIds = categoryIds != null ? categoryIds : new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ProductResponse fromEntity(Product product) {
        if (product == null) {
            return null;
        }

        List<String> images = new ArrayList<>();
        if (product.getImages() != null) {
            for (ProductImage img : product.getImages()) {
                images.add(img.getImageUrl());
            }
        }

        List<Long> categories = new ArrayList<>();
        if (product.getCategories() != null) {
            for (Category cat : product.getCategories()) {
                categories.add(cat.getCategoryId());
            }
        }

        Long sellerId = product.getSeller() != null ? product.getSeller().getSellerId() : null;
        String shopName = product.getSeller() != null ? product.getSeller().getShopName() : null;

        return new ProductResponse(
                product.getProductId(),
                sellerId,
                shopName,
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getStatus(),
                product.getAverageRating(),
                product.getReviewCount(),
                product.getShippingInfo(),
                images,
                categories,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerShopName() {
        return sellerShopName;
    }

    public void setSellerShopName(String sellerShopName) {
        this.sellerShopName = sellerShopName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public BigDecimal getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(BigDecimal averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public String getShippingInfo() {
        return shippingInfo;
    }

    public void setShippingInfo(String shippingInfo) {
        this.shippingInfo = shippingInfo;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public List<Long> getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(List<Long> categoryIds) {
        this.categoryIds = categoryIds;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
