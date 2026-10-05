package project.project.DTO.seller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RequestMoreDocumentsRequest {

    @NotBlank(message = "ข้อความขอเอกสารเพิ่มเติมห้ามว่าง")
    @Size(max = 1000, message = "ข้อความขอเอกสารเพิ่มเติมต้องมีความยาวไม่เกิน 1000 ตัวอักษร")
    private String message;

    public RequestMoreDocumentsRequest() {
    }

    public RequestMoreDocumentsRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
