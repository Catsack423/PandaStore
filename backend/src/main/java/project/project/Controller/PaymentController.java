package project.project.Controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.project.ApiResponse.ApiResponse;
import project.project.DTO.payment.*;
import project.project.Entity.order.Payment;
import project.project.Service.api.PaymentService;
import project.project.Security.CurrentUser;
import project.project.Service.implement.CustomerPaymentService;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;
    private final CustomerPaymentService customerPayments;
    private final CurrentUser currentUser;

    public PaymentController(PaymentService paymentService, CustomerPaymentService customerPayments, CurrentUser currentUser) {
        this.paymentService = paymentService;
        this.customerPayments = customerPayments;
        this.currentUser = currentUser;
    }

    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest request) {
        Payment payment = paymentService.initiatePayment(
                request.getOrderGroupId(),
                request.getPaymentMethod(),
                request.getAmount()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("เริ่มทำรายการชำระเงินสำเร็จ", PaymentResponse.fromEntity(payment)));
    }

    @PostMapping("/callback")
    public ResponseEntity<ApiResponse<PaymentResponse>> handleGatewayCallback(
            @Valid @RequestBody PaymentCallbackRequest request) {
        // TODO: Enable only after verifying a real gateway's signed webhook and persisted amount.
        // An unsigned callback must not provide an alternate route around the demo feature flag.
        throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                "Payment gateway is not configured");
    }

    @PostMapping("/simulate")
    public ResponseEntity<ApiResponse<PaymentResponse>> simulatePayment(
            @RequestBody PaymentCallbackRequest request) {
        Long customerId = currentUser.requireCustomerId();
        if (request.getOrderGroupId() == null || Boolean.FALSE.equals(request.getIsSuccess())) {
            throw new IllegalArgumentException("Use an order group ID to confirm a demo payment");
        }
        // The legacy demo endpoint must enforce the same ownership, flag and pending-state guards.
        customerPayments.confirmPayment(customerId, request.getOrderGroupId());
        return ResponseEntity.ok(ApiResponse.success("Payment successful (demo)",
                PaymentResponse.fromEntity(paymentService.getPaymentByOrderGroupId(request.getOrderGroupId()))));
    }

    @PostMapping("/refund/partial")
    public ResponseEntity<ApiResponse<Void>> processPartialRefund(
            @Valid @RequestBody PartialRefundRequest request) {
        paymentService.processPartialRefund(request.getOrderId(), request.getRefundAmount(), request.getReason());
        return ResponseEntity.ok(ApiResponse.success("ดำเนินการคืนเงินบางส่วนสำเร็จ", null));
    }

    @PostMapping("/refund/full")
    public ResponseEntity<ApiResponse<Void>> processFullRefund(
            @Valid @RequestBody FullRefundRequest request) {
        paymentService.processFullRefund(request.getOrderGroupId(), request.getReason());
        return ResponseEntity.ok(ApiResponse.success("ดำเนินการคืนเงินเต็มจำนวนสำเร็จ", null));
    }

    @GetMapping("/order-group/{orderGroupId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrderGroupId(@PathVariable Long orderGroupId) {
        Payment payment = paymentService.getPaymentByOrderGroupId(orderGroupId);
        return ResponseEntity.ok(ApiResponse.success("ดึงข้อมูลการชำระเงินสำเร็จ", PaymentResponse.fromEntity(payment)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(@PathVariable Long id) {
        Payment payment = paymentService.getPaymentById(id);
        return ResponseEntity.ok(ApiResponse.success("ดึงข้อมูลการชำระเงินสำเร็จ", PaymentResponse.fromEntity(payment)));
    }

}
