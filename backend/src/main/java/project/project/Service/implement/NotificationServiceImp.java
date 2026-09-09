package project.project.Service.implement;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.project.Entity.notification.Notification;
import project.project.Entity.notification.NotificationType;
import project.project.Entity.order.Order;
import project.project.Entity.order.OrderGroup;
import project.project.Entity.review.Review;
import project.project.Entity.seller.SellerApplication;
import project.project.Entity.user.User;
import project.project.Entity.user.UserRole;
import project.project.Entity.user.UserStatus;
import project.project.Service.api.NotificationService;

import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class NotificationServiceImp implements NotificationService {

    private final EntityManager entityManager;

    public NotificationServiceImp(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Notification sendNotification(
            Long recipientUserId,
            String title,
            String message,
            NotificationType type) {

        requireText(title, "title");
        requireText(message, "message");

        if (title.trim().length() > 150) {
            throw new IllegalArgumentException(
                    "Notification title must not exceed 150 characters");
        }
        if (type == null) {
            throw new IllegalArgumentException(
                    "Notification type is required");
        }

        User recipient = find(
                User.class, recipientUserId, "recipientUserId");

        Notification notification = new Notification();
        notification.setRecipientUser(recipient);
        notification.setTitle(title.trim());
        notification.setMessage(message.trim());
        notification.setType(type);
        notification.setIsRead(false);

        entityManager.persist(notification);
        return notification;
    }

    @Override
    public void notifyCustomerOrderPaid(
            Long customerId,
            Long orderGroupId) {

        requireId(customerId, "customerId");

        OrderGroup group = find(
                OrderGroup.class, orderGroupId, "orderGroupId");

        requireOwner(
                group.getCustomer().getCustomerId(),
                customerId);

        sendNotification(
                group.getCustomer().getUser().getUserId(),
                "ชำระเงินสำเร็จ",
                "คำสั่งซื้อ " + group.getGroupNumber()
                        + " ชำระเงินสำเร็จแล้ว",
                NotificationType.PAYMENT_SUCCESS);
    }

    @Override
    public void notifySellerNewOrder(Long sellerId, Long orderId) {
        requireId(sellerId, "sellerId");

        Order order = find(Order.class, orderId, "orderId");
        requireOwner(order.getSeller().getSellerId(), sellerId);

        sendNotification(
                order.getSeller().getUser().getUserId(),
                "มีคำสั่งซื้อใหม่",
                "คำสั่งซื้อ " + order.getSubOrderNumber()
                        + " รอการยืนยัน",
                NotificationType.NEW_ORDER_FOR_SELLER);
    }

    @Override
    public void notifyAdminNewSellerApplication(Long applicationId) {
        SellerApplication application = find(
                SellerApplication.class,
                applicationId,
                "applicationId");

        List<User> admins = entityManager.createQuery(
                """
                        select u from User u
                        where u.role = :role
                          and u.status = :status
                        """,
                User.class)
                .setParameter("role", UserRole.ADMIN)
                .setParameter("status", UserStatus.ACTIVE)
                .getResultList();

        for (User admin : admins) {
            sendNotification(
                    admin.getUserId(),
                    "มีคำขอสมัครร้านค้าใหม่",
                    "คำขอสมัครร้านค้า "
                            + application.getShopName()
                            + " รอการตรวจสอบ",
                    NotificationType.NEW_SELLER_APPLICATION);
        }
    }

    @Override
    public void notifyCustomerOrderShipped(
            Long customerId,
            Long orderId,
            String trackingNumber) {

        requireId(customerId, "customerId");
        requireText(trackingNumber, "trackingNumber");

        Order order = find(Order.class, orderId, "orderId");

        requireOwner(
                order.getOrderGroup().getCustomer().getCustomerId(),
                customerId);

        sendNotification(
                order.getOrderGroup().getCustomer()
                        .getUser().getUserId(),
                "จัดส่งสินค้าแล้ว",
                "คำสั่งซื้อ " + order.getSubOrderNumber()
                        + " ถูกจัดส่งแล้ว เลขพัสดุ: "
                        + trackingNumber.trim(),
                NotificationType.ORDER_SHIPPED);
    }

    @Override
    public void notifySellerNewReview(Long sellerId, Long reviewId) {
        requireId(sellerId, "sellerId");

        Review review = find(Review.class, reviewId, "reviewId");

        requireOwner(
                review.getProduct().getSeller().getSellerId(),
                sellerId);

        sendNotification(
                review.getProduct().getSeller()
                        .getUser().getUserId(),
                "มีรีวิวสินค้าใหม่",
                "สินค้า " + review.getProduct().getName()
                        + " ได้รับรีวิวใหม่",
                NotificationType.NEW_REVIEW);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(Long userId) {
        requireId(userId, "userId");

        return entityManager.createQuery(
                """
                        select n from Notification n
                        where n.recipientUser.userId = :userId
                        order by n.createdAt desc, n.notificationId desc
                        """,
                Notification.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    /**
     * ใช้กับคำขอจากผู้ใช้ โดย userId ต้องมาจากบัญชีที่ login
     * หากเรียกผ่าน NotificationService ให้เพิ่ม signature นี้ใน interface
     */
    @Override
    public void markAsRead(Long userId, Long notificationId) {
        requireId(userId, "userId");

        Notification notification = find(
                Notification.class,
                notificationId,
                "notificationId");

        requireOwner(
                notification.getRecipientUser().getUserId(),
                userId);

        notification.setIsRead(true);
    }

    private <T> T find(Class<T> type, Long id, String name) {
        requireId(id, name);

        T entity = entityManager.find(type, id);
        if (entity == null) {
            throw new EntityNotFoundException(
                    type.getSimpleName() + " not found: " + id);
        }
        return entity;
    }

    private void requireOwner(Long actualId, Long expectedId) {
        if (!Objects.equals(actualId, expectedId)) {
            throw new IllegalArgumentException(
                    "The resource does not belong to this user");
        }
    }

    private void requireId(Long id, String name) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    name + " must be a positive value");
        }
    }

    private void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    name + " is required");
        }
    }
}