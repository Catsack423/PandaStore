package project.project.Observer;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import project.project.Event.OrderPaidEvent;
import project.project.Service.api.NotificationService;

@Component
public class OrderPaidNotificationObserver {

    private final NotificationService notificationService;

    public OrderPaidNotificationObserver(
            NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onOrderPaid(OrderPaidEvent event) {

        notificationService.notifyCustomerOrderPaid(
                event.customerId(),
                event.orderGroupId());

        for (OrderPaidEvent.SellerOrder sellerOrder : event.sellerOrders()) {
            notificationService.notifySellerNewOrder(
                    sellerOrder.sellerId(),
                    sellerOrder.orderId());
        }
    }
}