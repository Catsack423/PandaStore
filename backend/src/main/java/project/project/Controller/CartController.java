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
        return ApiResponse.success("ดำเนินการตะกร้าสำเร็จ", CartDtos.CartResponse.from(carts.createCart(currentUser.requireCustomerId())));
    }

    @GetMapping
    public ApiResponse<CartDtos.CartResponse> get() {
        return ApiResponse.success("ดำเนินการตะกร้าสำเร็จ", CartDtos.CartResponse.from(carts.getCartByCustomerId(currentUser.requireCustomerId())));
    }

    @PostMapping("/items")
    public ApiResponse<CartDtos.Item> add(@Valid @RequestBody CartDtos.AddItem request) {
        return ApiResponse.success("ดำเนินการตะกร้าสำเร็จ", CartDtos.Item.from(carts.addItemToCart(currentUser.requireCustomerId(), request.productId(), request.quantity())));
    }

    @PatchMapping("/items/{itemId}")
    public ApiResponse<CartDtos.Item> update(@PathVariable Long itemId, @Valid @RequestBody CartDtos.UpdateQuantity request) {
        return ApiResponse.success("ดำเนินการตะกร้าสำเร็จ", CartDtos.Item.from(carts.updateItemQuantity(currentUser.requireCustomerId(), itemId, request.quantity())));
    }

    @PatchMapping("/items/{itemId}/selection")
    public ApiResponse<CartDtos.Item> updateSelection(
            @PathVariable Long itemId,
            @Valid @RequestBody CartDtos.UpdateSelection request) {
        Long customerId = currentUser.requireCustomerId();
        CartItem item = carts.updateItemSelection(customerId, itemId, request.selected());
        CartDtos.Item response = CartDtos.Item.from(item);

        return ApiResponse.success("เปลี่ยนการเลือกสินค้าสำเร็จ", response);
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable Long itemId) {
        carts.removeItemFromCart(currentUser.requireCustomerId(), itemId);
        return ResponseEntity.ok(ApiResponse.success("ลบรายการสินค้าสำเร็จ", null));
    }

    @DeleteMapping("/items")
    public ResponseEntity<ApiResponse<Void>> clear() {
        carts.clearCart(currentUser.requireCustomerId());
        return ResponseEntity.ok(ApiResponse.success("ลบรายการสินค้าสำเร็จ", null));
    }

    @GetMapping("/stock")
    public ApiResponse<Map<String, Boolean>> stock() {
        return ApiResponse.success("ตรวจสอบสต็อกสำเร็จ", Map.of("valid", carts.validateCartStock(currentUser.requireCustomerId())));
    }

    @GetMapping("/sellers")
    public ApiResponse<Map<Long, List<CartDtos.Item>>> split() {
        return ApiResponse.success("แยกสินค้าตามร้านสำเร็จ", carts.splitCartBySeller(currentUser.requireCustomerId()).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream().map(CartDtos.Item::from).toList())));
    }
}
