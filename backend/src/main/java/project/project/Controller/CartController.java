package project.project.Controller;

import java.util.List;
import project.project.ApiResponse.ApiResponse;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.project.Security.CurrentUser;
import project.project.DTO.cart.CartDtos;
import project.project.Entity.order.CartItem;
import project.project.Service.api.CartService;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService carts;
    private final CurrentUser currentUser;

    public CartController(CartService carts, CurrentUser currentUser) {
        this.carts = carts;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ApiResponse<CartDtos.CartResponse> create() {
        return ApiResponse.success("Cart updated successfully", CartDtos.CartResponse.from(carts.createCart(currentUser.requireCustomerId())));
    }

    @GetMapping
    public ApiResponse<CartDtos.CartResponse> get() {
        Long customerId = currentUser.requireCustomerId();
        try {
            return ApiResponse.success("Cart updated successfully", CartDtos.CartResponse.from(carts.getCartByCustomerId(customerId)));
        } catch (java.util.NoSuchElementException missingCart) {
            return ApiResponse.success("Cart is empty", new CartDtos.CartResponse(null, List.of()));
        }
    }

    @PostMapping("/items")
    public ApiResponse<CartDtos.Item> add(@Valid @RequestBody CartDtos.AddItem request) {
        return ApiResponse.success("Cart updated successfully", CartDtos.Item.from(carts.addItemToCart(currentUser.requireCustomerId(), request.productId(), request.quantity())));
    }

    @PostMapping("/sync")
    public ApiResponse<CartDtos.StockSyncResponse> synchronizeStock() {
        return ApiResponse.success("Cart stock synchronized", carts.synchronizeStock(currentUser.requireCustomerId()));
    }

    @PatchMapping("/items/{itemId}")
    public ApiResponse<CartDtos.Item> update(@PathVariable Long itemId, @Valid @RequestBody CartDtos.UpdateQuantity request) {
        return ApiResponse.success("Cart updated successfully", CartDtos.Item.from(carts.updateItemQuantity(currentUser.requireCustomerId(), itemId, request.quantity())));
    }

    @PatchMapping("/items/{itemId}/selection")
    public ApiResponse<CartDtos.Item> updateSelection(
            @PathVariable Long itemId,
            @Valid @RequestBody CartDtos.UpdateSelection request) {
        Long customerId = currentUser.requireCustomerId();
        CartItem item = carts.updateItemSelection(customerId, itemId, request.selected());
        CartDtos.Item response = CartDtos.Item.from(item);

        return ApiResponse.success("Item selection updated successfully", response);
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable Long itemId) {
        carts.removeItemFromCart(currentUser.requireCustomerId(), itemId);
        return ResponseEntity.ok(ApiResponse.success("Item removed successfully", null));
    }

    @DeleteMapping("/items")
    public ResponseEntity<ApiResponse<Void>> clear() {
        carts.clearCart(currentUser.requireCustomerId());
        return ResponseEntity.ok(ApiResponse.success("Item removed successfully", null));
    }

    @GetMapping("/stock")
    public ApiResponse<Map<String, Boolean>> stock() {
        return ApiResponse.success("Stock checked successfully", Map.of("valid", carts.validateCartStock(currentUser.requireCustomerId())));
    }

    @GetMapping("/sellers")
    public ApiResponse<Map<Long, List<CartDtos.Item>>> split() {
        return ApiResponse.success("Items grouped by shop successfully", carts.splitCartBySeller(currentUser.requireCustomerId()).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream().map(CartDtos.Item::from).toList())));
    }
}
