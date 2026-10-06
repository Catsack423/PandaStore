package project.project.DTO.shipping;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class ShippingFeeResponse {

    private Long sellerId;
    private String shippingMethod;
    private BigDecimal shippingFee;

    public ShippingFeeResponse() {
    }

    public ShippingFeeResponse(Long sellerId, String shippingMethod, BigDecimal shippingFee) {
        this.sellerId = sellerId;
        this.shippingMethod = shippingMethod;
        this.shippingFee = shippingFee;
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

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(BigDecimal shippingFee) {
        this.shippingFee = shippingFee;
    }
}
