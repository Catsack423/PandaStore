import type { Metadata } from "next";
import { cookies } from "next/headers";
import { notFound, redirect } from "next/navigation";
import Payment from "@/components/Payment";
import type { PaymentOrder } from "@/components/Payment/api";

export const metadata: Metadata = { title: "Payment | Panda Store" };

export default async function PaymentPage({ params }: { params: Promise<{ orderGroupId: string }> }) {
  const { orderGroupId } = await params;
  if (!/^[1-9]\d*$/.test(orderGroupId) || !Number.isSafeInteger(Number(orderGroupId))) notFound();
  const token = (await cookies()).get("auth_token")?.value;
  if (!token) redirect(`/signin?callbackUrl=${encodeURIComponent(`/payment/${orderGroupId}`)}`);
  let order: PaymentOrder | null = null;
  let missing = false;
  let unauthorized = false;
  try {
    const response = await fetch(`${process.env.BACKEND_API_URL || "http://localhost:8080"}/api/checkout/orders/${orderGroupId}`, {
      headers: { Authorization: `Bearer ${token}` }, cache: "no-store", signal: AbortSignal.timeout(20000),
    });
    missing = response.status === 404 || response.status === 403;
    unauthorized = response.status === 401;
    if (response.ok) {
      const result = await response.json();
      if (result.success) order = result.data;
    }
  } catch { /* Render a retryable service error without exposing order information. */ }
  if (missing) notFound();
  if (unauthorized) redirect(`/signin?callbackUrl=${encodeURIComponent(`/payment/${orderGroupId}`)}`);
  return <Payment initialOrder={order} qrSource="/images/payment/qr-code.svg" loadError={!order} />;
}
