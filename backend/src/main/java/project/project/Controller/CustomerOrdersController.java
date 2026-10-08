package project.project.Controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import project.project.ApiResponse.ApiResponse;
import project.project.Security.CurrentUser;
import project.project.Service.implement.CustomerCheckoutService;

@RestController
@RequestMapping("/api/v1/customers/{customerId}/orders")
public class CustomerOrdersController {
    private final CurrentUser currentUser;
    private final CustomerCheckoutService checkout;

    public CustomerOrdersController(CurrentUser currentUser, CustomerCheckoutService checkout) {
        this.currentUser = currentUser;
        this.checkout = checkout;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<?>> create(@PathVariable Long customerId,
            @Valid @RequestBody CustomerCheckoutService.PlaceOrder request) {
        requireOwner(customerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created", checkout.placeOrder(customerId, request)));
    }

    @GetMapping
    public ApiResponse<?> list(@PathVariable Long customerId) {
        requireOwner(customerId);
        return ApiResponse.success("Order history loaded", checkout.orderHistory(customerId));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<?> get(@PathVariable Long customerId, @PathVariable Long orderId) {
        requireOwner(customerId);
        return ApiResponse.success("Order loaded", checkout.order(customerId, orderId));
    }

    private void requireOwner(Long customerId) {
        if (customerId == null || customerId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer ID must be positive");
        }
        if (!customerId.equals(currentUser.requireCustomerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customer account owner required");
        }
    }
}
