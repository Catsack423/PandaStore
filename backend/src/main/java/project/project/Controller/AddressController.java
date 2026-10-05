package project.project.Controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import project.project.ApiResponse.ApiResponse;
import project.project.DTO.address.AddressResponse;
import project.project.DTO.address.CreateAddressRequest;
import project.project.Service.api.AddressService;
import project.project.Service.api.ResourceOwnershipService;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/customers/{customerId}/addresses")
public class AddressController {

    private final AddressService addressService;
    private final RequestUserResolver requestUserResolver;
    private final ResourceOwnershipService ownershipService;

    public AddressController(
            AddressService addressService,
            RequestUserResolver requestUserResolver,
            ResourceOwnershipService ownershipService) {

        this.addressService = addressService;
        this.requestUserResolver = requestUserResolver;
        this.ownershipService = ownershipService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(
            @PathVariable("customerId") Long customerId,
            @Valid @RequestBody CreateAddressRequest request,
            Principal principal) {

        requireOwner(principal, customerId);

        var address = addressService.addAddressToCustomerByCustomerId(customerId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("เพิ่มที่อยู่สำเร็จ", AddressResponse.fromEntity(address)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(
            @PathVariable("customerId") Long customerId,
            Principal principal) {

        requireOwner(principal, customerId);

        var addresses = addressService
                .getAllAddressesByCustomerId(customerId)
                .stream()
                .map(AddressResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(
                ApiResponse.success("ดึงข้อมูลที่อยู่สำเร็จ", addresses));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @PathVariable("customerId") Long customerId,
            @PathVariable("addressId") Long addressId,
            Principal principal) {

        requireOwner(principal, customerId);

        boolean deleted = addressService
                .removeAddressCustomerByCustomerIdAndAddressId(customerId, addressId);

        if (!deleted) {
            throw new EntityNotFoundException("Address not found");
        }

        return ResponseEntity.ok(
                ApiResponse.<Void>success("ลบที่อยู่สำเร็จ", null));
    }

    private void requireOwner(Principal principal, Long customerId) {
        Long userId = requestUserResolver.requireUserId(principal);
        ownershipService.requireCustomerOwner(userId, customerId);
    }
}