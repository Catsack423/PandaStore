package project.project.Controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.project.Controller.support.CurrentUser;
import project.project.DTO.cart.CartDtos;
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
    public CartDtos.CartResponse create(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return CartDtos.CartResponse.from(carts.createCart(currentUser.requireCustomerId(authorization)));
    }

    @GetMapping
    public CartDtos.CartResponse get(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return CartDtos.CartResponse.from(carts.getCartByCustomerId(currentUser.requireCustomerId(authorization)));
    }

    @PostMapping("/items")
    public CartDtos.Item add(@RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CartDtos.AddItem request) {
        return CartDtos.Item.from(carts.addItemToCart(currentUser.requireCustomerId(authorization), request.productId(), request.quantity()));
    }

    @PatchMapping("/items/{itemId}")
    public CartDtos.Item update(@RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long itemId, @Valid @RequestBody CartDtos.UpdateQuantity request) {
        return CartDtos.Item.from(carts.updateItemQuantity(currentUser.requireCustomerId(authorization), itemId, request.quantity()));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> remove(@RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long itemId) {
        carts.removeItemFromCart(currentUser.requireCustomerId(authorization), itemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/items")
    public ResponseEntity<Void> clear(@RequestHeader(value = "Authorization", required = false) String authorization) {
        carts.clearCart(currentUser.requireCustomerId(authorization));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stock")
    public Map<String, Boolean> stock(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return Map.of("valid", carts.validateCartStock(currentUser.requireCustomerId(authorization)));
    }

    @GetMapping("/sellers")
    public Map<Long, List<CartDtos.Item>> split(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return carts.splitCartBySeller(currentUser.requireCustomerId(authorization)).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream().map(CartDtos.Item::from).toList()));
    }
}
