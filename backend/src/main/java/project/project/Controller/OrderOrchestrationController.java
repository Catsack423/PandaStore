package project.project.Controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.project.ApiResponse.ApiResponse;
import project.project.DTO.order.CreateOrderGroupRequest;
import project.project.DTO.order.OrderGroupResponse;
import project.project.Service.api.OrderOrchestrationService;
import project.project.Service.api.ResourceOwnershipService;

import java.security.Principal;

@RestController
@RequestMapping("/api/order-groups")
public class OrderOrchestrationController {

    private final OrderOrchestrationService orderOrchestrationService;
    private final RequestUserResolver requestUserResolver;
    private final ResourceOwnershipService ownershipService;

    public OrderOrchestrationController(
            OrderOrchestrationService orderOrchestrationService,
            RequestUserResolver requestUserResolver,
            ResourceOwnershipService ownershipService) {
        this.orderOrchestrationService = orderOrchestrationService;
        this.requestUserResolver = requestUserResolver;
        this.ownershipService = ownershipService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createOrderGroup(
            @Valid @RequestBody CreateOrderGroupRequest request,
            Principal principal) {
        Long userId = requestUserResolver.requireUserId(principal);

        ownershipService.requireCustomerOwner(userId, request.customerId());

        var group = orderOrchestrationService.createOrderGroupFromCart(
                request.customerId(),
                request.shippingAddressId(),
                request.sellerShippingMethods(),
                request.paymentMethod());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "สร้างคำสั่งซื้อสำเร็จ",
                                group.getOrderGroupId()));
    }

    @GetMapping("/{orderGroupId}")
    public ResponseEntity<ApiResponse<OrderGroupResponse>> getOrderGroup(
            @PathVariable Long orderGroupId,
            Principal principal) {
        Long userId = requestUserResolver.requireUserId(principal);

        ownershipService.requireOrderGroupOwner(userId, orderGroupId);

        var group = orderOrchestrationService
                .getOrderGroupDetails(orderGroupId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "ดึงข้อมูลคำสั่งซื้อสำเร็จ",
                        OrderGroupResponse.fromEntity(group)));
    }
}