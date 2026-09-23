package project.project.DTO.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CreateProductRequest {

    @NotBlank(message = "ชื่อสินค้าห้ามว่าง")
    @Size(max = 200, message = "ชื่อสินค้าต้องมีความยาวไม่เกิน 200 ตัวอักษร")
    private String name;

    private String description;

    @NotNull(message = "ราคาสินค้าห้ามว่าง")
    @DecimalMin(value = "0.01", message = "ราคาสินค้าต้องมากกว่า 0")
    private BigDecimal price;

    @NotNull(message = "จำนวนสต็อกห้ามว่าง")
    @Min(value = 0, message = "จำนวนสต็อกต้องไม่ติดลบ")
    private Integer stock;

    @Size(max = 255, message = "ข้อมูลการจัดส่งต้องมีความยาวไม่เกิน 255 ตัวอักษร")
    private String shippingInfo;

    private Set<Long> categoryIds;

    private List<String> imageUrls = new ArrayList<>();

    public CreateProductRequest() {
    }

    public CreateProductRequest(String name, String description, BigDecimal price, Integer stock,
                                String shippingInfo, Set<Long> categoryIds, List<String> imageUrls) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.shippingInfo = shippingInfo;
        this.categoryIds = categoryIds;
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
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

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
    }
}
