package project.project.Service.api;

import project.project.Entity.review.Review;
import project.project.Entity.review.ReviewReply;
import java.util.List;

public interface ReviewService {
    Review createReview(Long customerId, Long orderItemId, Integer rating, String comment);
    void generateAutoFiveStarReview(Long orderItemId);
    Review updateReview(Long customerId, Long reviewId, Integer rating, String comment);
    ReviewReply replyReview(Long sellerId, Long reviewId, String replyMessage);
    boolean isEligibleToReview(Long customerId, Long orderItemId);
    List<Review> getReviewsByProduct(Long productId);
    Review getReviewByOrderItemId(Long orderItemId);
}
