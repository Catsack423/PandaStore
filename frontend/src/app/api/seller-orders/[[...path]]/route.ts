import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";
import { sellerActionSchemas, sellerOrderSchema, sellerShopSchema, type SellerOrderAction } from "@/lib/sellerOrders";

type Context = { params: Promise<{ path?: string[] }> };
const backend = process.env.BACKEND_API_URL || "http://localhost:8080";
class ApiError extends Error { constructor(public status: number, message: string) { super(message); } }
function reply(body: unknown, status = 200) { return NextResponse.json(body, { status, headers: { "Cache-Control": "no-store" } }); }

async function forward(request: NextRequest, context: Context) {
  const path = (await context.params).path || [];
  const orderId = path[0];
  const action = path[1] as SellerOrderAction;
  if ((path.length && (!/^[1-9]\d*$/.test(orderId) || !Number.isSafeInteger(Number(orderId)))) ||
      (request.method === "GET" ? path.length > 1 : path.length !== 2 || !Object.hasOwn(sellerActionSchemas, action)))
    return reply({ success: false, message: "Not found" }, 404);
  if (request.nextUrl.searchParams.has("sellerId")) return reply({ success: false, message: "Shop identity comes from your session" }, 400);
  const origin = request.headers.get("origin");
  const host = request.headers.get("host");
  const allowedOrigins = new Set([
    request.nextUrl.origin,
    host ? `http://${host}` : "",
    host ? `https://${host}` : "",
    "http://localhost:3000",
    "http://127.0.0.1:3000",
    "http://172.27.0.1:3000",
  ]);
  let isAllowedOrigin = !origin || allowedOrigins.has(origin);
  if (origin && !isAllowedOrigin) {
    try {
      const originUrl = new URL(origin);
      const isLoopback = ["localhost", "127.0.0.1", "::1"].includes(originUrl.hostname);
      const isSameHost = Boolean(host && (host === originUrl.host || host.split(":")[0] === originUrl.hostname));
      if (isLoopback || isSameHost) {
        isAllowedOrigin = true;
      }
    } catch {
      isAllowedOrigin = false;
    }
  }
  if (request.method === "POST" && !isAllowedOrigin)
    return reply({ success: false, message: "Invalid request origin" }, 403);

  const token = (await cookies()).get("auth_token")?.value;
  if (!token) return reply({ success: false, message: "Please sign in first" }, 401);
  async function call(url: string, body?: unknown) {
    const response = await fetch(`${backend}${url}`, {
      method: body === undefined ? "GET" : "POST", cache: "no-store", signal: AbortSignal.timeout(body === undefined ? 8000 : 15000),
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    });
    const result = await response.json().catch(() => { throw new ApiError(502, "Invalid order service response"); });
    if (!response.ok || !result.success) throw new ApiError(response.ok ? 502 : response.status, result.message || "Order service is unavailable");
    return result.data;
  }
  try {
    const me = await call("/api/auth/me");
    if (me?.role !== "SELLER" || me.status !== "ACTIVE" || !Number.isSafeInteger(me.userId) || me.userId < 1)
      throw new ApiError(403, "An active Seller account is required");
    const ownedShop = await call(`/api/seller/shops/user/${me.userId}`);
    if (ownedShop?.status !== "ACTIVE") throw new ApiError(403, "An active shop is required");
    const shop = sellerShopSchema.parse(ownedShop);
    if (!orderId) {
      const orders = z.array(sellerOrderSchema).parse(await call(`/api/sub-orders/seller/${shop.sellerId}`));
      if (orders.some(order => order.sellerId !== shop.sellerId)) throw new ApiError(502, "Invalid shop order response");
      return reply({ success: true, data: { shop, orders } });
    }
    const order = sellerOrderSchema.parse(await call(`/api/sub-orders/${orderId}`));
    if (order.sellerId !== shop.sellerId || order.orderId !== Number(orderId)) throw new ApiError(403, "This order is not available to your shop");
    if (request.method === "GET") return reply({ success: true, data: { shop, order } });
    let payload: unknown;
    try { payload = await request.json(); } catch { throw new ApiError(400, "Invalid request data"); }
    const validated = sellerActionSchemas[action].safeParse(payload);
    if (!validated.success) throw new ApiError(400, validated.error.issues[0]?.message || "Invalid request data");
    const input = validated.data;
    const endpoint = action === "ship" ? `/api/shipping/orders/${orderId}/assign-tracking` : `/api/sub-orders/${orderId}/${action}`;
    await call(`${endpoint}?sellerId=${shop.sellerId}`, input);
    return reply({ success: true, data: null, message: "Order updated" });
  } catch (error) {
    const status = error instanceof ApiError ? error.status : error instanceof z.ZodError ? 502 : 503;
    const message = error instanceof ApiError ? error.message : error instanceof z.ZodError ? "Invalid order service response" : "Order service is unavailable. Reload the order to check its latest status.";
    return reply({ success: false, message }, status);
  }
}
export const GET = forward;
export const POST = forward;
