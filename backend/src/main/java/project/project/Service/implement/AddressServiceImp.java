package project.project.Service.implement;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import project.project.DTO.address.CreateAddressRequest;
import project.project.Entity.user.Address;
import project.project.Entity.user.Customer;
import project.project.Repository.AddressRepository;
import project.project.Repository.CustomerRepository;
import project.project.Repository.OrderGroupRepository;
import project.project.Service.api.AddressService;

import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class AddressServiceImp implements AddressService {

    private final AddressRepository addressRepository;
    private final CustomerRepository customerRepository;
    private final OrderGroupRepository orderGroupRepository;

    public AddressServiceImp(
            AddressRepository addressRepository,
            CustomerRepository customerRepository,
            OrderGroupRepository orderGroupRepository) {

        this.addressRepository = addressRepository;
        this.customerRepository = customerRepository;
        this.orderGroupRepository = orderGroupRepository;
    }

    @Override
    public Address addAddressToCustomerByCustomerId(
            Long customerId,
            CreateAddressRequest request) {

        requirePositiveId(customerId, "customerId");
        Assert.notNull(request, "Address request is required");

        Assert.isTrue(
                Objects.equals(customerId, request.customerId()),
                "Customer ID in URL and request must match");

        Customer customer = lockCustomer(customerId);

        List<Address> existingAddresses = addressRepository
                .findByCustomer_CustomerIdOrderByIsDefaultDescAddressIdAsc(customerId);

        boolean makeDefault = existingAddresses.isEmpty()
                || Boolean.TRUE.equals(request.isDefault());

        if (makeDefault) {
            existingAddresses.forEach(address -> address.setIsDefault(false));
        }

        Address address = new Address();
        address.setCustomer(customer);
        address.setReceiverName(request.receiverName());
        address.setPhoneNumber(request.phoneNumber());
        address.setAddressLine(request.addressLine());
        address.setDistrict(request.district());
        address.setProvince(request.province());
        address.setPostalCode(request.postalCode());
        address.setIsDefault(makeDefault);

        return addressRepository.save(address);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Address> getAllAddressesByCustomerId(Long customerId) {
        requirePositiveId(customerId, "customerId");

        if (!customerRepository.existsById(customerId)) {
            throw new EntityNotFoundException("Customer not found: " + customerId);
        }

        return addressRepository
                .findByCustomer_CustomerIdOrderByIsDefaultDescAddressIdAsc(customerId);
    }

    @Override
    public boolean removeAddressCustomerByCustomerIdAndAddressId(Long customerId, Long addressId) {

        requirePositiveId(customerId, "customerId");
        requirePositiveId(addressId, "addressId");

        lockCustomer(customerId);

        var found = addressRepository.findOwnedAddressForUpdate(customerId, addressId);

        // ไม่พบ หรือเป็นที่อยู่ของลูกค้าคนอื่น
        if (found.isEmpty()) {
            return false;
        }

        Address address = found.get();

        if (orderGroupRepository
                .existsByShippingAddress_AddressId(addressId)) {
            throw new IllegalStateException("ไม่สามารถลบที่อยู่ที่มีคำสั่งซื้ออ้างอิงอยู่ได้");
        }

        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());

        addressRepository.delete(address);

        // ตรวจข้อจำกัดฐานข้อมูลและลบก่อนค้นหารายการที่เหลือ
        addressRepository.flush();

        if (wasDefault) {
            List<Address> remaining = addressRepository
                    .findByCustomer_CustomerIdOrderByIsDefaultDescAddressIdAsc(customerId);

            if (!remaining.isEmpty()) {
                remaining.forEach(value -> value.setIsDefault(false));
                remaining.get(0).setIsDefault(true);
            }
        }

        return true;
    }

    private Customer lockCustomer(Long customerId) {
        return customerRepository.findByIdForUpdate(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + customerId));
    }

    private void requirePositiveId(Long id, String name) {
        Assert.isTrue(id != null && id > 0, name + " must be positive");
    }
}