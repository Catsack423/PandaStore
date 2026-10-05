package project.project.DTO.shipping;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignTrackingRequest {

    @NotBlank(message = "กรุณาระบุชื่อบริษัทขนส่ง (courierName)")
    @Size(max = 100, message = "ชื่อบริษัทขนส่งต้องไม่เกิน 100 ตัวอักษร")
    private String courierName;

    @NotBlank(message = "กรุณาระบุหมายเลข Tracking (trackingNumber)")
    @Size(max = 100, message = "หมายเลข Tracking ต้องไม่เกิน 100 ตัวอักษร")
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
