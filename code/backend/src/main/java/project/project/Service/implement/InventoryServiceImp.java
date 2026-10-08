package project.project.Service.implement;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import project.project.Entity.product.Product;
import project.project.Entity.product.ProductStatus;
import project.project.Entity.seller.SellerStatus;
import project.project.Repository.ProductRepository;
import project.project.Service.api.InventoryService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class InventoryServiceImp implements InventoryService {

    private final ProductRepository productRepository;

    public InventoryServiceImp(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Map<Long, Product> reserve(Map<Long, Integer> quantities) {
        Map<Long, Product> reservedProducts = new LinkedHashMap<>();

        // Lock ตามลำดับ ID เดียวกัน เพื่อลดโอกาสเกิด deadlock
        for (var entry : new TreeMap<>(quantities).entrySet()) {
            int quantity = requireQuantity(entry.getValue());
            Product product = lockProduct(entry.getKey());

            if (product.getStatus() != ProductStatus.ACTIVE
                    || product.getSeller().getStatus() != SellerStatus.ACTIVE) {
                throw new IllegalStateException(
                        "Product is unavailable: " + product.getProductId());
            }

            if (product.getStock() == null || product.getStock() < quantity) {
                throw new IllegalStateException(
                        "Insufficient stock: " + product.getProductId());
            }

            product.setStock(product.getStock() - quantity);
            reservedProducts.put(product.getProductId(), product);
        }

        return reservedProducts;
    }

    @Override
    public void release(Map<Long, Integer> quantities) {
        for (var entry : new TreeMap<>(quantities).entrySet()) {
            int quantity = requireQuantity(entry.getValue());
            Product product = lockProduct(entry.getKey());

            if (product.getStock() == null) {
                throw new IllegalStateException(
                        "Invalid stock: " + product.getProductId());
            }

            product.setStock(Math.addExact(product.getStock(), quantity));
        }
    }

    private Product lockProduct(Long productId) {
        Assert.notNull(productId, "Product ID is required");

        return productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Product not found: " + productId));
    }

    private int requireQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        return quantity;
    }
}