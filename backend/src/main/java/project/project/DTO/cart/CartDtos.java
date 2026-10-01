package project.project.DTO.cart;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import project.project.Entity.order.Cart;
import project.project.Entity.order.CartItem;

public final class CartDtos {
    private CartDtos() {}

    public record AddItem(@NotNull @Positive Long productId, @NotNull @Positive Integer quantity) {}
    public record UpdateQuantity(@NotNull @Positive Integer quantity) {}
    public record UpdateSelection(@NotNull Boolean selected) {}
    public record Item(Long cartItemId, Long productId, String productName, Long sellerId,
            BigDecimal unitPrice, Integer quantity, Boolean selected, String sellerShopName, List<String> imageUrls, Integer stock) {
        public static Item from(CartItem item) {
            var product = item.getProduct();
            return new Item(item.getCartItemId(), product.getProductId(), product.getName(),
                    product.getSeller().getSellerId(), product.getPrice(), item.getQuantity(), item.getIsSelected(),
                    product.getSeller().getShopName(), product.getImages().stream().map(image -> image.getImageUrl()).toList(), product.getStock());
        }
    }
    public record CartResponse(Long cartId, List<Item> items) {
        public static CartResponse from(Cart cart) {
            return new CartResponse(cart.getCartId(), cart.getItems().stream().map(Item::from).toList());
        }
    }
    public record StockSyncResponse(Long cartId, List<Item> items, List<String> removedProducts) {}
}
