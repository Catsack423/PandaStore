package project.project.Service.implement;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import project.project.Entity.order.Order;
import project.project.Entity.order.OrderStatus;
import project.project.Repository.OrderRepository;
import project.project.Service.api.SubOrderService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Background / Scheduled Tasks for Order Lifecycle Management (UC1-45A & Auto Cancellation)
 */
@Component
public class OrderSchedulerService {

    private final SubOrderService subOrderService;
    private final OrderRepository orderRepository;

    public OrderSchedulerService(SubOrderService subOrderService, OrderRepository orderRepository) {
        this.subOrderService = subOrderService;
        this.orderRepository = orderRepository;
    }

    /**
     * UC1 - Alternative Flow 45A:
     * "ลูกค้าไม่กดยืนยันรับสินค้า หลัง 7 วันระบบจะกดยืนยันอัตโนมัติ"
     * ตรวจหา Sub-Order แต่ละร้านที่มีสถานะ SHIPPED เกิน 7 วัน แล้วเปลี่ยนเป็น COMPLETED
     */
    @Scheduled(cron = "0 0 * * * *") // รันทุกชั่วโมง
    public void processAutoConfirmSubOrders() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Order> expiredOrders = orderRepository.findByOrderStatusAndShippedAtBefore(
                OrderStatus.SHIPPED, sevenDaysAgo);

        if (expiredOrders != null && !expiredOrders.isEmpty()) {
            for (Order order : expiredOrders) {
                try {
                    subOrderService.autoConfirmDelivered(order.getOrderId());
                } catch (Exception e) {
                    // Log error and continue to next order
                }
            }
        }
    }

    /**
     * Auto-Cancel Sub-Orders:
     * หากร้านค้าไม่กดยืนยันคำสั่งซื้อภายใน 24 ชั่วโมง ระบบจะยกเลิกคำสั่งซื้ออัตโนมัติ
     * พร้อมคืนเงิน (Partial Refund) และคืนสต็อก
     */
    @Scheduled(cron = "0 30 * * * *") // รันทุกชั่วโมงที่นาทีที่ 30
    public void processAutoCancelUnconfirmedOrders() {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        List<Order> unconfirmedOrders = orderRepository.findByOrderStatusAndShippedAtBefore(
                OrderStatus.WAITING_SELLER_CONFIRM, oneDayAgo);

        if (unconfirmedOrders != null && !unconfirmedOrders.isEmpty()) {
            for (Order order : unconfirmedOrders) {
                try {
                    subOrderService.sellerRejectOrder(
                            order.getSeller().getSellerId(),
                            order.getOrderId(),
                            "ระบบยกเลิกคำสั่งซื้ออัตโนมัติเนื่องจากร้านค้าไม่ยืนยันภายใน 24 ชั่วโมง");
                } catch (Exception e) {
                    // Log error and continue to next order
                }
            }
        }
    }
}
