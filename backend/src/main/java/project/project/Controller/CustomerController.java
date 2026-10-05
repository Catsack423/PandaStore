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
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        long customerId = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("สร้างบัญชีลูกค้าสำเร็จ", customerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomer(@PathVariable long id) {
        CustomerResponse customer = customerService.getCustomer(id);
        if (customer == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("ไม่พบข้อมูลลูกค้า", null));
        }
        return ResponseEntity.ok(ApiResponse.success("ดึงข้อมูลลูกค้าสำเร็จ", customer));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getAllCustomers() {
        return ResponseEntity.ok(ApiResponse.success("ดึงข้อมูลลูกค้าทั้งหมดสำเร็จ", customerService.getAllCustomer()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateCustomer(@PathVariable long id, @Valid @RequestBody UpdateCustomerRequest request) {
        boolean updated = customerService.updateCustomer(id, request);
        if (!updated) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("ไม่พบข้อมูลลูกค้าที่ต้องการแก้ไข", null));
        }
        return ResponseEntity.ok(ApiResponse.success("แก้ไขข้อมูลลูกค้าสำเร็จ", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable long id) {
        boolean deleted = customerService.deleteCustomer(id);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("ไม่พบข้อมูลลูกค้าที่ต้องการลบ", null));
        }
        return ResponseEntity.ok(ApiResponse.success("ลบข้อมูลลูกค้าสำเร็จ", null));
    }
}