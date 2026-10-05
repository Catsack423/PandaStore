package project.project.Controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.project.ApiResponse.ApiResponse;
import project.project.DTO.product.CreateProductRequest;
import project.project.DTO.product.ProductResponse;
import project.project.DTO.product.CatalogSummary;
import project.project.DTO.product.PageResponse;
import project.project.DTO.product.UpdateProductRequest;
import project.project.Entity.product.Product;
import project.project.Service.api.ProductService;
import project.project.Security.CurrentUser;
import project.project.Repository.SellerRepository;
import project.project.Entity.user.UserRole;
import project.project.Entity.seller.SellerStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final CurrentUser currentUser;
    private final SellerRepository sellers;

    public ProductController(ProductService productService, CurrentUser currentUser, SellerRepository sellers) {
        this.productService = productService;
        this.currentUser = currentUser;
        this.sellers = sellers;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @RequestParam Long sellerId,
            @Valid @RequestBody CreateProductRequest request) {
        var identity = currentUser.requireIdentity();
        if (identity.role() != UserRole.SELLER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seller account required");
        var shop = sellers.findByUser_UserId(identity.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Active shop required"));
        if (shop.getStatus() != SellerStatus.ACTIVE || !shop.getSellerId().equals(sellerId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only add products to your active shop");
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setShippingInfo(request.getShippingInfo());

        Product created = productService.createProduct(sellerId, product, request.getImageUrls(), request.getCategoryIds());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", ProductResponse.fromEntity(created)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("Product retrieved successfully", ProductResponse.fromEntity(product)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllActiveProducts() {
        List<Product> products = productService.getAllActiveProducts();
        List<ProductResponse> responses = products.stream()
                .map(ProductResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Available products retrieved successfully", responses));
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProductsBySeller(@PathVariable Long sellerId) {
        List<Product> products = productService.getProductsBySeller(sellerId);
        List<ProductResponse> responses = products.stream()
                .map(ProductResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Shop products retrieved successfully", responses));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) List<Long> categoryIds,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var selected = categoryIds != null && !categoryIds.isEmpty()
                ? categoryIds : categoryId == null ? List.<Long>of() : List.of(categoryId);
        var result = productService.searchProductsPage(keyword, selected, minPrice, maxPrice, sort, page, size);
        return ResponseEntity.ok(ApiResponse.success("Product search completed successfully", PageResponse.from(result)));
    }

    @GetMapping("/catalog-summary")
    public ResponseEntity<ApiResponse<CatalogSummary>> getCatalogSummary() {
        return ResponseEntity.ok(ApiResponse.success("Product catalog summary retrieved successfully", productService.getCatalogSummary()));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = productService.searchProductsPage(null, categoryId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Products retrieved by category successfully", PageResponse.from(result)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @RequestParam(required = false) Long sellerId,
            @Valid @RequestBody UpdateProductRequest request) {
        Long effectiveSellerId = sellerId;
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && (authentication.getPrincipal() instanceof project.project.Security.AuthenticatedUser identity)) {
            if (identity.role() == UserRole.SELLER) {
                var shop = sellers.findByUser_UserId(identity.userId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Shop required"));
                if (effectiveSellerId == null) {
                    effectiveSellerId = shop.getSellerId();
                } else if (!shop.getSellerId().equals(effectiveSellerId)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot update product for another seller");
                }
            }
        }
        if (effectiveSellerId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seller ID is required");
        }

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setStatus(request.getStatus());
        product.setShippingInfo(request.getShippingInfo());
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            java.util.Set<project.project.Entity.product.Category> categories = new java.util.HashSet<>();
            for (Long catId : request.getCategoryIds()) {
                if (catId != null) {
                    project.project.Entity.product.Category cat = new project.project.Entity.product.Category();
                    cat.setCategoryId(catId);
                    categories.add(cat);
                }
            }
            product.setCategories(categories);
        }

        Product updated = productService.updateProduct(effectiveSellerId, id, product);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", ProductResponse.fromEntity(updated)));
    }

    @PostMapping("/{id}/deduct-stock")
    public ResponseEntity<ApiResponse<Void>> deductStock(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        productService.validateAndDeductStock(id, quantity);
        return ResponseEntity.ok(ApiResponse.success("Stock deducted successfully", null));
    }

    @PostMapping("/{id}/restore-stock")
    public ResponseEntity<ApiResponse<Void>> restoreStock(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        productService.restoreStock(id, quantity);
        return ResponseEntity.ok(ApiResponse.success("Stock restored successfully", null));
    }
}
