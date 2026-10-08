import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";

const backend = process.env.BACKEND_API_URL || "http://localhost:8080";

function backendPath(method: string, segments: string[]): string | null {
  if (
    segments.length === 1 &&
    segments[0] === "applications" &&
    method === "GET"
  )
    return "/api/seller-applications/pending";
  if (
    segments.length === 2 &&
    segments[0] === "applications" &&
    segments[1] === "history" &&
    method === "GET"
  )
    return "/api/seller-applications";
  if (
    segments.length === 2 &&
    segments[0] === "applications" &&
    /^\d+$/.test(segments[1]) &&
    method === "GET"
  )
    return `/api/seller-applications/${segments[1]}`;
  if (
    segments.length === 3 &&
    segments[0] === "applications" &&
    /^\d+$/.test(segments[1]) &&
    ["approve", "reject", "request-docs"].includes(segments[2]) &&
    method === "PUT"
  )
    return `/api/seller-applications/${segments[1]}/${segments[2]}`;
  if (
    segments.length === 1 &&
    segments[0] === "categories" &&
    ["GET", "POST"].includes(method)
  )
    return "/api/categories";
  if (
    segments.length === 2 &&
    segments[0] === "categories" &&
    /^\d+$/.test(segments[1]) &&
    method === "DELETE"
  )
    return `/api/categories/${segments[1]}`;
  return null;
}

async function forward(request: NextRequest, path: string[]) {
  const target = backendPath(request.method, path);
  if (!target)
    return NextResponse.json(
      { success: false, message: "Not found" },
      { status: 404 },
    );
  const token = (await cookies()).get("auth_token")?.value;
  if (!token)
    return NextResponse.json(
      { success: false, message: "Please sign in" },
      { status: 401 },
    );

  try {
    const identity = await fetch(`${backend}/api/auth/me`, {
      headers: { Authorization: `Bearer ${token}` },
      cache: "no-store",
      signal: AbortSignal.timeout(8000),
    });
    const me = identity.ok ? await identity.json() : null;
    if (!me?.success)
      return NextResponse.json(
        { success: false, message: "Please sign in again" },
        { status: 401 },
      );
    if (me.data?.role !== "ADMIN")
      return NextResponse.json(
        { success: false, message: "Admin access required" },
        { status: 403 },
      );

    const response = await fetch(`${backend}${target}`, {
      method: request.method,
      cache: "no-store",
      signal: AbortSignal.timeout(8000),
      headers: {
        Authorization: `Bearer ${token}`,
        ...(["POST", "PUT"].includes(request.method)
          ? { "Content-Type": "application/json" }
          : {}),
      },
      ...(["POST", "PUT"].includes(request.method)
        ? { body: await request.text() }
        : {}),
    });
    const body = await response
      .json()
      .catch(() => ({
        success: false,
        message: "Invalid response from backend",
      }));
    return NextResponse.json(body, {
      status: response.status,
      headers: { "Cache-Control": "no-store" },
    });
  } catch {
    return NextResponse.json(
      { success: false, message: "Admin service is unavailable" },
      { status: 503 },
    );
  }
}

type Context = { params: Promise<{ path: string[] }> };
export async function GET(request: NextRequest, context: Context) {
  return forward(request, (await context.params).path);
}
export async function POST(request: NextRequest, context: Context) {
  return forward(request, (await context.params).path);
}
export async function PUT(request: NextRequest, context: Context) {
  return forward(request, (await context.params).path);
}
export async function DELETE(request: NextRequest, context: Context) {
  return forward(request, (await context.params).path);
}
