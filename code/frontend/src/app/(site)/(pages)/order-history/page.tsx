import type { Metadata } from "next";
import OrderHistory from "@/components/OrderHistory";

export const metadata: Metadata = {
  title: "Order History | PandaStore",
  description: "View your orders and delivery progress.",
};

export default async function OrderHistoryPage({ searchParams }: { searchParams: Promise<{ from?: string }> }) {
  const { from } = await searchParams;
  return <OrderHistory showBackToAccount={from === "my-account"} />;
}
