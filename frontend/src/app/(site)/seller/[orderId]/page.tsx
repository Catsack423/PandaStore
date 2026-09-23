import type { Metadata } from "next";
import SellerOrder from "@/components/Seller/SellerOrder";

export const metadata: Metadata = { title: "Seller Order | PandaStore" };
export default async function SellerOrderPage({ params }: { params: Promise<{ orderId: string }> }) {
  const { orderId } = await params;
  return <SellerOrder orderId={orderId} />;
}
