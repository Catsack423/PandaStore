package project.project.Service.api;

import project.project.Entity.notification.Notification;
import project.project.Entity.notification.NotificationType;
import java.util.List;

public interface NotificationService {
    Notification sendNotification(Long recipientUserId, String title, String message, NotificationType type);

    void notifyCustomerOrderPaid(Long customerId, Long orderGroupId);

    void notifySellerNewOrder(Long sellerId, Long orderId);

    void notifyAdminNewSellerApplication(Long applicationId);

    void notifyCustomerOrderShipped(Long customerId, Long orderId, String trackingNumber);

    void notifySellerNewReview(Long sellerId, Long reviewId);

    List<Notification> getUserNotifications(Long userId);

    void markAsRead(Long userId, Long notificationId);
}
