package project.project.DTO.address;

import project.project.Entity.user.Address;

public record AddressResponse(
        Long addressId,
        String receiverName,
        String phoneNumber,
        String addressLine,
        String district,
        String province,
        String postalCode,
        boolean isDefault) {

    public static AddressResponse fromEntity(Address address) {
        return new AddressResponse(
                address.getAddressId(),
                address.getReceiverName(),
                address.getPhoneNumber(),
                address.getAddressLine(),
                address.getDistrict(),
                address.getProvince(),
                address.getPostalCode(),
                Boolean.TRUE.equals(address.getIsDefault()));
    }
}