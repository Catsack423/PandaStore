package project.project.Controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.project.ApiResponse.ApiResponse;
import project.project.DTO.product.CreateProductRequest;
import project.project.DTO.product.ProductResponse;
import project.project.DTO.product.UpdateProductRequest;
import project.project.Entity.product.Product;
import project.project.Service.api.ProductService;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @RequestParam Long sellerId,
            @Valid @RequestBody CreateProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setShippingInfo(request.getShippingInfo());

        Product created = productService.createProduct(sellerId, product, request.getImageUrls());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("สร้างสินค้าสำเร็จ", ProductResponse.fromEntity(created)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("ดึงข้อมูลสินค้าสำเร็จ", ProductResponse.fromEntity(product)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllActiveProducts() {
        List<Product> products = productService.getAllActiveProducts();
        List<ProductResponse> responses = products.stream()
                .map(ProductResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("ดึงรายการสินค้าที่วางจำหน่ายทั้งหมดสำเร็จ", responses));
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProductsBySeller(@PathVariable Long sellerId) {
        List<Product> products = productService.getProductsBySeller(sellerId);
        List<ProductResponse> responses = products.stream()
                .map(ProductResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("ดึงรายการสินค้าของร้านค้าสำเร็จ", responses));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        List<Product> products = productService.searchProducts(keyword, categoryId);
        List<ProductResponse> responses = products.stream()
                .map(ProductResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("ค้นหาสินค้าสำเร็จ", responses));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @RequestParam Long sellerId,
            @Valid @RequestBody UpdateProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setStatus(request.getStatus());
        product.setShippingInfo(request.getShippingInfo());

        Product updated = productService.updateProduct(sellerId, id, product);
        return ResponseEntity.ok(ApiResponse.success("แก้ไขข้อมูลสินค้าสำเร็จ", ProductResponse.fromEntity(updated)));
    }

    @PostMapping("/{id}/deduct-stock")
    public ResponseEntity<ApiResponse<Void>> deductStock(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        productService.validateAndDeductStock(id, quantity);
        return ResponseEntity.ok(ApiResponse.success("ตัดสต็อกสินค้าสำเร็จ", null));
    }

    @PostMapping("/{id}/restore-stock")
    public ResponseEntity<ApiResponse<Void>> restoreStock(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        productService.restoreStock(id, quantity);
        return ResponseEntity.ok(ApiResponse.success("คืนสต็อกสินค้าสำเร็จ", null));
    }
}
