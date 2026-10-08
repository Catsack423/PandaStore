import type { Metadata } from "next";
import SellerApplication from "@/components/Seller/SellerApplication";

export const metadata: Metadata = { title: "Seller Applications | PandaStore" };
export default function SellerApplicationPage() { return <SellerApplication />; }
