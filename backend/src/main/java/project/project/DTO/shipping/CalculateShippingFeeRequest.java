package project.project.DTO.shipping;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CalculateShippingFeeRequest {

    private Long sellerId;

    @NotBlank(message = "กรุณาระบุวิธีจัดส่ง")
    private String shippingMethod;

    private Long addressId;

    public CalculateShippingFeeRequest() {
    }

    public CalculateShippingFeeRequest(Long sellerId, String shippingMethod, Long addressId) {
        this.sellerId = sellerId;
        this.shippingMethod = shippingMethod;
        this.addressId = addressId;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public String getShippingMethod() {
        return shippingMethod;
    }

    public void setShippingMethod(String shippingMethod) {
        this.shippingMethod = shippingMethod;
    }

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }
}
