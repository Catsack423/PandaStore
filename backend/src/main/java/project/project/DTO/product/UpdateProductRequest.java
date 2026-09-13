package project.project.DTO.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import project.project.Entity.product.ProductStatus;

import java.math.BigDecimal;
import java.util.Set;

public class UpdateProductRequest {

    @Size(max = 200, message = "ชื่อสินค้าต้องมีความยาวไม่เกิน 200 ตัวอักษร")
    private String name;

    private String description;

    @DecimalMin(value = "0.01", message = "ราคาสินค้าต้องมากกว่า 0")
    private BigDecimal price;

    @Min(value = 0, message = "จำนวนสต็อกต้องไม่ติดลบ")
    private Integer stock;

    private ProductStatus status;

    @Size(max = 255, message = "ข้อมูลการจัดส่งต้องมีความยาวไม่เกิน 255 ตัวอักษร")
    private String shippingInfo;

    private Set<Long> categoryIds;

    public UpdateProductRequest() {
    }

    public UpdateProductRequest(String name, String description, BigDecimal price, Integer stock,
                                ProductStatus status, String shippingInfo, Set<Long> categoryIds) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.status = status;
        this.shippingInfo = shippingInfo;
        this.categoryIds = categoryIds;
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

    public String getShippingInfo() {
        return shippingInfo;
    }

    public void setShippingInfo(String shippingInfo) {
        this.shippingInfo = shippingInfo;
    }

    public Set<Long> getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(Set<Long> categoryIds) {
        this.categoryIds = categoryIds;
    }
}
