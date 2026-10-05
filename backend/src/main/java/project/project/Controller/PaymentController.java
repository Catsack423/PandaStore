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
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
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
        String txnId = resolveGatewayTransactionId(request);
        boolean isSuccess = request.getIsSuccess() != null ? request.getIsSuccess() : true;

        paymentService.handleGatewayCallback(txnId, isSuccess);
        Payment updatedPayment = (request.getOrderGroupId() != null)
                ? paymentService.getPaymentByOrderGroupId(request.getOrderGroupId())
                : paymentService.getPaymentByGatewayTransactionId(txnId);

        return ResponseEntity.ok(ApiResponse.success("ประมวลผลผลการชำระเงินสำเร็จ", PaymentResponse.fromEntity(updatedPayment)));
    }

    @PostMapping("/simulate")
    public ResponseEntity<ApiResponse<PaymentResponse>> simulatePayment(
            @RequestBody PaymentCallbackRequest request) {
        String txnId = resolveGatewayTransactionId(request);
        boolean isSuccess = request.getIsSuccess() != null ? request.getIsSuccess() : true;

        paymentService.handleGatewayCallback(txnId, isSuccess);
        Payment updatedPayment = (request.getOrderGroupId() != null)
                ? paymentService.getPaymentByOrderGroupId(request.getOrderGroupId())
                : paymentService.getPaymentByGatewayTransactionId(txnId);

        String msg = isSuccess ? "จำลองการชำระเงินสำเร็จ (PAID)" : "จำลองการชำระเงินล้มเหลว (FAILED)";
        return ResponseEntity.ok(ApiResponse.success(msg, PaymentResponse.fromEntity(updatedPayment)));
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

    private String resolveGatewayTransactionId(PaymentCallbackRequest request) {
        if (request.getGatewayTransactionId() != null && !request.getGatewayTransactionId().trim().isEmpty()) {
            return request.getGatewayTransactionId().trim();
        }
        if (request.getOrderGroupId() != null) {
            Payment payment = paymentService.getPaymentByOrderGroupId(request.getOrderGroupId());
            return payment.getGatewayTransactionId();
        }
        throw new IllegalArgumentException("ต้องระบุ gatewayTransactionId หรือ orderGroupId อย่างใดอย่างหนึ่ง");
    }
}
