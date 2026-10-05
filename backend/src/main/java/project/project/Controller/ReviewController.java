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
import project.project.Service.api.ReviewService;
import project.project.Security.CurrentUser;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final CurrentUser currentUser;
    private final project.project.Security.OrderAccess access;

    public ReviewController(ReviewService reviewService, CurrentUser currentUser, project.project.Security.OrderAccess access) {
        this.reviewService = reviewService;
        this.currentUser = currentUser;
        this.access = access;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody CreateReviewRequest request) {
        Long customerId = currentUser.requireCustomerId();
        Review review = reviewService.createReview(
                customerId,
                request.getOrderItemId(),
                request.getRating(),
                request.getComment()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product review created successfully", ReviewResponse.fromEntity(review)));
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewRequest request) {
        Long customerId = currentUser.requireCustomerId();
        Review updated = reviewService.updateReview(
                customerId,
                reviewId,
                request.getRating(),
                request.getComment()
        );
        return ResponseEntity.ok(ApiResponse.success("Review updated successfully", ReviewResponse.fromEntity(updated)));
    }

    @PostMapping("/{reviewId}/reply")
    public ResponseEntity<ApiResponse<Void>> replyReview(
            @PathVariable Long reviewId,
            @RequestParam Long sellerId,
            @Valid @RequestBody ReplyReviewRequest request) {
        access.requireSeller(sellerId);
        reviewService.replyReview(sellerId, reviewId, request.getReplyMessage());
        return ResponseEntity.ok(ApiResponse.success("Review reply submitted successfully", null));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getReviewsByProduct(@PathVariable Long productId) {
        List<Review> reviews = reviewService.getReviewsByProduct(productId);
        List<ReviewResponse> responses = reviews.stream()
                .map(ReviewResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Product reviews retrieved successfully", responses));
    }

    @GetMapping("/order-item/{orderItemId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> getReviewByOrderItemId(@PathVariable Long orderItemId) {
        Review review = reviewService.getReviewByOrderItemId(orderItemId);
        return ResponseEntity.ok(ApiResponse.success("Order item review retrieved successfully", ReviewResponse.fromEntity(review)));
    }

    @GetMapping("/check-eligibility")
    public ResponseEntity<ApiResponse<Boolean>> checkEligibility(
            @RequestParam Long orderItemId) {
        Long customerId = currentUser.requireCustomerId();
        boolean eligible = reviewService.isEligibleToReview(customerId, orderItemId);
        return ResponseEntity.ok(ApiResponse.success("Review eligibility checked successfully", eligible));
    }
}
