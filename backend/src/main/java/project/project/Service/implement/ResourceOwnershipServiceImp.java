package project.project.Service.implement;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.project.Repository.CustomerRepository;
import project.project.Repository.NotificationRepository;
import project.project.Repository.OrderGroupRepository;
import project.project.Service.api.ResourceOwnershipService;

import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class ResourceOwnershipServiceImp
        implements ResourceOwnershipService {

    private final CustomerRepository customerRepository;
    private final NotificationRepository notificationRepository;
    private final OrderGroupRepository orderGroupRepository;

    public ResourceOwnershipServiceImp(
            CustomerRepository customerRepository,
            NotificationRepository notificationRepository,
            OrderGroupRepository orderGroupRepository) {
        this.customerRepository = customerRepository;
        this.notificationRepository = notificationRepository;
        this.orderGroupRepository = orderGroupRepository;
    }

    @Override
    public void requireCustomerOwner(Long userId, Long customerId) {
        var customer = customerRepository.findById(customerId)
                .orElseThrow(() -> notFound("Customer"));

        requireOwner(userId, customer.getUser().getUserId(), "Customer");
    }

    @Override
    public void requireNotificationOwner(Long userId, Long notificationId) {
        var notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> notFound("Notification"));

        requireOwner(
                userId,
                notification.getRecipientUser().getUserId(),
                "Notification");
    }

    @Override
    public void requireOrderGroupOwner(Long userId, Long orderGroupId) {
        var group = orderGroupRepository.findById(orderGroupId)
                .orElseThrow(() -> notFound("Order group"));

        requireOwner(
                userId,
                group.getCustomer().getUser().getUserId(),
                "Order group");
    }

    private void requireOwner(
            Long requestedUserId,
            Long ownerUserId,
            String resource) {
        if (requestedUserId == null
                || !Objects.equals(requestedUserId, ownerUserId)) {
            throw notFound(resource);
        }
    }

    private EntityNotFoundException notFound(String resource) {
        return new EntityNotFoundException(resource + " not found");
    }
}