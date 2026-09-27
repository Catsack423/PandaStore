import type { Metadata } from "next";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import SellerDashboard from "@/components/Seller/SellerDashboard";

export const metadata: Metadata = { title: "Shop Dashboard | PandaStore" };
export default async function SellerDashboardPage() {
  const token = (await cookies()).get("auth_token")?.value;
  if (!token) redirect("/signin?callbackUrl=%2Fseller-dashboard");
  let seller = false;
  try {
    const response = await fetch(`${process.env.BACKEND_API_URL || "http://localhost:8080"}/api/auth/me`, {
      headers: { Authorization: `Bearer ${token}` }, cache: "no-store", signal: AbortSignal.timeout(8000),
    });
    const body = await response.json();
    seller = response.ok && body.success && body.data?.role === "SELLER" && body.data?.status === "ACTIVE";
  } catch { /* Keep dashboard closed when identity cannot be verified. */ }
  if (!seller) redirect("/seller-application");
  return <SellerDashboard />;
}
