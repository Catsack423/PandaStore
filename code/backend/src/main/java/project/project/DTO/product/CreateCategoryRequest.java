package project.project.DTO.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank(message = "Category name is required")
        @Size(max = 20, message = "Category name must not exceed 20 characters")
        String categoryName,
        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description) {
}
