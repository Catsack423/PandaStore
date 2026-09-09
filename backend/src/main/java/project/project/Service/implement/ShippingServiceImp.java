package project.project.Service.implement;

import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import project.project.Entity.order.Order;
import project.project.Entity.order.OrderStatus;
import project.project.Entity.order.Shipment;
import project.project.Entity.order.ShippingStatus;
import project.project.Repository.OrderRepository;
import project.project.Repository.ShipmentRepository;
import project.project.Service.api.NotificationService;
import project.project.Service.api.ShippingService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ShippingServiceImp implements ShippingService {

    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;
    private final NotificationService notificationService;

    /**
     * Shipping fee rates ต่อ courier (Strategy Pattern — Map-based)
     * เพิ่ม courier ใหม่ได้โดยไม่ต้องแก้ if-else (OCP)
     */
    //make this to stategy pattern
    private static final Map<String, BigDecimal> SHIPPING_RATES = Map.of(
            "KERRY", new BigDecimal("50.00"),
            "FLASH", new BigDecimal("40.00"),
            "STANDARD", new BigDecimal("30.00"),
            "EMS", new BigDecimal("60.00"),
            "THAILANDPOST", new BigDecimal("35.00"),
            "J&T", new BigDecimal("45.00"));

    private static final BigDecimal DEFAULT_SHIPPING_FEE = new BigDecimal("50.00");

    // Constructor Injection (SOLID - Dependency Inversion Principle)
    public ShippingServiceImp(OrderRepository orderRepository,
            ShipmentRepository shipmentRepository,
            NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.shipmentRepository = shipmentRepository;
        this.notificationService = notificationService;
    }

    /**
     * คำนวณค่าจัดส่งสำหรับร้านค้า โดยใช้ shipping method ที่ลูกค้าเลือก
     * (UC1 Step 8: ลูกค้าเลือกวิธีจัดส่งแยกตามร้าน)
     *
     * @param sellerId       รหัสร้านค้า
     * @param shippingMethod วิธีจัดส่ง เช่น "KERRY", "FLASH", "STANDARD"
     * @param addressId      รหัสที่อยู่จัดส่ง (สำหรับคำนวณระยะทางในอนาคต)
     * @return ค่าจัดส่ง (BigDecimal)
     */
    @Override
    public BigDecimal calculateShippingFee(Long sellerId, String shippingMethod, Long addressId) {
        if (shippingMethod == null || shippingMethod.isBlank()) {
            return DEFAULT_SHIPPING_FEE;
        }

        // Strategy Pattern: ดึงค่าส่งจาก Map ตาม shippingMethod
        String methodKey = shippingMethod.toUpperCase().trim();
        return SHIPPING_RATES.getOrDefault(methodKey, DEFAULT_SHIPPING_FEE);
    }

    /**
     * ร้านค้ากรอก Tracking Number (UC1 Step 41 & 41A)
     * - ตรวจว่า Order เป็นของร้านค้านี้
     * - ตรวจว่าสถานะ Order คือ PREPARING (ร้านค้ากดรับแล้ว)
     * - สร้าง/อัปเดต Shipment พร้อม courierName และ trackingNumber
     * - เปลี่ยนสถานะ Order เป็น SHIPPED พร้อมบันทึก shippedAt
     * - แจ้งเตือนลูกค้าว่าพัสดุถูกส่งแล้ว
     *
     * @param sellerId       รหัสร้านค้า
     * @param orderId        รหัส Sub-Order
     * @param courierName    ชื่อบริษัทขนส่ง
     * @param trackingNumber หมายเลข Tracking
     * @return Shipment ที่สร้าง/อัปเดตแล้ว
     */
    @Override
    @Transactional
    public Shipment assignTrackingNumber(Long sellerId, Long orderId,
            String courierName, String trackingNumber) {
        // 1. ค้นหา Order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException(
                        "ไม่พบคำสั่งซื้อ orderId: " + orderId));

        // 2. ตรวจว่า Order เป็นของ Seller นี้
        if (!order.getSeller().getSellerId().equals(sellerId)) {
            throw new RuntimeException(
                    "คำสั่งซื้อนี้ไม่ใช่ของร้านค้า sellerId: " + sellerId);
        }

        // 3. ตรวจสถานะ — ต้อง PREPARING เท่านั้นจึงจะกรอก Tracking ได้
        if (order.getOrderStatus() != OrderStatus.PREPARING) {
            throw new RuntimeException(
                    "ไม่สามารถกรอก Tracking ได้ สถานะปัจจุบัน: " + order.getOrderStatus()
                            + " (ต้องเป็น PREPARING)");
        }

        // 4. ตรวจว่า courierName และ trackingNumber ไม่ว่าง (UC1-41A)
        if (courierName == null || courierName.isBlank()) {
            throw new RuntimeException("กรุณาระบุชื่อบริษัทขนส่ง (courierName)");
        }
        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new RuntimeException("กรุณาระบุหมายเลข Tracking (trackingNumber)");
        }

        // 5. สร้างหรืออัปเดต Shipment
        Shipment shipment = shipmentRepository.findByOrder_OrderId(orderId)
                .orElse(new Shipment());

        shipment.setOrder(order);
        shipment.setCourierName(courierName);
        shipment.setTrackingNumber(trackingNumber);
        shipment.setShippingStatus(ShippingStatus.SHIPPED);
        shipment.setShippedAt(LocalDateTime.now());

        Shipment savedShipment = shipmentRepository.save(shipment);

        // 6. อัปเดตสถานะ Order เป็น SHIPPED
        order.setOrderStatus(OrderStatus.SHIPPED);
        order.setShippedAt(LocalDateTime.now());
        orderRepository.save(order);

        // 7. แจ้งเตือนลูกค้าว่าพัสดุถูกส่งแล้ว (พร้อม Tracking Number)
        try {
            Long customerId = order.getOrderGroup().getCustomer().getCustomerId();
            notificationService.notifyCustomerOrderShipped(
                    customerId, orderId, trackingNumber);
        } catch (Exception e) {
            // ไม่ให้ Notification error กระทบ business logic หลัก
            // Log error ในระบบจริง
        }

        return savedShipment;
    }

    /**
     * ค้นหา Shipment จาก orderId
     * 
     * @param orderId รหัส Sub-Order
     * @return Shipment entity
     * @throws RuntimeException ถ้าไม่พบ Shipment
     */
    @Override
    public Shipment getShipmentByOrderId(Long orderId) {
        return shipmentRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new RuntimeException(
                        "ไม่พบข้อมูลการจัดส่งสำหรับ orderId: " + orderId));
    }

    /**
     * อัปเดตสถานะ Shipment (เช่น SHIPPED → DELIVERED)
     * 
     * @param shipmentId รหัส Shipment
     * @param status     สถานะใหม่ ("SHIPPED" / "DELIVERED")
     */
    @Override
    @Transactional
    public void updateShippingStatus(Long shipmentId, String status) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException(
                        "ไม่พบ Shipment shipmentId: " + shipmentId));

        ShippingStatus newStatus;
        try {
            newStatus = ShippingStatus.valueOf(status.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                    "สถานะไม่ถูกต้อง: " + status
                            + " (ต้องเป็น PENDING, SHIPPED, หรือ DELIVERED)");
        }

        shipment.setShippingStatus(newStatus);

        // บันทึก timestamp ตามสถานะ
        if (newStatus == ShippingStatus.SHIPPED && shipment.getShippedAt() == null) {
            shipment.setShippedAt(LocalDateTime.now());
        } else if (newStatus == ShippingStatus.DELIVERED) {
            shipment.setDeliveredAt(LocalDateTime.now());
        }

        shipmentRepository.save(shipment);
    }
}
