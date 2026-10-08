package project.project.Controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.project.ApiResponse.ApiResponse;
import project.project.DTO.payment.*;
import project.project.Entity.order.Payment;
import project.project.Service.api.PaymentService;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final project.project.Security.OrderAccess access;

    public PaymentController(PaymentService paymentService, project.project.Security.OrderAccess access) {
        this.paymentService = paymentService;
        this.access = access;
    }

    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest request) {
        access.requireGroupRead(request.getOrderGroupId());
        Payment payment = paymentService.initiatePayment(
                request.getOrderGroupId(),
                request.getPaymentMethod(),
                request.getAmount()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment initiated successfully", PaymentResponse.fromEntity(payment)));
    }

    @PostMapping("/callback")
    public ResponseEntity<ApiResponse<PaymentResponse>> handleGatewayCallback(
            @Valid @RequestBody PaymentCallbackRequest request) {
        access.requireAdmin();
        String txnId = resolveGatewayTransactionId(request);
        boolean isSuccess = request.getIsSuccess() != null ? request.getIsSuccess() : true;

        paymentService.handleGatewayCallback(txnId, isSuccess);
        Payment updatedPayment = (request.getOrderGroupId() != null)
                ? paymentService.getPaymentByOrderGroupId(request.getOrderGroupId())
                : paymentService.getPaymentByGatewayTransactionId(txnId);

        return ResponseEntity.ok(ApiResponse.success("Payment result processed successfully", PaymentResponse.fromEntity(updatedPayment)));
    }

    @PostMapping("/simulate")
    public ResponseEntity<ApiResponse<PaymentResponse>> simulatePayment(
            @Valid @RequestBody PaymentCallbackRequest request) {
        String txnId = resolveGatewayTransactionId(request);
        // Authorize the transaction that will be mutated, even when both identifiers are supplied.
        final Payment targetPayment = paymentService.getPaymentByGatewayTransactionId(txnId);
        final Long targetGroupId = targetPayment.getOrderGroup().getOrderGroupId();
        access.requireGroupRead(targetGroupId);
        if (request.getOrderGroupId() != null && !request.getOrderGroupId().equals(targetGroupId)) {
            throw new IllegalArgumentException("Payment transaction does not match order group");
        }
        boolean isSuccess = request.getIsSuccess() != null ? request.getIsSuccess() : true;

        paymentService.handleGatewayCallback(txnId, isSuccess);
        Payment updatedPayment = (request.getOrderGroupId() != null)
                ? paymentService.getPaymentByOrderGroupId(request.getOrderGroupId())
                : paymentService.getPaymentByGatewayTransactionId(txnId);

        String msg = isSuccess ? "Payment simulation successful (PAID)" : "Payment simulation failed (FAILED)";
        return ResponseEntity.ok(ApiResponse.success(msg, PaymentResponse.fromEntity(updatedPayment)));
    }

    @PostMapping("/refund/partial")
    public ResponseEntity<ApiResponse<Void>> processPartialRefund(
            @Valid @RequestBody PartialRefundRequest request) {
        access.requireAdmin();
        paymentService.processPartialRefund(request.getOrderId(), request.getRefundAmount(), request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Partial refund completed successfully", null));
    }

    @PostMapping("/refund/full")
    public ResponseEntity<ApiResponse<Void>> processFullRefund(
            @Valid @RequestBody FullRefundRequest request) {
        access.requireAdmin();
        paymentService.processFullRefund(request.getOrderGroupId(), request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Full refund completed successfully", null));
    }

    @GetMapping("/order-group/{orderGroupId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrderGroupId(@PathVariable Long orderGroupId) {
        access.requireGroupRead(orderGroupId);
        Payment payment = paymentService.getPaymentByOrderGroupId(orderGroupId);
        return ResponseEntity.ok(ApiResponse.success("Payment retrieved successfully", PaymentResponse.fromEntity(payment)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(@PathVariable Long id) {
        Payment payment = paymentService.getPaymentById(id);
        access.requireGroupRead(payment.getOrderGroup().getOrderGroupId());
        return ResponseEntity.ok(ApiResponse.success("Payment retrieved successfully", PaymentResponse.fromEntity(payment)));
    }

    private String resolveGatewayTransactionId(PaymentCallbackRequest request) {
        if (request.getGatewayTransactionId() != null && !request.getGatewayTransactionId().trim().isEmpty()) {
            return request.getGatewayTransactionId().trim();
        }
        if (request.getOrderGroupId() != null) {
            Payment payment = paymentService.getPaymentByOrderGroupId(request.getOrderGroupId());
            return payment.getGatewayTransactionId();
        }
        throw new IllegalArgumentException("Specify either gatewayTransactionId or orderGroupId");
    }
}
