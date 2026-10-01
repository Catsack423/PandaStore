package project.project.Controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.project.ApiResponse.ApiResponse;
import project.project.DTO.shipping.*;
import project.project.Entity.order.Shipment;
import project.project.Service.api.ShippingService;
import project.project.Security.OrderAccess;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/shipping")
public class ShippingController {

    private final ShippingService shippingService;
    private final OrderAccess access;

    public ShippingController(ShippingService shippingService, OrderAccess access) {
        this.shippingService = shippingService;
        this.access = access;
    }

    @PostMapping("/calculate-fee")
    public ResponseEntity<ApiResponse<ShippingFeeResponse>> calculateShippingFee(
            @Valid @RequestBody CalculateShippingFeeRequest request) {

        BigDecimal fee = shippingService.calculateShippingFee(
                request.getSellerId(),
                request.getShippingMethod(),
                request.getAddressId()
        );

        ShippingFeeResponse response = new ShippingFeeResponse(
                request.getSellerId(),
                request.getShippingMethod(),
                fee
        );

        return ResponseEntity.ok(ApiResponse.success("คำนวณค่าจัดส่งสำเร็จ", response));
    }

    @PostMapping("/orders/{orderId}/assign-tracking")
    public ResponseEntity<ApiResponse<ShipmentResponse>> assignTrackingNumber(
            @PathVariable Long orderId,
            @RequestParam Long sellerId,
            @Valid @RequestBody AssignTrackingRequest request) {

        access.requireSellerOrder(sellerId, orderId);
        Shipment shipment = shippingService.assignTrackingNumber(
                sellerId,
                orderId,
                request.getCourierName(),
                request.getTrackingNumber()
        );

        return ResponseEntity.ok(ApiResponse.success("บันทึกหมายเลข Tracking สำเร็จ", ShipmentResponse.fromEntity(shipment)));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<ShipmentResponse>> getShipmentByOrderId(@PathVariable Long orderId) {
        access.requireOrderRead(orderId);
        Shipment shipment = shippingService.getShipmentByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success("ดึงข้อมูลการจัดส่งสำเร็จ", ShipmentResponse.fromEntity(shipment)));
    }

    @PatchMapping("/shipments/{shipmentId}/status")
    public ResponseEntity<ApiResponse<Void>> updateShippingStatus(
            @PathVariable Long shipmentId,
            @Valid @RequestBody UpdateShippingStatusRequest request) {

        access.requireAdmin();
        shippingService.updateShippingStatus(shipmentId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("อัปเดตสถานะการจัดส่งสำเร็จ", null));
    }
}
