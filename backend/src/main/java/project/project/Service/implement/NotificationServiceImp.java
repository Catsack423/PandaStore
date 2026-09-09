package project.project.Service.implement;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import project.project.Entity.notification.Notification;
import project.project.Entity.notification.NotificationType;
import project.project.Entity.user.User;
import project.project.Entity.user.UserRole;
import project.project.Entity.user.UserStatus;
import project.project.Repository.CustomerRepository;
import project.project.Repository.NotificationRepository;
import project.project.Repository.SellerRepository;
import project.project.Repository.UserRepository;
import project.project.Service.api.NotificationService;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class NotificationServiceImp implements NotificationService {

        private final NotificationRepository notificationRepository;
        private final UserRepository userRepository;
        private final CustomerRepository customerRepository;
        private final SellerRepository sellerRepository;

        public NotificationServiceImp(
                        NotificationRepository notificationRepository,
                        UserRepository userRepository,
                        CustomerRepository customerRepository,
                        SellerRepository sellerRepository) {
                this.notificationRepository = notificationRepository;
                this.userRepository = userRepository;
                this.customerRepository = customerRepository;
                this.sellerRepository = sellerRepository;
        }

        @Override
        @Transactional
        public Notification sendNotification(
                        Long recipientUserId,
                        String title,
                        String message,
                        NotificationType type) {
                Assert.notNull(recipientUserId, "Recipient user ID is required");
                Assert.hasText(title, "Title is required");
                Assert.isTrue(title.length() <= 150, "Title exceeds 150 characters");
                Assert.hasText(message, "Message is required");
                Assert.notNull(type, "Notification type is required");

                User recipient = userRepository.findById(recipientUserId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "User not found: " + recipientUserId));

                return saveNotification(recipient, title, message, type);
        }

        @Override
        @Transactional
        public void notifyCustomerOrderPaid(Long customerId, Long orderGroupId) {
                Assert.notNull(orderGroupId, "Order group ID is required");

                sendNotification(
                                findCustomerUserId(customerId),
                                "ชำระเงินสำเร็จ",
                                "ได้รับการชำระเงินสำหรับคำสั่งซื้อ #" + orderGroupId,
                                NotificationType.PAYMENT_SUCCESS);
        }

        @Override
        @Transactional
        public void notifySellerNewOrder(Long sellerId, Long orderId) {
                Assert.notNull(orderId, "Order ID is required");

                sendNotification(
                                findSellerUserId(sellerId),
                                "มีคำสั่งซื้อใหม่",
                                "คำสั่งซื้อ #" + orderId + " รอการยืนยันจากร้านค้า",
                                NotificationType.NEW_ORDER_FOR_SELLER);
        }

        @Override
        @Transactional
        public void notifyAdminNewSellerApplication(Long applicationId) {
                Assert.notNull(applicationId, "Application ID is required");

                List<User> admins = userRepository.findByRoleAndStatus(
                                UserRole.ADMIN,
                                UserStatus.ACTIVE);

                for (User admin : admins) {
                        saveNotification(
                                        admin,
                                        "มีคำขอสมัครผู้ขายใหม่",
                                        "กรุณาตรวจสอบคำขอสมัครผู้ขาย #" + applicationId,
                                        NotificationType.NEW_SELLER_APPLICATION);
                }
        }

        @Override
        @Transactional
        public void notifyCustomerOrderShipped(
                        Long customerId,
                        Long orderId,
                        String trackingNumber) {
                Assert.notNull(orderId, "Order ID is required");
                Assert.hasText(trackingNumber, "Tracking number is required");

                sendNotification(
                                findCustomerUserId(customerId),
                                "จัดส่งสินค้าแล้ว",
                                "คำสั่งซื้อ #" + orderId
                                                + " เลขติดตามพัสดุ: " + trackingNumber,
                                NotificationType.ORDER_SHIPPED);
        }

        @Override
        @Transactional
        public void notifySellerNewReview(Long sellerId, Long reviewId) {
                Assert.notNull(reviewId, "Review ID is required");

                sendNotification(
                                findSellerUserId(sellerId),
                                "มีรีวิวใหม่",
                                "ร้านค้าของคุณได้รับรีวิวใหม่ #" + reviewId,
                                NotificationType.NEW_REVIEW);
        }

        @Override
        public List<Notification> getUserNotifications(Long userId) {
                Assert.notNull(userId, "User ID is required");

                return notificationRepository
                                .findByRecipientUser_UserIdOrderByCreatedAtDescNotificationIdDesc(
                                                userId);
        }

        @Override
        @Transactional
        public void markAsRead(Long notificationId) {
                Assert.notNull(notificationId, "Notification ID is required");

                Notification notification = notificationRepository
                                .findById(notificationId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Notification not found: " + notificationId));

                notification.setIsRead(true);
        }

        private Notification saveNotification(
                        User recipient,
                        String title,
                        String message,
                        NotificationType type) {
                Notification notification = new Notification();
                notification.setRecipientUser(recipient);
                notification.setTitle(title);
                notification.setMessage(message);
                notification.setType(type);
                notification.setIsRead(false);

                return notificationRepository.save(notification);
        }

        private Long findCustomerUserId(Long customerId) {
                Assert.notNull(customerId, "Customer ID is required");

                return customerRepository.findById(customerId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Customer not found: " + customerId))
                                .getUser()
                                .getUserId();
        }

        private Long findSellerUserId(Long sellerId) {
                Assert.notNull(sellerId, "Seller ID is required");

                return sellerRepository.findById(sellerId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Seller not found: " + sellerId))
                                .getUser()
                                .getUserId();
        }
}