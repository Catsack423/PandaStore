import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";

const backend = process.env.BACKEND_API_URL || "http://localhost:8080";
type Context = { params: Promise<{ path?: string[] }> };

async function forward(request: NextRequest, context: Context) {
  const path = (await context.params).path?.join("/") || "";
  const token = (await cookies()).get("auth_token")?.value;

  const url = new URL(request.url);
  const search = url.search;
  const targetUrl = `${backend}/api/reviews${path ? `/${path}` : ""}${search}`;

  try {
    const headers: Record<string, string> = {
      "Content-Type": "application/json",
    };
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }

    const bodyText =
      request.method === "POST" || request.method === "PUT"
        ? await request.text()
        : undefined;

    const response = await fetch(targetUrl, {
      method: request.method,
      cache: "no-store",
      headers,
      signal: AbortSignal.timeout(20000),
      ...(bodyText ? { body: bodyText } : {}),
    });

    const body = await response.json().catch(() => ({
      success: false,
      message: "Invalid review response",
    }));

    return NextResponse.json(body, {
      status: response.status,
      headers: { "Cache-Control": "no-store" },
    });
  } catch {
    return NextResponse.json(
      { success: false, message: "Review service is currently unavailable" },
      { status: 503 },
    );
  }
}

export const GET = forward;
export const POST = forward;
export const PUT = forward;
