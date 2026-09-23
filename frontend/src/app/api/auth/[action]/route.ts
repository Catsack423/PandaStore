import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";

const backend = process.env.BACKEND_API_URL || "http://localhost:8080";
const cookieName = "auth_token";

async function callBackend(path: string, options: RequestInit = {}) {
  const response = await fetch(`${backend}${path}`, {
    ...options,
    cache: "no-store",
    signal: AbortSignal.timeout(8000),
  });
  const body = await response.json().catch(() => null);
  return { response, body };
}

function failure(message: string, status: number) {
  return NextResponse.json({ success: false, message }, { status });
}

export async function GET(_request: NextRequest, { params }: { params: Promise<{ action: string }> }) {
  if ((await params).action !== "me") return failure("Not found", 404);
  const token = (await cookies()).get(cookieName)?.value;
  if (!token) return failure("Please sign in", 401);
  try {
    const { response, body } = await callBackend("/api/auth/me", {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!response.ok || !body?.success) {
      const result = failure("Session expired. Please sign in again.", 401);
      result.cookies.delete(cookieName);
      result.cookies.delete("user_role");
      return result;
    }
    const data = body.data;
    const result = NextResponse.json({ user: {
      id: String(data.userId), name: data.username, email: data.email,
      role: data.role, status: data.status,
    } }, { headers: { "Cache-Control": "no-store" } });
    result.cookies.set("user_role", data.role, { httpOnly: true, secure: process.env.NODE_ENV === "production", sameSite: "lax", path: "/", maxAge: 60 * 60 * 24 });
    return result;
  } catch {
    return failure("Authentication service is unavailable", 503);
  }
}

export async function POST(request: NextRequest, { params }: { params: Promise<{ action: string }> }) {
  const action = (await params).action;
  if (action === "logout") {
    const token = (await cookies()).get(cookieName)?.value;
    if (token) {
      try { await callBackend("/api/auth/logout", { method: "POST", headers: { Authorization: `Bearer ${token}` } }); } catch { /* Clear the browser session even if backend is offline. */ }
    }
    const result = NextResponse.json({ success: true });
    result.cookies.delete(cookieName);
    result.cookies.delete("user_role");
    return result;
  }
  if (action !== "login" && action !== "register") return failure("Not found", 404);

  const input = await request.json().catch(() => null);
  if (!input || typeof input !== "object") return failure("Invalid request", 400);
  const path = action === "login" ? "/api/auth/login" : "/api/auth/register/customer";
  try {
    const { response, body } = await callBackend(path, {
      method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(input),
    });
    if (!response.ok || !body?.success) return failure(body?.message || (action === "login" ? "Invalid username or password" : "Registration failed"), response.status);
    if (action === "register") return NextResponse.json({ success: true, data: body.data }, { status: 201 });

    const { token, user } = body.data || {};
    if (typeof token !== "string" || !user?.userId || !["CUSTOMER", "SELLER", "ADMIN"].includes(user.role)) return failure("Invalid login response", 502);
    const result = NextResponse.json({ success: true, user: {
      id: String(user.userId), name: user.username, email: user.email,
      role: user.role, status: user.status,
    } });
    const options = { httpOnly: true, secure: process.env.NODE_ENV === "production", sameSite: "lax" as const, path: "/", maxAge: 60 * 60 * 24 };
    result.cookies.set(cookieName, token, options);
    result.cookies.set("user_role", user.role, options);
    return result;
  } catch {
    return failure("Authentication service is unavailable", 503);
  }
}
