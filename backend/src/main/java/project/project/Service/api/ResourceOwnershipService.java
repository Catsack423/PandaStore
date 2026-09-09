package project.project.Service.api;

public interface ResourceOwnershipService {

    void requireCustomerOwner(Long userId, Long customerId);

    void requireNotificationOwner(Long userId, Long notificationId);

    void requireOrderGroupOwner(Long userId, Long orderGroupId);
}