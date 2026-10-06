package project.project.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project.project.Entity.order.Order;
import project.project.Entity.order.OrderGroup;
import project.project.Entity.order.OrderItem;
import project.project.Entity.order.OrderStatus;
import project.project.Entity.product.Product;
import project.project.Entity.review.Review;
import project.project.Entity.review.ReviewReply;
import project.project.Entity.seller.Seller;
import project.project.Entity.user.Customer;
import project.project.Entity.user.User;
import project.project.Exception.InvalidReviewException;
import project.project.Repository.CustomerRepository;
import project.project.Repository.OrderItemRepository;
import project.project.Repository.ProductRepository;
import project.project.Repository.ReviewReplyRepository;
import project.project.Repository.ReviewRepository;
import project.project.Repository.SellerRepository;
import project.project.Service.api.NotificationService;
import project.project.Service.api.ProductService;
import project.project.Service.implement.ReviewServiceImp;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewReplyRepository reviewReplyRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductService productService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ReviewServiceImp reviewService;

    private Customer sampleCustomer;
    private Seller sampleSeller;
    private Product sampleProduct;
    private Order sampleOrder;
    private OrderItem sampleOrderItem;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUserId(1L);

        sampleCustomer = new Customer();
        sampleCustomer.setCustomerId(10L);
        sampleCustomer.setUser(user);

        User sellerUser = new User();
        sellerUser.setUserId(2L);
        sampleSeller = new Seller();
        sampleSeller.setSellerId(20L);
        sampleSeller.setUser(sellerUser);

        sampleProduct = new Product();
        sampleProduct.setProductId(100L);
        sampleProduct.setName("สินค้าทดสอบ");
        sampleProduct.setSeller(sampleSeller);
        sampleProduct.setAverageRating(BigDecimal.ZERO);
        sampleProduct.setReviewCount(0);

        OrderGroup orderGroup = new OrderGroup();
        orderGroup.setCustomer(sampleCustomer);

        sampleOrder = new Order();
        sampleOrder.setOrderId(300L);
        sampleOrder.setOrderGroup(orderGroup);
        sampleOrder.setOrderStatus(OrderStatus.COMPLETED);

        sampleOrderItem = new OrderItem();
        sampleOrderItem.setOrderItemId(500L);
        sampleOrderItem.setOrder(sampleOrder);
        sampleOrderItem.setProduct(sampleProduct);
        sampleOrderItem.setIsReviewed(false);
    }

    @Test
    @DisplayName("UC5: ตรวจสอบสิทธิ์การรีวิว (Verified Purchase) สำเร็จเมื่อ Order มีสถานะ COMPLETED")
    void testIsEligibleToReview_Success() {
        when(orderItemRepository.findById(500L)).thenReturn(Optional.of(sampleOrderItem));
        when(reviewRepository.existsByOrderItem_OrderItemId(500L)).thenReturn(false);

        boolean eligible = reviewService.isEligibleToReview(10L, 500L);

        assertTrue(eligible);
    }

    @Test
    @DisplayName("UC5: ตรวจสอบสิทธิ์การรีวิว ไม่ผ่านหาก Sub-Order ยังไม่ COMPLETED")
    void testIsEligibleToReview_OrderNotCompleted_ReturnsFalse() {
        sampleOrder.setOrderStatus(OrderStatus.SHIPPED);
        when(orderItemRepository.findById(500L)).thenReturn(Optional.of(sampleOrderItem));
        when(reviewRepository.existsByOrderItem_OrderItemId(500L)).thenReturn(false);

        boolean eligible = reviewService.isEligibleToReview(10L, 500L);

        assertFalse(eligible);
    }

    @Test
    @DisplayName("UC5: ตรวจสอบสิทธิ์การรีวิว ไม่ผ่านหากสินค้านั้นเคยรีวิวไปแล้ว")
    void testIsEligibleToReview_AlreadyReviewed_ReturnsFalse() {
        sampleOrderItem.setIsReviewed(true);
        when(orderItemRepository.findById(500L)).thenReturn(Optional.of(sampleOrderItem));

        boolean eligible = reviewService.isEligibleToReview(10L, 500L);

        assertFalse(eligible);
    }

    @Test
    @DisplayName("UC5: ลูกค้าสร้างรีวิวสำเร็จ บันทึก Review, ทำเครื่องหมายว่ารีวิวแล้ว, อัปเดต Rating สินค้า และแจ้งเตือนร้านค้า")
    void testCreateReview_Success() {
        when(orderItemRepository.findById(500L)).thenReturn(Optional.of(sampleOrderItem));
        when(customerRepository.findById(10L)).thenReturn(Optional.of(sampleCustomer));
        when(reviewRepository.existsByOrderItem_OrderItemId(500L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review r = invocation.getArgument(0);
            r.setReviewId(900L);
            return r;
        });

        Review result = reviewService.createReview(10L, 500L, 5, "สินค้าดีมาก จัดส่งไว");

        assertNotNull(result);
        assertEquals(900L, result.getReviewId());
        assertEquals(5, result.getRating());
        assertEquals("สินค้าดีมาก จัดส่งไว", result.getComment());
        assertFalse(result.getIsAutoReview());

        assertTrue(sampleOrderItem.getIsReviewed());
        verify(orderItemRepository, times(1)).save(sampleOrderItem);
        verify(productService, times(1)).updateAverageRating(100L, 5);
        verify(notificationService, times(1)).notifySellerNewReview(20L, 900L);
    }

    @Test
    @DisplayName("UC5: สร้างรีวิวด้วยคะแนนเกิน 5 หรือต่ำกว่า 1 ต้องโยน IllegalArgumentException")
    void testCreateReview_InvalidRating_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            reviewService.createReview(10L, 500L, 6, "คะแนนเกิน");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            reviewService.createReview(10L, 500L, 0, "คะแนนต่ำกว่า 1");
        });

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("UC5: ลูกค้าที่ไม่ได้สั่งซื้อหรือสถานะไม่สมบูรณ์ สร้างรีวิวต้องโยน InvalidReviewException")
    void testCreateReview_NotEligible_ThrowsInvalidReviewException() {
        sampleOrder.setOrderStatus(OrderStatus.WAITING_SELLER_CONFIRM);
        when(orderItemRepository.findById(500L)).thenReturn(Optional.of(sampleOrderItem));

        assertThrows(InvalidReviewException.class, () -> {
            reviewService.createReview(10L, 500L, 4, "รีวิว");
        });

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("UC5-6A: ระบบสร้างรีวิว 5 ดาวอัตโนมัติ (generateAutoFiveStarReview)")
    void testGenerateAutoFiveStarReview_Success() {
        when(orderItemRepository.findById(500L)).thenReturn(Optional.of(sampleOrderItem));
        when(reviewRepository.existsByOrderItem_OrderItemId(500L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review r = invocation.getArgument(0);
            r.setReviewId(901L);
            return r;
        });

        reviewService.generateAutoFiveStarReview(500L);

        assertTrue(sampleOrderItem.getIsReviewed());
        verify(reviewRepository, times(1)).save(any(Review.class));
        verify(productService, times(1)).updateAverageRating(100L, 5);
        verify(notificationService, times(1)).notifySellerNewReview(20L, 901L);
    }

    @Test
    @DisplayName("UC5-23A: ร้านค้าตอบกลับรีวิว (replyReview) สำเร็จ")
    void testReplyReview_Success() {
        Review review = new Review();
        review.setReviewId(900L);
        review.setProduct(sampleProduct);
        review.setCustomer(sampleCustomer);

        when(reviewRepository.findById(900L)).thenReturn(Optional.of(review));
        when(sellerRepository.findById(20L)).thenReturn(Optional.of(sampleSeller));
        when(reviewReplyRepository.findByReview_ReviewId(900L)).thenReturn(Optional.empty());
        when(reviewReplyRepository.save(any(ReviewReply.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewReply reply = reviewService.replyReview(20L, 900L, "ขอบพระคุณสำหรับรีวิวครับ");

        assertNotNull(reply);
        assertEquals("ขอบพระคุณสำหรับรีวิวครับ", reply.getReplyMessage());
        assertEquals(sampleSeller, reply.getSeller());
        verify(reviewReplyRepository, times(1)).save(any(ReviewReply.class));
    }
}
