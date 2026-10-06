package project.project.DTO.review;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ReplyReviewRequest {

    @NotBlank(message = "Reply is required")
    @Size(max = 2000, message = "Reply must not exceed 2000 characters")
    private String replyMessage;

    public ReplyReviewRequest() {
    }

    public ReplyReviewRequest(String replyMessage) {
        this.replyMessage = replyMessage;
    }

    public String getReplyMessage() {
        return replyMessage;
    }

    public void setReplyMessage(String replyMessage) {
        this.replyMessage = replyMessage;
    }
}
