import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";

const backend = process.env.BACKEND_API_URL || "http://localhost:8080";
type Context = { params: Promise<{ path?: string[] }> };

export async function POST(request: NextRequest, context: Context) {
  const path = (await context.params).path || [];
  const orderId = path[0];
  const action = path[1];

  if (!orderId || !/^[1-9]\d*$/.test(orderId) || !action || !["confirm-delivered", "cancel"].includes(action)) {
    return NextResponse.json({ success: false, message: "Not found" }, { status: 404 });
  }

  const token = (await cookies()).get("auth_token")?.value;
  if (!token) return NextResponse.json({ success: false, message: "Please sign in first" }, { status: 401 });

  try {
    const bodyText = await request.text();
    const headers: Record<string, string> = {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    };

    const response = await fetch(`${backend}/api/sub-orders/${orderId}/${action}`, {
      method: "POST",
      cache: "no-store",
      headers,
      ...(bodyText ? { body: bodyText } : {}),
    });

    const result = await response.json().catch(() => ({ success: false, message: "Invalid service response" }));
    return NextResponse.json(result, { status: response.status, headers: { "Cache-Control": "no-store" } });
  } catch {
    return NextResponse.json({ success: false, message: "Order service is unavailable" }, { status: 503 });
  }
}
