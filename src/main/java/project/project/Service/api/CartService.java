package project.project.Service.api;

import project.project.Entity.order.Cart;
import project.project.Entity.order.CartItem;
import java.util.List;
import java.util.Map;

public interface CartService {
    Cart getCartByCustomerId(Long customerId);
    CartItem addItemToCart(Long customerId, Long productId, Integer quantity);
    CartItem updateItemQuantity(Long customerId, Long cartItemId, Integer quantity);
    void removeItemFromCart(Long customerId, Long cartItemId);
    void clearCart(Long customerId);
    boolean validateCartStock(Long customerId);
    Map<Long, List<CartItem>> splitCartBySeller(Long customerId);
}
