import { CircleCheck, CircleX, Clock3, Hourglass, Package, Truck } from "lucide-react";

export type OrderStatus = "PENDING_PAYMENT" | "WAITING_SELLER_CONFIRM" | "PREPARING" | "SHIPPED" | "COMPLETED" | "CANCELLED";

export const orderStatusLabels: Record<OrderStatus, string> = {
  PENDING_PAYMENT: "Pending payment",
  WAITING_SELLER_CONFIRM: "Awaiting seller confirmation",
  PREPARING: "Preparing",
  SHIPPED: "Shipped",
  COMPLETED: "Completed",
  CANCELLED: "Cancelled",
};

const presentation = {
  PENDING_PAYMENT: { icon: Clock3, color: "bg-yellow-light-4 text-yellow-dark" },
  WAITING_SELLER_CONFIRM: { icon: Hourglass, color: "bg-blue/10 text-blue" },
  PREPARING: { icon: Package, color: "bg-orange/10 text-orange-dark" },
  SHIPPED: { icon: Truck, color: "bg-teal/10 text-teal-dark" },
  COMPLETED: { icon: CircleCheck, color: "bg-green-light-6 text-green-dark" },
  CANCELLED: { icon: CircleX, color: "bg-red-light-6 text-red-dark" },
};

export default function OrderStatusBadge({ status, label }: { status: OrderStatus; label?: string }) {
  const { icon: Icon, color } = presentation[status];
  return <span className={`inline-flex max-w-full items-center gap-2 rounded-full px-3 py-1.5 text-xs font-medium ${color}`}>
    <Icon size={16} className="shrink-0" aria-hidden="true" />
    <span className="min-w-0 leading-5">{label || orderStatusLabels[status]}</span>
  </span>;
}
