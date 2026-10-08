package project.project.DTO.seller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RequestMoreDocumentsRequest {

    @NotBlank(message = "Additional document request message is required")
    @Size(max = 1000, message = "Additional document request message must not exceed 1000 characters")
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
