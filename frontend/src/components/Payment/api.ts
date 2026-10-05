import { type PlacedOrder } from "@/components/Checkout/api";

export type PaymentOrder = PlacedOrder;

export function isPendingPayment(order: PaymentOrder): boolean {
  return order.paymentStatus === "PENDING" && order.subOrders.length > 0
    && order.subOrders.every(item => item.orderStatus === "PENDING_PAYMENT");
}

export function paymentStatusLabel(order: PaymentOrder): string {
  if (order.subOrders.length > 0 && order.subOrders.every(item => item.orderStatus === "CANCELLED")) return "Order cancelled";
  if (isPendingPayment(order)) return "Pending payment";
  return ({ PAID: "Payment successful", PARTIALLY_REFUNDED: "Payment partially refunded",
    REFUNDED: "Payment refunded", FAILED: "Payment failed" }[order.paymentStatus] || "Payment is not available");
}

export async function confirmPayment(orderGroupId: number): Promise<PaymentOrder> {
  const response = await fetch(`/api/payment/${orderGroupId}`, { method: "POST", cache: "no-store" });
  const result = await response.json().catch(() => null);
  if (!response.ok || !result?.success) throw new Error(result?.message || "Could not confirm payment. Please try again.");
  return result.data as PaymentOrder;
}

export async function cancelPaymentOrder(orderGroupId: number): Promise<void> {
  const response = await fetch(`/api/payment/${orderGroupId}`, { method: "DELETE", cache: "no-store" });
  const result = await response.json().catch(() => null);
  if (!response.ok || !result?.success) throw new Error(result?.message || "Could not cancel order. Please try again.");
}
