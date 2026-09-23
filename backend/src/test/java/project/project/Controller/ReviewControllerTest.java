package project.project.Controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import project.project.Entity.order.OrderItem;
import project.project.Entity.product.Product;
import project.project.Entity.review.Review;
import project.project.Entity.review.ReviewReply;
import project.project.Entity.user.Customer;
import project.project.Service.api.ReviewService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class ReviewControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController reviewController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reviewController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/reviews - ลูกค้ารีวิวสินค้าสำเร็จ (Rating 5 ดาว)")
    void createReview_Success() throws Exception {
        Long customerId = 1L;
        String requestJson = """
                {
                    "orderItemId": 10,
                    "rating": 5,
                    "comment": "สินค้าคุณภาพดีมาก คุ้มราคา จัดส่งรวดเร็ว"
                }
                """;

        Review review = new Review();
        review.setReviewId(201L);
        review.setRating(5);
        review.setComment("สินค้าคุณภาพดีมาก คุ้มราคา จัดส่งรวดเร็ว");
        review.setIsAutoReview(false);
        review.setCreatedAt(LocalDateTime.now());

        Customer customer = new Customer();
        customer.setCustomerId(customerId);
        customer.setFullName("John Doe");
        review.setCustomer(customer);

        Product product = new Product();
        product.setProductId(100L);
        review.setProduct(product);

        OrderItem item = new OrderItem();
        item.setOrderItemId(10L);
        review.setOrderItem(item);

        when(reviewService.createReview(eq(customerId), eq(10L), eq(5), any())).thenReturn(review);

        mockMvc.perform(post("/api/reviews")
                        .param("customerId", String.valueOf(customerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reviewId").value(201L))
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.comment").value("สินค้าคุณภาพดีมาก คุ้มราคา จัดส่งรวดเร็ว"));
    }

    @Test
    @DisplayName("POST /api/reviews - ลูกค้ารีวิวกรณีประเภท { review: อื่นๆ } / ข้อเสนอแนะอื่นๆ")
    void createReview_OtherCategory_Success() throws Exception {
        Long customerId = 1L;
        String requestJson = """
                {
                    "orderItemId": 11,
                    "rating": 4,
                    "comment": "อื่นๆ: สินค้าใช้งานได้ดี แต่สีเพี้ยนจากในรูปเล็กน้อย บรรจุภัณฑ์แข็งแรงดี"
                }
                """;

        Review review = new Review();
        review.setReviewId(202L);
        review.setRating(4);
        review.setComment("อื่นๆ: สินค้าใช้งานได้ดี แต่สีเพี้ยนจากในรูปเล็กน้อย บรรจุภัณฑ์แข็งแรงดี");
        review.setIsAutoReview(false);

        Customer customer = new Customer();
        customer.setCustomerId(customerId);
        customer.setFullName("สมชาย ใจดี");
        review.setCustomer(customer);

        Product product = new Product();
        product.setProductId(101L);
        review.setProduct(product);

        OrderItem item = new OrderItem();
        item.setOrderItemId(11L);
        review.setOrderItem(item);

        when(reviewService.createReview(eq(customerId), eq(11L), eq(4), any())).thenReturn(review);

        mockMvc.perform(post("/api/reviews")
                        .param("customerId", String.valueOf(customerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reviewId").value(202L))
                .andExpect(jsonPath("$.data.comment").value("อื่นๆ: สินค้าใช้งานได้ดี แต่สีเพี้ยนจากในรูปเล็กน้อย บรรจุภัณฑ์แข็งแรงดี"));
    }

    @Test
    @DisplayName("POST /api/reviews - Validation Error เมื่อ Rating เกิน 5 หรือต่ำกว่า 1")
    void createReview_RatingValidationError() throws Exception {
        Long customerId = 1L;
        String invalidJson = """
                {
                    "orderItemId": 10,
                    "rating": 6,
                    "comment": "คะแนนเกินขอบเขต"
                }
                """;

        mockMvc.perform(post("/api/reviews")
                        .param("customerId", String.valueOf(customerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.rating").exists());
    }

    @Test
    @DisplayName("GET /api/reviews/check-eligibility - ตรวจสอบสิทธิ์การรีวิว")
    void checkEligibility_Success() throws Exception {
        Long customerId = 1L;
        Long orderItemId = 10L;

        when(reviewService.isEligibleToReview(customerId, orderItemId)).thenReturn(true);

        mockMvc.perform(get("/api/reviews/check-eligibility")
                        .param("customerId", String.valueOf(customerId))
                        .param("orderItemId", String.valueOf(orderItemId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("PUT /api/reviews/{reviewId} - ลูกค้าแก้ไขรีวิวสำเร็จ")
    void updateReview_Success() throws Exception {
        Long reviewId = 201L;
        Long customerId = 1L;
        String updateJson = """
                {
                    "rating": 5,
                    "comment": "อัปเดตรีวิว: ลองใช้แล้ว 1 เดือน ทนทานมาก แนะนำเลย"
                }
                """;

        Review updated = new Review();
        updated.setReviewId(reviewId);
        updated.setRating(5);
        updated.setComment("อัปเดตรีวิว: ลองใช้แล้ว 1 เดือน ทนทานมาก แนะนำเลย");

        when(reviewService.updateReview(eq(customerId), eq(reviewId), eq(5), any())).thenReturn(updated);

        mockMvc.perform(put("/api/reviews/{reviewId}", reviewId)
                        .param("customerId", String.valueOf(customerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.comment").value("อัปเดตรีวิว: ลองใช้แล้ว 1 เดือน ทนทานมาก แนะนำเลย"));
    }

    @Test
    @DisplayName("POST /api/reviews/{reviewId}/reply - ร้านค้าตอบกลับรีวิวสำเร็จ")
    void replyReview_Success() throws Exception {
        Long reviewId = 201L;
        Long sellerId = 5L;
        String replyJson = """
                {
                    "replyMessage": "ขอบพระคุณลูกค้าที่ไว้วางใจสั่งซื้อสินค้ากับ PandaStore ครับ"
                }
                """;

        ReviewReply reply = new ReviewReply();
        reply.setReplyMessage("ขอบพระคุณลูกค้าที่ไว้วางใจสั่งซื้อสินค้ากับ PandaStore ครับ");

        when(reviewService.replyReview(sellerId, reviewId, "ขอบพระคุณลูกค้าที่ไว้วางใจสั่งซื้อสินค้ากับ PandaStore ครับ"))
                .thenReturn(reply);

        mockMvc.perform(post("/api/reviews/{reviewId}/reply", reviewId)
                        .param("sellerId", String.valueOf(sellerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(replyJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/reviews/product/{productId} - ดึงรายการรีวิวตามสินค้า")
    void getReviewsByProduct_Success() throws Exception {
        Long productId = 100L;
        Review r = new Review();
        r.setReviewId(201L);
        r.setRating(5);
        r.setComment("ดีมาก");

        when(reviewService.getReviewsByProduct(productId)).thenReturn(List.of(r));

        mockMvc.perform(get("/api/reviews/product/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].reviewId").value(201L));
    }
}
