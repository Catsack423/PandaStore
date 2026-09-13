package project.project.Controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.project.ApiResponse.ApiResponse;
import project.project.DTO.order.RejectOrderRequest;
import project.project.DTO.order.SubOrderResponse;
import project.project.Entity.order.Order;
import project.project.Service.api.SubOrderService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sub-orders")
public class SubOrderController {

    private final SubOrderService subOrderService;

    public SubOrderController(SubOrderService subOrderService) {
        this.subOrderService = subOrderService;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<SubOrderResponse>> getSubOrderById(@PathVariable Long orderId) {
        Order order = subOrderService.getSubOrderById(orderId);
        return ResponseEntity.ok(ApiResponse.success("ดึงข้อมูลคำสั่งซื้อย่อยสำเร็จ", SubOrderResponse.fromEntity(order)));
    }

    @GetMapping("/order-group/{orderGroupId}")
    public ResponseEntity<ApiResponse<List<SubOrderResponse>>> getSubOrdersByOrderGroup(@PathVariable Long orderGroupId) {
        List<Order> orders = subOrderService.getSubOrdersByOrderGroup(orderGroupId);
        List<SubOrderResponse> responses = orders.stream()
                .map(SubOrderResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("ดึงรายการคำสั่งซื้อย่อยในกลุ่มสำเร็จ", responses));
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<ApiResponse<List<SubOrderResponse>>> getSubOrdersBySeller(@PathVariable Long sellerId) {
        List<Order> orders = subOrderService.getSubOrdersBySeller(sellerId);
        List<SubOrderResponse> responses = orders.stream()
                .map(SubOrderResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("ดึงรายการคำสั่งซื้อย่อยของร้านค้าสำเร็จ", responses));
    }

    @PostMapping("/{orderId}/accept")
    public ResponseEntity<ApiResponse<Void>> sellerAcceptOrder(
            @PathVariable Long orderId,
            @RequestParam Long sellerId) {

        subOrderService.sellerAcceptOrder(sellerId, orderId);
        return ResponseEntity.ok(ApiResponse.success("ยืนยันรับคำสั่งซื้อสำเร็จ", null));
    }

    @PostMapping("/{orderId}/reject")
    public ResponseEntity<ApiResponse<Void>> sellerRejectOrder(
            @PathVariable Long orderId,
            @RequestParam Long sellerId,
            @Valid @RequestBody RejectOrderRequest request) {

        subOrderService.sellerRejectOrder(sellerId, orderId, request.getReason());
        return ResponseEntity.ok(ApiResponse.success("ปฏิเสธคำสั่งซื้อสำเร็จ", null));
    }

    @PostMapping("/{orderId}/confirm-delivered")
    public ResponseEntity<ApiResponse<Void>> confirmOrderDelivered(
            @PathVariable Long orderId,
            @RequestParam Long customerId) {

        subOrderService.confirmOrderDelivered(customerId, orderId);
        return ResponseEntity.ok(ApiResponse.success("ยืนยันการรับสินค้าสำเร็จ", null));
    }
}
