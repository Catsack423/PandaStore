package project.project.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project.project.Entity.order.Order;
import project.project.Entity.order.OrderGroup;
import project.project.Entity.order.OrderStatus;
import project.project.Entity.order.Shipment;
import project.project.Entity.order.ShippingStatus;
import project.project.Entity.seller.Seller;
import project.project.Entity.user.Customer;
import project.project.Entity.user.User;
import project.project.Repository.OrderRepository;
import project.project.Repository.ShipmentRepository;
import project.project.Service.api.NotificationService;
import project.project.Service.implement.ShippingServiceImp;
import project.project.Service.strategy.shipping.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ShippingServiceTest {

    private ShippingServiceImp shippingService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private NotificationService notificationService;

    private ShippingFeeStrategyFactory strategyFactory;

    @BeforeEach
    void setUp() {
        DefaultShippingStrategy defaultStrategy = new DefaultShippingStrategy();
        List<ShippingFeeStrategy> strategies = List.of(
                new KerryShippingStrategy(),
                new FlashShippingStrategy(),
                new StandardShippingStrategy(),
                new EmsShippingStrategy(),
                new ThailandPostShippingStrategy(),
                new JAndTShippingStrategy(),
                defaultStrategy
        );
        strategyFactory = new ShippingFeeStrategyFactory(strategies, defaultStrategy);

        shippingService = new ShippingServiceImp(
                orderRepository,
                shipmentRepository,
                notificationService,
                strategyFactory
        );
    }

    // ==================== Strategy Pattern Tests ====================

    @Test
    @DisplayName("Strategy Pattern: คำนวณค่าส่ง Kerry = 50.00")
    void calculateShippingFee_Kerry() {
        BigDecimal fee = shippingService.calculateShippingFee(1L, "KERRY", 1L);
        assertEquals(new BigDecimal("50.00"), fee);
    }

    @Test
    @DisplayName("Strategy Pattern: คำนวณค่าส่ง Flash = 40.00")
    void calculateShippingFee_Flash() {
        BigDecimal fee = shippingService.calculateShippingFee(1L, "flash", 1L);
        assertEquals(new BigDecimal("40.00"), fee);
    }

    @Test
    @DisplayName("Strategy Pattern: คำนวณค่าส่ง Standard = 30.00")
    void calculateShippingFee_Standard() {
        BigDecimal fee = shippingService.calculateShippingFee(1L, "STANDARD", 1L);
        assertEquals(new BigDecimal("30.00"), fee);
    }

    @Test
    @DisplayName("Strategy Pattern: คำนวณค่าส่ง EMS = 60.00")
    void calculateShippingFee_Ems() {
        BigDecimal fee = shippingService.calculateShippingFee(1L, "EMS", 1L);
        assertEquals(new BigDecimal("60.00"), fee);
    }

    @Test
    @DisplayName("Strategy Pattern: คำนวณค่าส่ง ThailandPost = 35.00")
    void calculateShippingFee_ThailandPost() {
        BigDecimal fee = shippingService.calculateShippingFee(1L, "THAILANDPOST", 1L);
        assertEquals(new BigDecimal("35.00"), fee);
    }

    @Test
    @DisplayName("Strategy Pattern: คำนวณค่าส่ง J&T = 45.00")
    void calculateShippingFee_JAndT() {
        BigDecimal fee = shippingService.calculateShippingFee(1L, "J&T", 1L);
        assertEquals(new BigDecimal("45.00"), fee);

        BigDecimal feeJt = shippingService.calculateShippingFee(1L, "JT", 1L);
        assertEquals(new BigDecimal("45.00"), feeJt);
    }

    @Test
    @DisplayName("Strategy Pattern: กรณีไม่ระบุหรือระบุขนส่งที่ไม่รู้จัก ต้องใช้ Default = 50.00")
    void calculateShippingFee_DefaultFallback() {
        assertEquals(new BigDecimal("50.00"), shippingService.calculateShippingFee(1L, null, 1L));
        assertEquals(new BigDecimal("50.00"), shippingService.calculateShippingFee(1L, "", 1L));
        assertEquals(new BigDecimal("50.00"), shippingService.calculateShippingFee(1L, "UNKNOWN_COURIER", 1L));
    }

    // ==================== assignTrackingNumber Tests ====================

    @Test
    @DisplayName("assignTrackingNumber - สำเร็จ: อัปเดต Shipment, เปลี่ยนสถานะ Order เป็น SHIPPED, และแจ้งเตือนลูกค้า")
    void assignTrackingNumber_Success() {
        Long sellerId = 1L;
        Long orderId = 100L;
        Long customerId = 50L;

        Seller seller = new Seller();
        seller.setSellerId(sellerId);

        Customer customer = new Customer();
        customer.setCustomerId(customerId);
        User user = new User();
        user.setUserId(200L);
        customer.setUser(user);

        OrderGroup orderGroup = new OrderGroup();
        orderGroup.setCustomer(customer);

        Order order = new Order();
        order.setOrderId(orderId);
        order.setSeller(seller);
        order.setOrderStatus(OrderStatus.PREPARING);
        order.setOrderGroup(orderGroup);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(shipmentRepository.findByOrder_OrderId(orderId)).thenReturn(Optional.empty());
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

        Shipment result = shippingService.assignTrackingNumber(sellerId, orderId, "Kerry Express", "KRY12345678");

        assertNotNull(result);
        assertEquals("Kerry Express", result.getCourierName());
        assertEquals("KRY12345678", result.getTrackingNumber());
        assertEquals(ShippingStatus.SHIPPED, result.getShippingStatus());
        assertNotNull(result.getShippedAt());

        assertEquals(OrderStatus.SHIPPED, order.getOrderStatus());
        assertNotNull(order.getShippedAt());

        verify(shipmentRepository, times(1)).save(any(Shipment.class));
        verify(orderRepository, times(1)).save(order);
        verify(notificationService, times(1)).notifyCustomerOrderShipped(customerId, orderId, "KRY12345678");
    }

    @Test
    @DisplayName("assignTrackingNumber - โยน Exception เมื่อร้านค้าไม่ใช่เจ้าของ Order")
    void assignTrackingNumber_WrongSeller_ThrowsException() {
        Long sellerId = 1L;
        Long otherSellerId = 2L;
        Long orderId = 100L;

        Seller otherSeller = new Seller();
        otherSeller.setSellerId(otherSellerId);

        Order order = new Order();
        order.setOrderId(orderId);
        order.setSeller(otherSeller);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            shippingService.assignTrackingNumber(sellerId, orderId, "Kerry", "KRY123");
        });

        assertTrue(ex.getMessage().contains("คำสั่งซื้อนี้ไม่ใช่ของร้านค้า"));
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("assignTrackingNumber - โยน Exception เมื่อ Order ไม่ได้อยู่ในสถานะ PREPARING")
    void assignTrackingNumber_InvalidStatus_ThrowsException() {
        Long sellerId = 1L;
        Long orderId = 100L;

        Seller seller = new Seller();
        seller.setSellerId(sellerId);

        Order order = new Order();
        order.setOrderId(orderId);
        order.setSeller(seller);
        order.setOrderStatus(OrderStatus.WAITING_SELLER_CONFIRM);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            shippingService.assignTrackingNumber(sellerId, orderId, "Kerry", "KRY123");
        });

        assertTrue(ex.getMessage().contains("ต้องเป็น PREPARING"));
    }

    @Test
    @DisplayName("assignTrackingNumber - โยน Exception เมื่อ courierName หรือ trackingNumber ว่าง")
    void assignTrackingNumber_EmptyCourierOrTracking_ThrowsException() {
        Long sellerId = 1L;
        Long orderId = 100L;

        Seller seller = new Seller();
        seller.setSellerId(sellerId);

        Order order = new Order();
        order.setOrderId(orderId);
        order.setSeller(seller);
        order.setOrderStatus(OrderStatus.PREPARING);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(RuntimeException.class, () -> {
            shippingService.assignTrackingNumber(sellerId, orderId, "", "KRY123");
        });

        assertThrows(RuntimeException.class, () -> {
            shippingService.assignTrackingNumber(sellerId, orderId, "Kerry", null);
        });
    }

    // ==================== getShipmentByOrderId Tests ====================

    @Test
    @DisplayName("getShipmentByOrderId - สำเร็จเมื่อพบ Shipment")
    void getShipmentByOrderId_Success() {
        Long orderId = 100L;
        Shipment shipment = new Shipment();
        shipment.setShipmentId(1L);
        shipment.setTrackingNumber("TH123");

        when(shipmentRepository.findByOrder_OrderId(orderId)).thenReturn(Optional.of(shipment));

        Shipment result = shippingService.getShipmentByOrderId(orderId);

        assertNotNull(result);
        assertEquals("TH123", result.getTrackingNumber());
    }

    @Test
    @DisplayName("getShipmentByOrderId - โยน Exception เมื่อไม่พบ Shipment")
    void getShipmentByOrderId_NotFound_ThrowsException() {
        Long orderId = 999L;
        when(shipmentRepository.findByOrder_OrderId(orderId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            shippingService.getShipmentByOrderId(orderId);
        });
    }

    // ==================== updateShippingStatus Tests ====================

    @Test
    @DisplayName("updateShippingStatus - เปลี่ยนเป็น DELIVERED พร้อมบันทึก deliveredAt")
    void updateShippingStatus_Delivered() {
        Long shipmentId = 1L;
        Shipment shipment = new Shipment();
        shipment.setShipmentId(shipmentId);
        shipment.setShippingStatus(ShippingStatus.SHIPPED);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

        shippingService.updateShippingStatus(shipmentId, "DELIVERED");

        assertEquals(ShippingStatus.DELIVERED, shipment.getShippingStatus());
        assertNotNull(shipment.getDeliveredAt());
        verify(shipmentRepository, times(1)).save(shipment);
    }

    @Test
    @DisplayName("updateShippingStatus - โยน Exception เมื่อสถานะไม่ถูกต้อง")
    void updateShippingStatus_InvalidStatus_ThrowsException() {
        Long shipmentId = 1L;
        Shipment shipment = new Shipment();
        shipment.setShipmentId(shipmentId);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        assertThrows(RuntimeException.class, () -> {
            shippingService.updateShippingStatus(shipmentId, "INVALID_STATUS_XYZ");
        });
    }
}
