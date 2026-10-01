package project.project.Controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.project.ApiResponse.ApiResponse;
import project.project.DTO.address.CreateAddressRequest;
import project.project.Security.CurrentUser;
import project.project.Service.implement.CustomerCheckoutService;
import project.project.Service.implement.CustomerPaymentService;

@RestController
@RequestMapping("/api/checkout")
public class CustomerCheckoutController {
    private final CurrentUser currentUser;
    private final CustomerCheckoutService checkout;
    private final CustomerPaymentService payments;

    public CustomerCheckoutController(CurrentUser currentUser, CustomerCheckoutService checkout, CustomerPaymentService payments) {
        this.currentUser = currentUser;
        this.checkout = checkout;
        this.payments = payments;
    }

    @GetMapping
    public ApiResponse<CustomerCheckoutService.Context> context() {
        return ApiResponse.success("Checkout loaded", checkout.context(currentUser.requireCustomerId()));
    }

    @PostMapping("/cart")
    public ApiResponse<CustomerCheckoutService.Context> cart(@Valid @RequestBody CustomerCheckoutService.SyncCart request) {
        return ApiResponse.success("Cart synchronized", checkout.synchronizeCart(currentUser.requireCustomerId(), request));
    }

    @PostMapping("/quote")
    public ApiResponse<?> quote(@Valid @RequestBody CustomerCheckoutService.Quote request) {
        return ApiResponse.success("Shipping calculated", checkout.quote(currentUser.requireCustomerId(), request));
    }

    @PostMapping("/addresses")
    public ResponseEntity<?> address(@Valid @RequestBody CreateAddressRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success("Address saved",
                checkout.addAddress(currentUser.requireCustomerId(), request)));
    }

    @PostMapping("/orders")
    public ResponseEntity<?> order(@Valid @RequestBody CustomerCheckoutService.PlaceOrder request) {
        return ResponseEntity.status(201).body(ApiResponse.success("Order created",
                checkout.placeOrder(currentUser.requireCustomerId(), request)));
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<?> order(@PathVariable Long id) {
        return ApiResponse.success("Order loaded", checkout.order(currentUser.requireCustomerId(), id));
    }

    @GetMapping("/orders")
    public ApiResponse<?> orderHistory() {
        return ApiResponse.success("Order history loaded", checkout.orderHistory(currentUser.requireCustomerId()));
    }

    @PostMapping("/orders/{id}/confirm-payment")
    public ApiResponse<?> confirmPayment(@PathVariable Long id) {
        return ApiResponse.success("Payment successful (demo)", payments.confirmPayment(currentUser.requireCustomerId(), id));
    }

    @PostMapping("/orders/{id}/cancel")
    public ApiResponse<?> cancelOrder(@PathVariable Long id) {
        return ApiResponse.success("Order cancelled", payments.cancelOrder(currentUser.requireCustomerId(), id));
    }
}
