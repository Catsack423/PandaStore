package project.project.DTO.shipping;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignTrackingRequest {

    @NotBlank(message = "Please specify the courier name (courierName)")
    @Size(max = 100, message = "Courier name must not exceed 100 characters")
    private String courierName;

    @NotBlank(message = "Please specify the tracking number (trackingNumber)")
    @Size(max = 100, message = "Tracking number must not exceed 100 characters")
    private String trackingNumber;

    public AssignTrackingRequest() {
    }

    public AssignTrackingRequest(String courierName, String trackingNumber) {
        this.courierName = courierName;
        this.trackingNumber = trackingNumber;
    }

    public String getCourierName() {
        return courierName;
    }

    public void setCourierName(String courierName) {
        this.courierName = courierName;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }
}
