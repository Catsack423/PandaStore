package project.project.DTO.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank(message = "ชื่อหมวดหมู่ห้ามว่าง")
        @Size(max = 100, message = "ชื่อหมวดหมู่ต้องไม่เกิน 100 ตัวอักษร")
        String categoryName,
        @Size(max = 255, message = "คำอธิบายต้องไม่เกิน 255 ตัวอักษร")
        String description) {
}
