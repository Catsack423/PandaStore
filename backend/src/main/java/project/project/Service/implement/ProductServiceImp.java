package project.project.Service.implement;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.project.Entity.product.Category;
import project.project.Entity.product.Product;
import project.project.Entity.product.ProductImage;
import project.project.Entity.product.ProductStatus;
import project.project.Entity.seller.Seller;
import project.project.Exception.InsufficientStockException;
import project.project.Exception.ResourceNotFoundException;
import project.project.Repository.CategoryRepository;
import project.project.Repository.ProductImageRepository;
import project.project.Repository.ProductRepository;
import project.project.Repository.SellerRepository;
import project.project.Service.api.ProductService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProductServiceImp implements ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final SellerRepository sellerRepository;
    private final CategoryRepository categoryRepository;

    public ProductServiceImp(ProductRepository productRepository,
                             ProductImageRepository productImageRepository,
                             SellerRepository sellerRepository,
                             CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.sellerRepository = sellerRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public Product createProduct(Long sellerId, Product product, List<String> imageUrls) {
        if (sellerId == null) {
            throw new IllegalArgumentException("Seller ID cannot be null");
        }
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found with id: " + sellerId));

        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Product price must be greater than 0");
        }
        if (product.getStock() == null || product.getStock() < 0) {
            throw new IllegalArgumentException("Product stock cannot be negative");
        }
        if (imageUrls == null || imageUrls.isEmpty()) {
            throw new IllegalArgumentException("At least one product image is required");
        }

        product.setSeller(seller);
        if (product.getStatus() == null) {
            product.setStatus(ProductStatus.ACTIVE);
        }
        if (product.getAverageRating() == null) {
            product.setAverageRating(BigDecimal.ZERO);
        }
        if (product.getReviewCount() == null) {
            product.setReviewCount(0);
        }

        Product savedProduct = productRepository.save(product);

        List<ProductImage> images = new ArrayList<>();
        for (int i = 0; i < imageUrls.size(); i++) {
            ProductImage img = new ProductImage();
            img.setProduct(savedProduct);
            img.setImageUrl(imageUrls.get(i));
            img.setIsPrimary(i == 0);
            img.setDisplayOrder(i);
            images.add(productImageRepository.save(img));
        }
        savedProduct.setImages(images);

        return savedProduct;
    }

    @Override
    @Transactional
    public Product updateProduct(Long sellerId, Long productId, Product updatedProduct) {
        if (sellerId == null || productId == null || updatedProduct == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }
        Product existing = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (existing.getSeller() == null || !existing.getSeller().getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("Seller does not own this product");
        }

        if (updatedProduct.getName() != null && !updatedProduct.getName().trim().isEmpty()) {
            existing.setName(updatedProduct.getName());
        }
        if (updatedProduct.getDescription() != null) {
            existing.setDescription(updatedProduct.getDescription());
        }
        if (updatedProduct.getPrice() != null) {
            if (updatedProduct.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Product price must be greater than 0");
            }
            existing.setPrice(updatedProduct.getPrice());
        }
        if (updatedProduct.getStock() != null) {
            if (updatedProduct.getStock() < 0) {
                throw new IllegalArgumentException("Product stock cannot be negative");
            }
            existing.setStock(updatedProduct.getStock());
            if (updatedProduct.getStock() == 0 && existing.getStatus() == ProductStatus.ACTIVE) {
                existing.setStatus(ProductStatus.OUT_OF_STOCK);
            }
        }
        if (updatedProduct.getStatus() != null) {
            existing.setStatus(updatedProduct.getStatus());
        }
        if (updatedProduct.getShippingInfo() != null) {
            existing.setShippingInfo(updatedProduct.getShippingInfo());
        }
        if (updatedProduct.getCategories() != null && !updatedProduct.getCategories().isEmpty()) {
            existing.setCategories(updatedProduct.getCategories());
        }

        return productRepository.save(existing);
    }

    @Override
    public Product getProductById(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
    }

    @Override
    public List<Product> getAllActiveProducts() {
        return productRepository.findByStatus(ProductStatus.ACTIVE);
    }

    @Override
    public List<Product> getProductsBySeller(Long sellerId) {
        if (sellerId == null) {
            throw new IllegalArgumentException("Seller ID cannot be null");
        }
        return productRepository.findBySeller_SellerId(sellerId);
    }

    @Override
    public List<Product> searchProducts(String keyword, Long categoryId) {
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        return productRepository.searchProducts(cleanKeyword, categoryId, ProductStatus.ACTIVE);
    }

    @Override
    @Transactional
    public void validateAndDeductStock(Long productId, Integer quantity) {
        if (productId == null || quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Valid product ID and positive quantity are required");
        }
        Product product = getProductById(productId);

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new IllegalStateException("Product is not active for sale: " + productId);
        }
        if (product.getStock() < quantity) {
            throw new InsufficientStockException("Insufficient stock for product id " + productId +
                    ". Available: " + product.getStock() + ", Requested: " + quantity);
        }

        int remaining = product.getStock() - quantity;
        product.setStock(remaining);
        if (remaining == 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        }
        productRepository.save(product);
    }

    @Override
    @Transactional
    public void restoreStock(Long productId, Integer quantity) {
        if (productId == null || quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Valid product ID and positive quantity are required");
        }
        Product product = getProductById(productId);

        int newStock = product.getStock() + quantity;
        product.setStock(newStock);
        if (product.getStatus() == ProductStatus.OUT_OF_STOCK && newStock > 0) {
            product.setStatus(ProductStatus.ACTIVE);
        }
        productRepository.save(product);
    }

    @Override
    @Transactional
    public void updateAverageRating(Long productId, Integer newRating) {
        if (productId == null || newRating == null) {
            throw new IllegalArgumentException("Product ID and new rating cannot be null");
        }
        if (newRating < 1 || newRating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        Product product = getProductById(productId);

        int currentCount = product.getReviewCount() != null ? product.getReviewCount() : 0;
        BigDecimal currentAvg = product.getAverageRating() != null ? product.getAverageRating() : BigDecimal.ZERO;

        BigDecimal totalScore = currentAvg.multiply(BigDecimal.valueOf(currentCount)).add(BigDecimal.valueOf(newRating));
        int newCount = currentCount + 1;
        BigDecimal newAvg = totalScore.divide(BigDecimal.valueOf(newCount), 2, RoundingMode.HALF_UP);

        product.setReviewCount(newCount);
        product.setAverageRating(newAvg);
        productRepository.save(product);
    }
}
