import type { Metadata } from "next";
import OrderHistory from "@/components/OrderHistory";

export const metadata: Metadata = {
  title: "Order History | NextMerce",
  description: "View your orders and delivery progress.",
};

export default function OrderHistoryPage() {
  return <OrderHistory />;
}
