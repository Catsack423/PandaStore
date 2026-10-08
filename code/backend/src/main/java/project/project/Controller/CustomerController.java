package project.project.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import project.project.ApiResponse.ApiResponse;
import project.project.DTO.customer.CreateCustomerRequest;
import project.project.DTO.customer.CustomerResponse;
import project.project.DTO.customer.UpdateCustomerRequest;
import project.project.Service.api.CustomerService;

@RestController
@RequestMapping({"/api/customers", "/api/v1/customers"})
public class CustomerController {

    private final CustomerService customerService;
    private final project.project.Security.CurrentUser currentUser;

    public CustomerController(CustomerService customerService, project.project.Security.CurrentUser currentUser) {
        this.customerService = customerService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        long customerId = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer account created successfully", customerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomer(@PathVariable long id) {
        requireOwner(id);
        CustomerResponse customer = customerService.getCustomer(id);
        if (customer == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Customer not found", null));
        }
        return ResponseEntity.ok(ApiResponse.success("Customer retrieved successfully", customer));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getAllCustomers() {
        return ResponseEntity.ok(ApiResponse.success("Customers retrieved successfully", customerService.getAllCustomer()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateCustomer(@PathVariable long id, @Valid @RequestBody UpdateCustomerRequest request) {
        requireOwner(id);
        boolean updated = customerService.updateCustomer(id, request);
        if (!updated) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Customer to update not found", null));
        }
        return ResponseEntity.ok(ApiResponse.success("Customer updated successfully", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable long id) {
        requireOwner(id);
        boolean deleted = customerService.deleteCustomer(id);
        if (!deleted) {
            throw new jakarta.persistence.EntityNotFoundException("Customer not found");
        }
        return ResponseEntity.noContent().build();
    }

    private void requireOwner(long customerId) {
        if (currentUser.requireIdentity().role() == project.project.Entity.user.UserRole.ADMIN) return;
        if (currentUser.requireCustomerId() != customerId) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Customer account owner required");
        }
    }
}
