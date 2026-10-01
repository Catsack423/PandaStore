package project.project.Controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import project.project.ApiResponse.ApiResponse;
import project.project.DTO.product.CategoryResponse;
import project.project.DTO.product.CreateCategoryRequest;
import project.project.DTO.product.PageResponse;
import project.project.DTO.product.ProductResponse;
import project.project.Entity.user.UserRole;
import project.project.Security.CurrentUser;
import project.project.Service.api.CategoryService;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categories;
    private final CurrentUser currentUser;

    public CategoryController(CategoryService categories, CurrentUser currentUser) {
        this.categories = categories;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request) {
        if (currentUser.requireIdentity().role() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "เฉพาะผู้ดูแลระบบเท่านั้น");
        }

        var category = categories.createCategory(request.categoryName(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("เพิ่มหมวดหมู่สำเร็จ", CategoryResponse.fromEntity(category)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> result = categories.getAllCategories().stream()
                .map(CategoryResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("ดึงหมวดหมู่ทั้งหมดสำเร็จ", result));
    }

    @GetMapping("/{categoryId}/products")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = categories.getProductsByCategory(categoryId, page, size);
        return ResponseEntity.ok(ApiResponse.success("ดึงสินค้าตามหมวดหมู่สำเร็จ", PageResponse.from(result)));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long categoryId) {
        if (currentUser.requireIdentity().role() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "เฉพาะผู้ดูแลระบบเท่านั้น");
        }

        categories.deleteCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.success("ลบหมวดหมู่สำเร็จ", null));
    }
}
