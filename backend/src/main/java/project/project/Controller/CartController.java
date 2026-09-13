package project.project.Controller;

import java.util.List;
import project.project.ApiResponse.ApiResponse;
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
    public ApiResponse<CartDtos.CartResponse> create(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.success("ดำเนินการตะกร้าสำเร็จ", CartDtos.CartResponse.from(carts.createCart(currentUser.requireCustomerId(authorization))));
    }

    @GetMapping
    public ApiResponse<CartDtos.CartResponse> get(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.success("ดำเนินการตะกร้าสำเร็จ", CartDtos.CartResponse.from(carts.getCartByCustomerId(currentUser.requireCustomerId(authorization))));
    }

    @PostMapping("/items")
    public ApiResponse<CartDtos.Item> add(@RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CartDtos.AddItem request) {
        return ApiResponse.success("ดำเนินการตะกร้าสำเร็จ", CartDtos.Item.from(carts.addItemToCart(currentUser.requireCustomerId(authorization), request.productId(), request.quantity())));
    }

    @PatchMapping("/items/{itemId}")
    public ApiResponse<CartDtos.Item> update(@RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long itemId, @Valid @RequestBody CartDtos.UpdateQuantity request) {
        return ApiResponse.success("ดำเนินการตะกร้าสำเร็จ", CartDtos.Item.from(carts.updateItemQuantity(currentUser.requireCustomerId(authorization), itemId, request.quantity())));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> remove(@RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long itemId) {
        carts.removeItemFromCart(currentUser.requireCustomerId(authorization), itemId);
        return ResponseEntity.ok(ApiResponse.success("ลบรายการสินค้าสำเร็จ", null));
    }

    @DeleteMapping("/items")
    public ResponseEntity<ApiResponse<Void>> clear(@RequestHeader(value = "Authorization", required = false) String authorization) {
        carts.clearCart(currentUser.requireCustomerId(authorization));
        return ResponseEntity.ok(ApiResponse.success("ลบรายการสินค้าสำเร็จ", null));
    }

    @GetMapping("/stock")
    public ApiResponse<Map<String, Boolean>> stock(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.success("ตรวจสอบสต็อกสำเร็จ", Map.of("valid", carts.validateCartStock(currentUser.requireCustomerId(authorization))));
    }

    @GetMapping("/sellers")
    public ApiResponse<Map<Long, List<CartDtos.Item>>> split(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.success("แยกสินค้าตามร้านสำเร็จ", carts.splitCartBySeller(currentUser.requireCustomerId(authorization)).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream().map(CartDtos.Item::from).toList())));
    }
}
