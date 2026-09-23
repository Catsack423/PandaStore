package project.project.Service.implement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.project.Entity.order.*;
import project.project.Exception.ResourceNotFoundException;
import project.project.Repository.OrderGroupRepository;
import project.project.Repository.OrderRepository;
import project.project.Repository.PaymentRepository;
import project.project.Service.api.NotificationService;
import project.project.Service.api.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentServiceImp implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderGroupRepository orderGroupRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    public PaymentServiceImp(PaymentRepository paymentRepository,
                             OrderGroupRepository orderGroupRepository,
                             OrderRepository orderRepository,
                             @Autowired(required = false) NotificationService notificationService) {
        this.paymentRepository = paymentRepository;
        this.orderGroupRepository = orderGroupRepository;
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payment initiatePayment(Long orderGroupId, PaymentMethod method) {
        return initiatePayment(orderGroupId, method, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payment initiatePayment(Long orderGroupId, PaymentMethod method, BigDecimal amount) {
        if (orderGroupId == null) {
            throw new IllegalArgumentException("Order group ID cannot be null");
        }
        if (method == null) {
            throw new IllegalArgumentException("Payment method cannot be null");
        }

        OrderGroup orderGroup = orderGroupRepository.findById(orderGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("Order group not found with id: " + orderGroupId));

        if (amount == null) {
            amount = orderGroup.getGrandTotal();
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than 0");
        }

        if (orderGroup.getGrandTotal() != null && amount.compareTo(orderGroup.getGrandTotal()) != 0) {
            throw new IllegalArgumentException("Payment amount (" + amount + ") does not match order grand total (" + orderGroup.getGrandTotal() + ")");
        }

        Optional<Payment> existingOpt = paymentRepository.findByOrderGroup_OrderGroupId(orderGroupId);
        Payment payment;
        if (existingOpt.isPresent()) {
            payment = existingOpt.get();
            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                throw new IllegalStateException("Order group has already been successfully paid");
            }
            if (payment.getStatus() == PaymentStatus.REFUNDED || payment.getStatus() == PaymentStatus.PARTIALLY_REFUNDED) {
                throw new IllegalStateException("Order group has already been processed and refunded");
            }
            payment.setPaymentMethod(method);
            payment.setAmount(amount);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setGatewayTransactionId("GW-TXN-" + UUID.randomUUID().toString());
            payment.setPaidAt(null);
        } else {
            payment = new Payment();
            payment.setOrderGroup(orderGroup);
            payment.setPaymentMethod(method);
            payment.setAmount(amount);
            payment.setRefundedAmount(BigDecimal.ZERO);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setGatewayTransactionId("GW-TXN-" + UUID.randomUUID().toString());
        }

        Payment savedPayment = paymentRepository.save(payment);
        orderGroup.setPayment(savedPayment);
        orderGroup.setPaymentStatus(OrderGroupPaymentStatus.PENDING);
        orderGroupRepository.save(orderGroup);

        return savedPayment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleGatewayCallback(String gatewayTransactionId, boolean isSuccess) {
        if (gatewayTransactionId == null || gatewayTransactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Gateway transaction ID cannot be null or empty");
        }

        Payment payment = paymentRepository.findByGatewayTransactionId(gatewayTransactionId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with gateway transaction id: " + gatewayTransactionId));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            if (isSuccess) {
                return; // Idempotent: already successful
            } else {
                throw new IllegalStateException("Cannot fail an already successful payment");
            }
        }

        if (payment.getStatus() == PaymentStatus.REFUNDED || payment.getStatus() == PaymentStatus.PARTIALLY_REFUNDED) {
            throw new IllegalStateException("Payment has already been refunded");
        }

        OrderGroup orderGroup = payment.getOrderGroup();

        if (isSuccess) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());

            if (orderGroup != null) {
                orderGroup.setPaymentStatus(OrderGroupPaymentStatus.PAID);
                if (orderGroup.getSubOrders() != null) {
                    for (Order subOrder : orderGroup.getSubOrders()) {
                        if (subOrder.getOrderStatus() == null || subOrder.getOrderStatus() == OrderStatus.WAITING_SELLER_CONFIRM) {
                            subOrder.setOrderStatus(OrderStatus.WAITING_SELLER_CONFIRM);
                        }
                    }
                }
                orderGroupRepository.save(orderGroup);

                if (notificationService != null) {
                    if (orderGroup.getCustomer() != null && orderGroup.getCustomer().getCustomerId() != null) {
                        notificationService.notifyCustomerOrderPaid(orderGroup.getCustomer().getCustomerId(), orderGroup.getOrderGroupId());
                    }
                    if (orderGroup.getSubOrders() != null) {
                        for (Order subOrder : orderGroup.getSubOrders()) {
                            if (subOrder.getSeller() != null && subOrder.getSeller().getSellerId() != null) {
                                notificationService.notifySellerNewOrder(subOrder.getSeller().getSellerId(), subOrder.getOrderId());
                            }
                        }
                    }
                }
            }
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            if (orderGroup != null) {
                orderGroup.setPaymentStatus(OrderGroupPaymentStatus.FAILED);
                orderGroupRepository.save(orderGroup);
            }
        }

        paymentRepository.save(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processPartialRefund(Long orderId, BigDecimal refundAmount, String reason) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be greater than 0");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Refund reason is required");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getTotalAmount() != null && refundAmount.compareTo(order.getTotalAmount()) > 0) {
            throw new IllegalArgumentException("Refund amount (" + refundAmount + ") cannot exceed sub-order total amount (" + order.getTotalAmount() + ")");
        }

        OrderGroup orderGroup = order.getOrderGroup();
        if (orderGroup == null) {
            throw new ResourceNotFoundException("Order group not associated with order id: " + orderId);
        }

        Payment payment = paymentRepository.findByOrderGroup_OrderGroupId(orderGroup.getOrderGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order group id: " + orderGroup.getOrderGroupId()));

        if (payment.getStatus() != PaymentStatus.SUCCESS && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new IllegalStateException("Cannot refund payment with status: " + payment.getStatus());
        }

        BigDecimal currentRefunded = payment.getRefundedAmount() != null ? payment.getRefundedAmount() : BigDecimal.ZERO;
        BigDecimal remainingRefundable = payment.getAmount().subtract(currentRefunded);

        if (refundAmount.compareTo(remainingRefundable) > 0) {
            throw new IllegalArgumentException("Refund amount (" + refundAmount + ") exceeds remaining refundable amount (" + remainingRefundable + ")");
        }

        BigDecimal newRefunded = currentRefunded.add(refundAmount);
        payment.setRefundedAmount(newRefunded);

        if (newRefunded.compareTo(payment.getAmount()) >= 0) {
            payment.setStatus(PaymentStatus.REFUNDED);
            orderGroup.setPaymentStatus(OrderGroupPaymentStatus.REFUNDED);
        } else {
            payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
            orderGroup.setPaymentStatus(OrderGroupPaymentStatus.PARTIALLY_REFUNDED);
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setRejectionReason(reason.trim());

        orderRepository.save(order);
        orderGroupRepository.save(orderGroup);
        paymentRepository.save(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processFullRefund(Long orderGroupId, String reason) {
        if (orderGroupId == null) {
            throw new IllegalArgumentException("Order group ID cannot be null");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Refund reason is required");
        }

        OrderGroup orderGroup = orderGroupRepository.findById(orderGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("Order group not found with id: " + orderGroupId));

        Payment payment = paymentRepository.findByOrderGroup_OrderGroupId(orderGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order group id: " + orderGroupId));

        if (payment.getStatus() != PaymentStatus.SUCCESS && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new IllegalStateException("Cannot refund payment with status: " + payment.getStatus());
        }

        BigDecimal currentRefunded = payment.getRefundedAmount() != null ? payment.getRefundedAmount() : BigDecimal.ZERO;
        if (currentRefunded.compareTo(payment.getAmount()) >= 0) {
            throw new IllegalStateException("Payment has already been fully refunded");
        }

        payment.setRefundedAmount(payment.getAmount());
        payment.setStatus(PaymentStatus.REFUNDED);
        orderGroup.setPaymentStatus(OrderGroupPaymentStatus.REFUNDED);

        if (orderGroup.getSubOrders() != null) {
            for (Order subOrder : orderGroup.getSubOrders()) {
                if (subOrder.getOrderStatus() != OrderStatus.COMPLETED) {
                    subOrder.setOrderStatus(OrderStatus.CANCELLED);
                    subOrder.setRejectionReason(reason.trim());
                    orderRepository.save(subOrder);
                }
            }
        }

        orderGroupRepository.save(orderGroup);
        paymentRepository.save(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentByOrderGroupId(Long orderGroupId) {
        if (orderGroupId == null) {
            throw new IllegalArgumentException("Order group ID cannot be null");
        }
        return paymentRepository.findByOrderGroup_OrderGroupId(orderGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order group id: " + orderGroupId));
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentById(Long paymentId) {
        if (paymentId == null) {
            throw new IllegalArgumentException("Payment ID cannot be null");
        }
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentByGatewayTransactionId(String gatewayTransactionId) {
        if (gatewayTransactionId == null || gatewayTransactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Gateway transaction ID cannot be null or empty");
        }
        return paymentRepository.findByGatewayTransactionId(gatewayTransactionId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with gateway transaction id: " + gatewayTransactionId));
    }
}
