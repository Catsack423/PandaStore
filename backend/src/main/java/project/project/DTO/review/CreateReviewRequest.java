package project.project.DTO.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateReviewRequest {

    @NotNull(message = "Order Item ID ห้ามว่าง")
    private Long orderItemId;

    @NotNull(message = "คะแนนรีวิวห้ามว่าง")
    @Min(value = 1, message = "คะแนนรีวิวต้องอยู่ระหว่าง 1 ถึง 5 ดาว")
    @Max(value = 5, message = "คะแนนรีวิวต้องอยู่ระหว่าง 1 ถึง 5 ดาว")
    private Integer rating;

    @Size(max = 2000, message = "ข้อความรีวิวต้องมีความยาวไม่เกิน 2000 ตัวอักษร")
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
