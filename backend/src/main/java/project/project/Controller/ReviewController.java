package project.project.Controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.project.ApiResponse.ApiResponse;
import project.project.DTO.review.CreateReviewRequest;
import project.project.DTO.review.ReplyReviewRequest;
import project.project.DTO.review.ReviewResponse;
import project.project.DTO.review.UpdateReviewRequest;
import project.project.Entity.review.Review;
import project.project.Entity.review.ReviewReply;
import project.project.Service.api.ReviewService;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @RequestParam Long customerId,
            @Valid @RequestBody CreateReviewRequest request) {
        Review review = reviewService.createReview(
                customerId,
                request.getOrderItemId(),
                request.getRating(),
                request.getComment()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("สร้างรีวิวสินค้าสำเร็จ", ReviewResponse.fromEntity(review)));
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable Long reviewId,
            @RequestParam Long customerId,
            @Valid @RequestBody UpdateReviewRequest request) {
        Review updated = reviewService.updateReview(
                customerId,
                reviewId,
                request.getRating(),
                request.getComment()
        );
        return ResponseEntity.ok(ApiResponse.success("แก้ไขรีวิวสำเร็จ", ReviewResponse.fromEntity(updated)));
    }

    @PostMapping("/{reviewId}/reply")
    public ResponseEntity<ApiResponse<Void>> replyReview(
            @PathVariable Long reviewId,
            @RequestParam Long sellerId,
            @Valid @RequestBody ReplyReviewRequest request) {
        reviewService.replyReview(sellerId, reviewId, request.getReplyMessage());
        return ResponseEntity.ok(ApiResponse.success("ตอบกลับรีวิวสำเร็จ", null));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getReviewsByProduct(@PathVariable Long productId) {
        List<Review> reviews = reviewService.getReviewsByProduct(productId);
        List<ReviewResponse> responses = reviews.stream()
                .map(ReviewResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("ดึงรายการรีวิวของสินค้าสำเร็จ", responses));
    }

    @GetMapping("/order-item/{orderItemId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> getReviewByOrderItemId(@PathVariable Long orderItemId) {
        Review review = reviewService.getReviewByOrderItemId(orderItemId);
        return ResponseEntity.ok(ApiResponse.success("ดึงข้อมูลรีวิวของรายการสั่งซื้อสำเร็จ", ReviewResponse.fromEntity(review)));
    }

    @GetMapping("/check-eligibility")
    public ResponseEntity<ApiResponse<Boolean>> checkEligibility(
            @RequestParam Long customerId,
            @RequestParam Long orderItemId) {
        boolean eligible = reviewService.isEligibleToReview(customerId, orderItemId);
        return ResponseEntity.ok(ApiResponse.success("ตรวจสอบสิทธิ์การรีวิวสำเร็จ", eligible));
    }
}
