import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";

const backend = process.env.BACKEND_API_URL || "http://localhost:8080";
type Context = { params: Promise<{ path?: string[] }> };

async function forward(request: NextRequest, context: Context) {
  const path = (await context.params).path?.join("/") || "";
  const paymentAction = /^orders\/[1-9]\d*\/(confirm-payment|cancel)$/.test(path);
  if (!(request.method === "GET" && (path === "" || path === "orders" || /^orders\/[1-9]\d*$/.test(path))) &&
      !(request.method === "POST" && (["cart", "quote", "addresses", "orders"].includes(path) || paymentAction))) {
    return NextResponse.json({ success: false, message: "Not found" }, { status: 404 });
  }
  const token = (await cookies()).get("auth_token")?.value;
  if (!token) return NextResponse.json({ success: false, message: "Please sign in first" }, { status: 401 });
  if (request.method === "POST" && path.endsWith("/confirm-payment") &&
      (process.env.MOCK_PAYMENT_ENABLED !== "true" || process.env.NEXT_PUBLIC_MOCK_PAYMENT !== "true")) {
    return NextResponse.json({ success: false, message: "Payment is not available" }, { status: 403 });
  }
  try {
    const response = await fetch(`${backend}/api/checkout${path ? `/${path}` : ""}`, {
      method: request.method, cache: "no-store", signal: AbortSignal.timeout(20000),
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      ...(request.method === "POST" ? { body: await request.text() } : {}),
    });
    const body = await response.json().catch(() => ({ success: false, message: "Invalid checkout response" }));
    return NextResponse.json(body, { status: response.status, headers: { "Cache-Control": "no-store" } });
  } catch {
    return NextResponse.json({ success: false, message: paymentAction
      ? "The order service is unavailable. Refresh order status before trying again."
      : "Checkout service is unavailable. Please reload to check your cart before trying again." }, { status: 503 });
  }
}
export const GET = forward;
export const POST = forward;
