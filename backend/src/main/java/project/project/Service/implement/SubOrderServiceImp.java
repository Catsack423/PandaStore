package project.project.Service.implement;

import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import project.project.Entity.order.Order;
import project.project.Entity.order.OrderItem;
import project.project.Entity.order.OrderStatus;
import project.project.Entity.order.OrderGroup;
import project.project.Entity.order.OrderGroupPaymentStatus;
import project.project.Entity.product.Product;
import project.project.Repository.OrderRepository;
import project.project.Repository.ProductRepository;
import project.project.Service.api.NotificationService;
import project.project.Service.api.PaymentService;
import project.project.Service.api.SubOrderService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SubOrderServiceImp implements SubOrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PaymentService paymentService;
    private final NotificationService notificationService;

    // Constructor Injection (SOLID - Dependency Inversion Principle)
    public SubOrderServiceImp(OrderRepository orderRepository,
            ProductRepository productRepository,
            PaymentService paymentService,
            NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.paymentService = paymentService;
        this.notificationService = notificationService;
    }

    /**
     * ค้นหา Sub-Order ด้วย orderId
     * 
     * @param orderId รหัสคำสั่งซื้อย่อย
     * @return Order entity
     * @throws RuntimeException ถ้าไม่พบ
     */
    
    @Override
    public Order getSubOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException(
                        "ไม่พบคำสั่งซื้อ orderId: " + orderId));
    }

    /**
     * ค้นหา Sub-Order ทั้งหมดที่อยู่ใน OrderGroup เดียวกัน
     * 
     * @param orderGroupId รหัส OrderGroup (Master Order)
     * @return รายการ Sub-Order
     */

    //[Fix] add throw Exception to Controller
    @Override
    public List<Order> getSubOrdersByOrderGroup(Long orderGroupId) {
        return orderRepository.findByOrderGroup_OrderGroupId(orderGroupId);
    }

    /**
     * ค้นหา Sub-Order ทั้งหมดของร้านค้า
     * 
     * @param sellerId รหัสร้านค้า
     * @return รายการ Sub-Order ของร้าน
     */
    @Override
    public List<Order> getSubOrdersBySeller(Long sellerId) {
            // [Fix] add throw Exception to Controller
        return orderRepository.findBySeller_SellerId(sellerId);
    }

    /**
     * ร้านค้ากดรับคำสั่งซื้อ
     * - ตรวจว่า Order เป็นของ Seller นี้
     * - สถานะต้องเป็น WAITING_SELLER_CONFIRM
     * - เปลี่ยนสถานะเป็น PREPARING
     *
     * @param sellerId รหัสร้านค้า
     * @param orderId  รหัส Sub-Order
     */
    @Override
    @Transactional
    public void sellerAcceptOrder(Long sellerId, Long orderId) {
        Order order = findAndValidateSellerOrder(sellerId, orderId);

        // ตรวจสถานะ — ต้อง WAITING_SELLER_CONFIRM
        if (order.getOrderStatus() != OrderStatus.WAITING_SELLER_CONFIRM) {
            throw new RuntimeException(
                    "ไม่สามารถยืนยันคำสั่งซื้อได้ สถานะปัจจุบัน: " + order.getOrderStatus()
                            + " (ต้องเป็น WAITING_SELLER_CONFIRM)");
        }

        // เปลี่ยนสถานะเป็น PREPARING
        order.setOrderStatus(OrderStatus.PREPARING);
        orderRepository.save(order);

        // แจ้งเตือนลูกค้า
        try {
            Long customerId = order.getOrderGroup().getCustomer().getCustomerId();
            notificationService.sendNotification(
                    order.getOrderGroup().getCustomer().getUser().getUserId(),
                    "ร้านค้ายืนยันคำสั่งซื้อ",
                    "คำสั่งซื้อ " + order.getSubOrderNumber() + " ได้รับการยืนยันจากร้านค้า กำลังเตรียมสินค้า",
                    project.project.Entity.notification.NotificationType.NEW_ORDER_FOR_SELLER);
        } catch (Exception e) {
            // ไม่ให้ Notification error กระทบ business logic หลัก
        }
    }

    /**
     * ร้านค้าปฏิเสธคำสั่งซื้อ (UC1-37A)
     *
     * Business Logic:
     * 1. ตรวจ ownership (Order ต้องเป็นของ Seller นี้)
     * 2. ตรวจสถานะ WAITING_SELLER_CONFIRM
     * 3. บันทึกเหตุผลการปฏิเสธ
     * 4. เปลี่ยนสถานะ Sub-Order เป็น CANCELLED
     * 5. คืนสต็อกสินค้าเฉพาะของร้านนี้
     * 6. Partial Refund — คืนเงินเฉพาะยอดของร้านนี้
     * 7. อัปเดตสถานะ OrderGroup เป็น PARTIALLY_REFUNDED
     * (ถ้ายังมีร้านอื่นที่ดำเนินอยู่)
     * 8. แจ้งเตือนลูกค้า
     *
     * ร้านอื่นๆ ใน OrderGroup เดียวกัน ยังคงดำเนินการต่อตามปกติ
     *
     * @param sellerId รหัสร้านค้า
     * @param orderId  รหัส Sub-Order
     * @param reason   เหตุผลที่ปฏิเสธ (เช่น สินค้าหมด, เสียหาย)
     */
    @Override
    @Transactional
    public void sellerRejectOrder(Long sellerId, Long orderId, String reason) {
        Order order = findAndValidateSellerOrder(sellerId, orderId);

        // ตรวจสถานะ
        if (order.getOrderStatus() != OrderStatus.WAITING_SELLER_CONFIRM) {
            throw new RuntimeException(
                    "ไม่สามารถปฏิเสธคำสั่งซื้อได้ สถานะปัจจุบัน: " + order.getOrderStatus()
                            + " (ต้องเป็น WAITING_SELLER_CONFIRM)");
        }

        // 1. บันทึกเหตุผลการปฏิเสธ
        order.setRejectionReason(reason);

        // 2. เปลี่ยนสถานะ Sub-Order เป็น CANCELLED
        order.setOrderStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        // 3. คืนสต็อกสินค้าเฉพาะของร้านนี้
        restoreStock(order);

        // 4. Partial Refund — คืนเงินเฉพาะยอดของร้านนี้ให้ลูกค้า
        try {
            paymentService.processPartialRefund(
                    orderId, order.getTotalAmount(),
                    "ร้านค้าปฏิเสธคำสั่งซื้อ: " + reason);
        } catch (Exception e) {
            // Log error — ในระบบจริงต้องมี retry mechanism
        }

        // 5. อัปเดตสถานะ OrderGroup
        updateOrderGroupPaymentStatus(order.getOrderGroup());

        // 6. แจ้งเตือนลูกค้า
        try {
            notificationService.sendNotification(
                    order.getOrderGroup().getCustomer().getUser().getUserId(),
                    "ร้านค้าปฏิเสธคำสั่งซื้อ",
                    "คำสั่งซื้อ " + order.getSubOrderNumber()
                            + " ถูกปฏิเสธโดยร้านค้า เหตุผล: " + reason
                            + " ยอดเงิน " + order.getTotalAmount() + " บาท จะถูกคืนให้คุณ",
                    project.project.Entity.notification.NotificationType.PAYMENT_SUCCESS);
        } catch (Exception e) {
            // ไม่ให้ Notification error กระทบ business logic หลัก
        }
    }

    /**
     * ลูกค้ากดยืนยันรับสินค้า
     * - ตรวจว่า Order เป็นของลูกค้าจริง (ผ่าน OrderGroup → Customer)
     * - สถานะต้อง SHIPPED
     * - เปลี่ยนเป็น COMPLETED + บันทึก completedAt
     *
     * @param customerId รหัสลูกค้า
     * @param orderId    รหัส Sub-Order
     */
    @Override
    @Transactional
    public void confirmOrderDelivered(Long customerId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException(
                        "ไม่พบคำสั่งซื้อ orderId: " + orderId));

        // ตรวจว่า Order เป็นของลูกค้าจริง
        Long orderCustomerId = order.getOrderGroup().getCustomer().getCustomerId();
        if (!orderCustomerId.equals(customerId)) {
            throw new RuntimeException(
                    "คำสั่งซื้อนี้ไม่ใช่ของลูกค้า customerId: " + customerId);
        }

        // ตรวจสถานะ — ต้อง SHIPPED
        if (order.getOrderStatus() != OrderStatus.SHIPPED) {
            throw new RuntimeException(
                    "ไม่สามารถยืนยันรับสินค้าได้ สถานะปัจจุบัน: " + order.getOrderStatus()
                            + " (ต้องเป็น SHIPPED)");
        }

        // เปลี่ยนสถานะเป็น COMPLETED
        completeOrder(order);
    }

    /**
     * Auto-confirm การรับสินค้าเมื่อครบ 7 วัน (UC1-45A)
     * ใช้โดย OrderSchedulerService (Scheduled Cron Job)
     *
     * @param orderId รหัส Sub-Order
     */
    @Override
    @Transactional
    public void autoConfirmDelivered(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException(
                        "ไม่พบคำสั่งซื้อ orderId: " + orderId));

        // ตรวจสถานะ — ต้อง SHIPPED
        if (order.getOrderStatus() != OrderStatus.SHIPPED) {
            return; // สถานะไม่ตรง ข้ามไป (อาจถูก manual confirm ไปแล้ว)
        }

        // เปลี่ยนสถานะเป็น COMPLETED
        completeOrder(order);
    }

    // ==================== Private Helper Methods ====================

    /**
     * ค้นหา Order และตรวจว่าเป็นของ Seller จริง
     */
    private Order findAndValidateSellerOrder(Long sellerId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException(
                        "ไม่พบคำสั่งซื้อ orderId: " + orderId));

        if (!order.getSeller().getSellerId().equals(sellerId)) {
            throw new RuntimeException(
                    "คำสั่งซื้อ " + orderId + " ไม่ใช่ของร้านค้า sellerId: " + sellerId);
        }

        return order;
    }

    /**
     * เปลี่ยนสถานะ Order เป็น COMPLETED พร้อมบันทึก completedAt
     * ใช้ร่วมกันระหว่าง confirmOrderDelivered และ autoConfirmDelivered
     */
    private void completeOrder(Order order) {
        order.setOrderStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    /**
     * คืนสต็อกสินค้าเมื่อร้านค้าปฏิเสธคำสั่งซื้อ (UC1-37A)
     * วนลูป OrderItem แต่ละรายการ เพิ่ม stock กลับเข้าสินค้า
     */
    private void restoreStock(Order order) {
        List<OrderItem> items = order.getOrderItems();
        if (items == null || items.isEmpty()) {
            return;
        }

        for (OrderItem item : items) {
            Product product = item.getProduct();
            if (product != null) {
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
            }
        }
    }

    /**
     * อัปเดตสถานะ OrderGroup หลังจากมี Sub-Order ถูก CANCELLED
     * - ถ้าทุก Sub-Order ถูก CANCELLED → REFUNDED
     * - ถ้ามีบาง Sub-Order ถูก CANCELLED → PARTIALLY_REFUNDED
     */
    private void updateOrderGroupPaymentStatus(OrderGroup orderGroup) {
        List<Order> allSubOrders = orderRepository.findByOrderGroup_OrderGroupId(
                orderGroup.getOrderGroupId());

        long cancelledCount = allSubOrders.stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.CANCELLED)
                .count();

        if (cancelledCount == allSubOrders.size()) {
            // ทุก Sub-Order ถูก CANCELLED → REFUNDED ทั้งหมด
            orderGroup.setPaymentStatus(OrderGroupPaymentStatus.REFUNDED);
        } else if (cancelledCount > 0) {
            // มีบาง Sub-Order ถูก CANCELLED → PARTIALLY_REFUNDED
            orderGroup.setPaymentStatus(OrderGroupPaymentStatus.PARTIALLY_REFUNDED);
        }
        // ถ้าไม่มี CANCELLED ไม่ต้องเปลี่ยน
    }
}
