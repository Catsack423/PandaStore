package project.project.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import project.project.DTO.customer.CreateCustomerRequest;
import project.project.DTO.customer.CustomerResponse;
import project.project.DTO.customer.UpdateCustomerRequest;
import project.project.Service.api.CustomerService;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        long customerId = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "success", true,
            "message", "สร้างบัญชีลูกค้าสำเร็จ",
            "customerId", customerId
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCustomer(@PathVariable long id) {
        CustomerResponse customer = customerService.getCustomer(id);
        if (customer == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "ไม่พบข้อมูลลูกค้าที่มีรหัส " + id));
        }
        return ResponseEntity.ok(customer);
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomer());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCustomer(@PathVariable long id, @Valid @RequestBody UpdateCustomerRequest request) {
        boolean updated = customerService.updateCustomer(id, request);
        if (!updated) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "ไม่พบข้อมูลลูกค้าที่ต้องการแก้ไข"));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "แก้ไขข้อมูลลูกค้าสำเร็จ"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCustomer(@PathVariable long id) {
        boolean deleted = customerService.deleteCustomer(id);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "ไม่พบข้อมูลลูกค้าที่ต้องการลบ"));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "ลบข้อมูลลูกค้าสำเร็จ"));
    }
}
