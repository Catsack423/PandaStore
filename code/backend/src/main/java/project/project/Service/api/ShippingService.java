package project.project.Service.api;

import project.project.Entity.order.Shipment;
import java.math.BigDecimal;

public interface ShippingService {
    BigDecimal calculateShippingFee(Long sellerId, String shippingMethod, Long addressId);
    Shipment assignTrackingNumber(Long sellerId, Long orderId, String courierName, String trackingNumber);
    Shipment getShipmentByOrderId(Long orderId);
    void updateShippingStatus(Long shipmentId, String status);
}
