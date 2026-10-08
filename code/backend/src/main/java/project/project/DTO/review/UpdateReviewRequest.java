package project.project.DTO.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateReviewRequest {

    @NotNull(message = "Review rating is required")
    @Min(value = 1, message = "Review rating must be between 1 and 5 stars")
    @Max(value = 5, message = "Review rating must be between 1 and 5 stars")
    private Integer rating;

    @Size(max = 2000, message = "Review comment must not exceed 2000 characters")
    private String comment;

    public UpdateReviewRequest() {
    }

    public UpdateReviewRequest(Integer rating, String comment) {
        this.rating = rating;
        this.comment = comment;
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
