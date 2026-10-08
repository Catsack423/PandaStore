package project.project.DTO.shipping;

import lombok.Getter;
import lombok.Setter;
import project.project.Entity.order.Shipment;
import project.project.Entity.order.ShippingStatus;

import java.time.LocalDateTime;

@Getter
@Setter
public class ShipmentResponse {

    private Long shipmentId;
    private Long orderId;
    private String courierName;
    private String trackingNumber;
    private ShippingStatus shippingStatus;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;

    public ShipmentResponse() {
    }

    public ShipmentResponse(Long shipmentId, Long orderId, String courierName, String trackingNumber,
                            ShippingStatus shippingStatus, LocalDateTime shippedAt, LocalDateTime deliveredAt) {
        this.shipmentId = shipmentId;
        this.orderId = orderId;
        this.courierName = courierName;
        this.trackingNumber = trackingNumber;
        this.shippingStatus = shippingStatus;
        this.shippedAt = shippedAt;
        this.deliveredAt = deliveredAt;
    }

    public static ShipmentResponse fromEntity(Shipment shipment) {
        if (shipment == null) {
            return null;
        }

        Long orderId = (shipment.getOrder() != null) ? shipment.getOrder().getOrderId() : null;

        return new ShipmentResponse(
                shipment.getShipmentId(),
                orderId,
                shipment.getCourierName(),
                shipment.getTrackingNumber(),
                shipment.getShippingStatus(),
                shipment.getShippedAt(),
                shipment.getDeliveredAt()
        );
    }

    public Long getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(Long shipmentId) {
        this.shipmentId = shipmentId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
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

    public ShippingStatus getShippingStatus() {
        return shippingStatus;
    }

    public void setShippingStatus(ShippingStatus shippingStatus) {
        this.shippingStatus = shippingStatus;
    }

    public LocalDateTime getShippedAt() {
        return shippedAt;
    }

    public void setShippedAt(LocalDateTime shippedAt) {
        this.shippedAt = shippedAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }
}
