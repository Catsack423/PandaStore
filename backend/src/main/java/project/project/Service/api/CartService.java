package project.project.Service.api;

import project.project.Entity.order.Cart;
import project.project.Entity.order.CartItem;
import java.util.List;
import java.util.Map;

public interface CartService {
   
    // Returns the existing cart if this customer already has one.
    Cart createCart(Long customerId);
    Cart getCartByCustomerId(Long customerId);
    CartItem addItemToCart(Long customerId, Long productId, Integer quantity);
    // Sets an absolute positive quantity; use removeItemFromCart to delete.
    CartItem updateItemQuantity(Long customerId, Long cartItemId, Integer quantity);
    void removeItemFromCart(Long customerId, Long cartItemId);
    void clearCart(Long customerId);
    // Checks selected items only; false when none are selected. Does not reserve stock.
    boolean validateCartStock(Long customerId);
    // Groups selected items only; rejects empty selections and unavailable items.
    Map<Long, List<CartItem>> splitCartBySeller(Long customerId);
}
