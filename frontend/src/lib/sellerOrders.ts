import { bahtCurrency } from "@/lib/currency";
import { z } from "zod";

export const sellerOrderSchema = z.object({
  orderId: z.number().int().positive().safe(), subOrderNumber: z.string(), sellerId: z.number().int().positive().safe(),
  orderGroupId: z.number().int().positive().safe().nullable(), shopName: z.string().nullable(),
  subtotal: z.number().nonnegative(), shippingFee: z.number().nonnegative(), sellerDiscount: z.number().nonnegative().nullable(),
  totalAmount: z.number().nonnegative(), shippingMethod: z.string().nullable(),
  orderStatus: z.enum(["PENDING_PAYMENT", "WAITING_SELLER_CONFIRM", "PREPARING", "SHIPPED", "COMPLETED", "CANCELLED"]),
  paymentStatus: z.enum(["PENDING", "PAID", "PARTIALLY_REFUNDED", "REFUNDED", "FAILED"]).nullable(),
  customerName: z.string().nullable(), createdAt: z.string().nullable(), rejectionReason: z.string().nullable(),
  shippingAddress: z.object({ receiverName: z.string(), phoneNumber: z.string(), addressLine: z.string(), district: z.string(), province: z.string(), postalCode: z.string() }).nullable(),
  items: z.array(z.object({ orderItemId: z.number().int(), productName: z.string(), quantity: z.number().int().positive(), unitPrice: z.number().nonnegative(), totalPrice: z.number().nonnegative() })),
  shipment: z.object({ courierName: z.string().nullable(), trackingNumber: z.string().nullable(), shippingStatus: z.string().nullable() }).nullable(),
});
export const sellerShopSchema = z.object({ sellerId: z.number().int().positive().safe(), shopName: z.string() });
export type SellerOrder = z.infer<typeof sellerOrderSchema>;
export type SellerOrderData = { shop: z.infer<typeof sellerShopSchema>; orders: SellerOrder[] };
export type SellerOrderAction = "accept" | "reject" | "ship";
export const sellerActionSchemas = {
  accept: z.object({}).strict(),
  reject: z.object({ reason: z.string().trim().min(1, "Enter a rejection reason.").max(255) }).strict(),
  ship: z.object({ courierName: z.string().trim().min(1, "Enter a courier.").max(100), trackingNumber: z.string().trim().min(1, "Enter a tracking number.").max(100) }).strict(),
};
export const sellerOrderCurrency = bahtCurrency;
export function isSellerOrderPaid(order: SellerOrder) { return order.paymentStatus === "PAID" || order.paymentStatus === "PARTIALLY_REFUNDED"; }
export function hasOrderStatusConflict(order: SellerOrder) {
  return (isSellerOrderPaid(order) && order.orderStatus === "PENDING_PAYMENT") ||
    (!isSellerOrderPaid(order) && ["WAITING_SELLER_CONFIRM", "PREPARING", "SHIPPED"].includes(order.orderStatus));
}
export function sellerOrderSummary(orders: SellerOrder[]) {
  const sorted = [...orders].sort((a, b) => (b.createdAt || "").localeCompare(a.createdAt || "") || b.orderId - a.orderId);
  const active = sorted.filter(order => !["COMPLETED", "CANCELLED"].includes(order.orderStatus));
  const history = sorted.filter(order => ["COMPLETED", "CANCELLED"].includes(order.orderStatus));
  return {
    active, history,
    paidSales: sorted.filter(order => isSellerOrderPaid(order) && order.orderStatus !== "CANCELLED").reduce((cents, order) => cents + Math.round(order.totalAmount * 100), 0) / 100,
    awaiting: sorted.filter(order => isSellerOrderPaid(order) && order.orderStatus === "WAITING_SELLER_CONFIRM").length,
    completed: sorted.filter(order => order.orderStatus === "COMPLETED").length,
  };
}
export function orderDate(value: string | null) {
  if (!value) return "Date unavailable";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Date unavailable" : new Intl.DateTimeFormat("en-US", { dateStyle: "medium", timeStyle: "short" }).format(date);
}
