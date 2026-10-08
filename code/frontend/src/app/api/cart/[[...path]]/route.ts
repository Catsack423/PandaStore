import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";

const backend = process.env.BACKEND_API_URL || "http://localhost:8080";
type Context = { params: Promise<{ path?: string[] }> };

async function forward(request: NextRequest, context: Context) {
  const path = (await context.params).path?.join("/") || "";
  const allowed = (path === "" && ["GET", "POST"].includes(request.method)) ||
    (path === "sync" && request.method === "POST") ||
    (path === "items" && ["POST", "DELETE"].includes(request.method)) ||
    (/^items\/[1-9]\d*$/.test(path) && ["PATCH", "DELETE"].includes(request.method));
  if (!allowed) return NextResponse.json({ success: false, message: "Not found" }, { status: 404 });
  const token = (await cookies()).get("auth_token")?.value;
  if (!token) return NextResponse.json({ success: false, message: "Please sign in first" }, { status: 401 });
  try {
    const response = await fetch(`${backend}/api/cart${path ? `/${path}` : ""}`, {
      method: request.method, cache: "no-store", signal: AbortSignal.timeout(15000),
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      ...(["POST", "PATCH"].includes(request.method) ? { body: await request.text() } : {}),
    });
    const body = await response.json().catch(() => ({ success: false, message: "Invalid cart response" }));
    return NextResponse.json(body, { status: response.status, headers: { "Cache-Control": "no-store" } });
  } catch {
    return NextResponse.json({ success: false, message: "Cart service is unavailable. Please refresh before trying again." }, { status: 503 });
  }
}
export const GET = forward;
export const POST = forward;
export const PATCH = forward;
export const DELETE = forward;
