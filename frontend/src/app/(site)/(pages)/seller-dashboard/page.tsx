import type { Metadata } from "next";
import SellerDashboard from "@/components/Seller/SellerDashboard";

export const metadata: Metadata = { title: "Shop Dashboard | PandaStore" };
export default function SellerDashboardPage() { return <SellerDashboard />; }
