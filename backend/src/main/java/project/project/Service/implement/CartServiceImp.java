package project.project.Service.implement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;
import project.project.Entity.order.Cart;
import project.project.Entity.order.CartItem;
import project.project.Entity.product.Product;
import project.project.Entity.product.ProductStatus;
import project.project.Entity.seller.SellerStatus;
import project.project.Entity.user.Customer;
import project.project.Entity.user.UserStatus;
import project.project.Repository.CartRepository;
import project.project.Repository.CartItemRepository;
import project.project.Repository.CustomerRepository;
import project.project.Service.api.ProductService;
import project.project.Service.api.CartService;

@Service
@Transactional
public class CartServiceImp implements CartService {
    private final CartRepository carts;
    private final CartItemRepository items;
    private final CustomerRepository customers;
    private final ProductService products;

    public CartServiceImp(CartRepository carts, CartItemRepository items,
            CustomerRepository customers, @Lazy ProductService products) {
        this.carts = carts;
        this.items = items;
        this.customers = customers;
        this.products = products;
    }

    @Override
    public Cart createCart(Long customerId) {
        Customer customer = lockCustomer(customerId);
        return carts.findByCustomer_CustomerId(customerId)
                .orElseGet(() -> carts.save(new Cart(null, customer)));
    }

    @Override
    @Transactional(readOnly = true)
    public Cart getCartByCustomerId(Long customerId) {
        requireId(customerId, "Customer");
        return carts.findByCustomer_CustomerId(customerId)
                .orElseThrow(() -> new NoSuchElementException("Cart not found for customer " + customerId));
    }

    @Override
    public CartItem addItemToCart(Long customerId, Long productId, Integer quantity) {
        requireId(productId, "Product");
        requireQuantity(quantity);
        lockCustomer(customerId);
        Cart cart = getCartByCustomerId(customerId);
        Product product = products.getProductById(productId);
        if (product == null) throw new NoSuchElementException("Product not found");
        CartItem existing = cart.getItems().stream()
                .filter(item -> item.getProduct().getProductId().equals(productId))
                .findFirst().orElse(null);
        long total = (long) quantity + (existing == null ? 0 : existing.getQuantity());
        if (total > Integer.MAX_VALUE || !isAvailable(product, total)) {
            throw new IllegalArgumentException("Product is unavailable or stock is insufficient");
        }
        if (existing != null) {
            existing.setQuantity((int) total);
            return items.save(existing);
        }
        CartItem item = new CartItem(null, cart, product, quantity, true);
        cart.getItems().add(item);
        return items.save(item);
    }

    @Override
    public CartItem updateItemQuantity(Long customerId, Long cartItemId, Integer quantity) {
        requireQuantity(quantity);
        lockCustomer(customerId);
        CartItem item = findOwnedItem(getCartByCustomerId(customerId), cartItemId);
        if (!isAvailable(item.getProduct(), quantity)) {
            throw new IllegalArgumentException("Product is unavailable or stock is insufficient");
        }
        item.setQuantity(quantity);
        return items.save(item);
    }

    @Override
    public void removeItemFromCart(Long customerId, Long cartItemId) {
        lockCustomer(customerId);
        Cart cart = getCartByCustomerId(customerId);
        CartItem item = findOwnedItem(cart, cartItemId);
        // The managed collection's orphanRemoval deletes the corresponding row.
        cart.getItems().remove(item);
    }

    @Override
    public void clearCart(Long customerId) {
        lockCustomer(customerId);
        getCartByCustomerId(customerId).getItems().clear();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateCartStock(Long customerId) {
        List<CartItem> selected = selectedItems(getCartByCustomerId(customerId));
        return !selected.isEmpty() && selected.stream()
                .allMatch(item -> isAvailable(item.getProduct(), item.getQuantity()));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<CartItem>> splitCartBySeller(Long customerId) {
        List<CartItem> selected = selectedItems(getCartByCustomerId(customerId));
        if (selected.isEmpty()) throw new IllegalArgumentException("No cart items are selected");
        Map<Long, List<CartItem>> groups = new LinkedHashMap<>();
        for (CartItem item : selected) {
            if (!isAvailable(item.getProduct(), item.getQuantity())) {
                throw new IllegalArgumentException("Product is unavailable or stock is insufficient");
            }
            groups.computeIfAbsent(item.getProduct().getSeller().getSellerId(), key -> new ArrayList<>())
                    .add(item);
        }
        return groups;
    }

    private Customer lockCustomer(Long customerId) {
        requireId(customerId, "Customer");
        // Serialize cart writes, including the first creation when no cart exists yet.
        return customers.findByIdForUpdate(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found"));
    }

    private CartItem findOwnedItem(Cart cart, Long cartItemId) {
        requireId(cartItemId, "Cart item");
        return cart.getItems().stream().filter(item -> item.getCartItemId().equals(cartItemId))
                .findFirst().orElseThrow(() -> new NoSuchElementException("Item not found in this cart"));
    }

    private List<CartItem> selectedItems(Cart cart) {
        return cart.getItems().stream().filter(item -> Boolean.TRUE.equals(item.getIsSelected())).toList();
    }

    private boolean isAvailable(Product product, long quantity) {
        return quantity > 0 && product.getStatus() == ProductStatus.ACTIVE
                && product.getStock() != null && quantity <= product.getStock()
                && product.getSeller().getStatus() == SellerStatus.ACTIVE
                && product.getSeller().getUser().getStatus() == UserStatus.ACTIVE;
    }

    private void requireId(Long id, String name) {
        if (id == null || id <= 0) throw new IllegalArgumentException(name + " ID must be positive");
    }

    private void requireQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive; use removeItemFromCart to delete an item");
        }
    }
}
