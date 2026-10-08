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
@RequestMapping({"/api/customers/{customerId}/addresses", "/api/v1/customers/{customerId}/addresses"})
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
                .body(ApiResponse.success("Address added successfully", AddressResponse.fromEntity(address)));
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @PathVariable("customerId") Long customerId,
            @PathVariable("addressId") Long addressId,
            @Valid @RequestBody CreateAddressRequest request,
            Principal principal) {

        requireOwner(principal, customerId);

        var address = addressService.updateAddress(customerId, addressId, request);

        return ResponseEntity.ok(
                ApiResponse.success("Address updated successfully", AddressResponse.fromEntity(address)));
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
                ApiResponse.success("Address retrieved successfully", addresses));
    }

    @GetMapping("/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddress(
            @PathVariable("customerId") Long customerId,
            @PathVariable("addressId") Long addressId,
            Principal principal) {
        requireOwner(principal, customerId);
        return ResponseEntity.ok(ApiResponse.success("Address retrieved successfully",
                AddressResponse.fromEntity(addressService.getAddress(customerId, addressId))));
    }

    @PutMapping("/{addressId}/default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefaultAddress(
            @PathVariable("customerId") Long customerId,
            @PathVariable("addressId") Long addressId,
            Principal principal) {
        requireOwner(principal, customerId);
        return ResponseEntity.ok(ApiResponse.success("Default address updated",
                AddressResponse.fromEntity(addressService.setDefaultAddress(customerId, addressId))));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable("customerId") Long customerId,
            @PathVariable("addressId") Long addressId,
            Principal principal) {

        requireOwner(principal, customerId);

        boolean deleted = addressService
                .removeAddressCustomerByCustomerIdAndAddressId(customerId, addressId);

        if (!deleted) {
            throw new EntityNotFoundException("Address not found");
        }

        return ResponseEntity.noContent().build();
    }

    private void requireOwner(Principal principal, Long customerId) {
        Long userId = requestUserResolver.requireUserId(principal);
        ownershipService.requireCustomerOwner(userId, customerId);
    }
}
