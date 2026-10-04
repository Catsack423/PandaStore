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
        boolean isDefault,
        boolean hasOrders) {

    public AddressResponse(
            Long addressId,
            String receiverName,
            String phoneNumber,
            String addressLine,
            String district,
            String province,
            String postalCode,
            boolean isDefault) {
        this(addressId, receiverName, phoneNumber, addressLine, district, province, postalCode, isDefault, false);
    }

    public static AddressResponse fromEntity(Address address) {
        return fromEntity(address, false);
    }

    public static AddressResponse fromEntity(Address address, boolean hasOrders) {
        return new AddressResponse(
                address.getAddressId(),
                address.getReceiverName(),
                address.getPhoneNumber(),
                address.getAddressLine(),
                address.getDistrict(),
                address.getProvince(),
                address.getPostalCode(),
                Boolean.TRUE.equals(address.getIsDefault()),
                hasOrders);
    }
}