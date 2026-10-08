package project.project.DTO.review;

import project.project.Entity.review.Review;

import java.time.LocalDateTime;

public class ReviewResponse {

    private Long reviewId;
    private Long orderItemId;
    private Long productId;
    private Long customerId;
    private String customerName;
    private Integer rating;
    private String comment;
    private Boolean isAutoReview;
    private String replyMessage;
    private LocalDateTime repliedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ReviewResponse() {
    }

    public ReviewResponse(Long reviewId, Long orderItemId, Long productId, Long customerId,
                          String customerName, Integer rating, String comment, Boolean isAutoReview,
                          String replyMessage, LocalDateTime repliedAt,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.reviewId = reviewId;
        this.orderItemId = orderItemId;
        this.productId = productId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.rating = rating;
        this.comment = comment;
        this.isAutoReview = isAutoReview != null ? isAutoReview : false;
        this.replyMessage = replyMessage;
        this.repliedAt = repliedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ReviewResponse fromEntity(Review review) {
        if (review == null) {
            return null;
        }

        Long orderItemId = review.getOrderItem() != null ? review.getOrderItem().getOrderItemId() : null;
        Long productId = review.getProduct() != null ? review.getProduct().getProductId() : null;
        Long customerId = review.getCustomer() != null ? review.getCustomer().getCustomerId() : null;
        String customerName = review.getCustomer() != null ? review.getCustomer().getFullName() : null;

        String replyMessage = null;
        LocalDateTime repliedAt = null;
        if (review.getReviewReply() != null) {
            replyMessage = review.getReviewReply().getReplyMessage();
            repliedAt = review.getReviewReply().getRepliedAt();
        }

        return new ReviewResponse(
                review.getReviewId(),
                orderItemId,
                productId,
                customerId,
                customerName,
                review.getRating(),
                review.getComment(),
                review.getIsAutoReview(),
                replyMessage,
                repliedAt,
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }

    public Long getReviewId() {
        return reviewId;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Boolean getIsAutoReview() {
        return isAutoReview;
    }

    public void setIsAutoReview(Boolean isAutoReview) {
        this.isAutoReview = isAutoReview;
    }

    public String getReplyMessage() {
        return replyMessage;
    }

    public void setReplyMessage(String replyMessage) {
        this.replyMessage = replyMessage;
    }

    public LocalDateTime getRepliedAt() {
        return repliedAt;
    }

    public void setRepliedAt(LocalDateTime repliedAt) {
        this.repliedAt = repliedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
