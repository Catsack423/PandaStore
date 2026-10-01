import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";
import type { PaymentOrder } from "@/components/Payment/api";
import { isPendingPayment } from "@/components/Payment/api";

const backend = process.env.BACKEND_API_URL || "http://localhost:8080";

/** Frontend proxy for the existing payment APIs; no backend service or endpoint is added. */
export async function POST(_request: NextRequest, context: { params: Promise<{ orderGroupId: string }> }) {
  const { orderGroupId } = await context.params;
  if (!/^[1-9]\d*$/.test(orderGroupId) || !Number.isSafeInteger(Number(orderGroupId))) {
    return NextResponse.json({ success: false, message: "Order not found" }, { status: 404 });
  }
  const token = (await cookies()).get("auth_token")?.value;
  if (!token) return NextResponse.json({ success: false, message: "Please sign in first" }, { status: 401 });

  async function call(path: string, body?: object) {
    const response = await fetch(`${backend}/api/${path}`, {
      method: body ? "POST" : "GET", cache: "no-store", signal: AbortSignal.timeout(20000),
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      ...(body ? { body: JSON.stringify(body) } : {}),
    });
    const result = await response.json();
    return { response, result };
  }
  function failed(upstream: Awaited<ReturnType<typeof call>>) {
    return NextResponse.json(upstream.result, { status: upstream.response.ok ? 502 : upstream.response.status });
  }

  try {
    // Checkout's existing read endpoint checks ownership using the customer's session.
    const owned = await call(`checkout/orders/${orderGroupId}`);
    if (!owned.response.ok || !owned.result.success) return failed(owned);
    const order = owned.result.data as PaymentOrder;
    if (!isPendingPayment(order)) {
      return NextResponse.json({ success: false, message: "This order is no longer pending payment. Refresh order status." }, { status: 409 });
    }
    const payment = await call(`payments/order-group/${orderGroupId}`);
    if (!payment.response.ok || !payment.result.success) return failed(payment);
    if (payment.result.data.status !== "PENDING") {
      return NextResponse.json({ success: false, message: "This payment is no longer pending. Refresh order status." }, { status: 409 });
    }
    // Checkout creates a pending payment without a gateway transaction ID. Initialize it
    // using the existing API and the persisted method/total, then call the existing mock.
    if (!payment.result.data.gatewayTransactionId) {
      const initiated = await call("payments/initiate", {
        orderGroupId: Number(orderGroupId), paymentMethod: payment.result.data.paymentMethod,
        amount: order.grandTotal,
      });
      if (!initiated.response.ok || !initiated.result.success) return failed(initiated);
    }
    const confirmed = await call("payments/simulate", { orderGroupId: Number(orderGroupId), isSuccess: true });
    if (!confirmed.response.ok || !confirmed.result.success) return failed(confirmed);
    const refreshed = await call(`checkout/orders/${orderGroupId}`);
    if (!refreshed.response.ok || !refreshed.result.success) return failed(refreshed);
    return NextResponse.json(refreshed.result, { headers: { "Cache-Control": "no-store" } });
  } catch {
    return NextResponse.json({ success: false, message: "The payment service is unavailable. Refresh order status before trying again." }, { status: 503 });
  }
}
