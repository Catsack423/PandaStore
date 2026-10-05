package project.project.DTO.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateReviewRequest {

    @NotNull(message = "Order item ID is required")
    private Long orderItemId;

    @NotNull(message = "Review rating is required")
    @Min(value = 1, message = "Review rating must be between 1 and 5 stars")
    @Max(value = 5, message = "Review rating must be between 1 and 5 stars")
    private Integer rating;

    @Size(max = 2000, message = "Review comment must not exceed 2000 characters")
    private String comment;

    public CreateReviewRequest() {
    }

    public CreateReviewRequest(Long orderItemId, Integer rating, String comment) {
        this.orderItemId = orderItemId;
        this.rating = rating;
        this.comment = comment;
    }

    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
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
}
