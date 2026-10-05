package project.project.DTO.review;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ReplyReviewRequest {

    @NotBlank(message = "ข้อความตอบกลับห้ามว่าง")
    @Size(max = 2000, message = "ข้อความตอบกลับต้องมีความยาวไม่เกิน 2000 ตัวอักษร")
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
