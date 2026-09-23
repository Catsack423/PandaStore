import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";

const backend = process.env.BACKEND_API_URL || "http://localhost:8080";

async function forward(method: "GET" | "POST", request?: NextRequest) {
  const token = (await cookies()).get("auth_token")?.value;
  if (!token)
    return NextResponse.json(
      { success: false, message: "Please sign in first" },
      { status: 401 },
    );
  try {
    const response = await fetch(
      `${backend}/api/seller-applications${method === "GET" ? "/mine" : ""}`,
      {
        method,
        cache: "no-store",
        signal: AbortSignal.timeout(8000),
        headers: {
          Authorization: `Bearer ${token}`,
          ...(method === "POST" ? { "Content-Type": "application/json" } : {}),
        },
        ...(method === "POST" ? { body: await request!.text() } : {}),
      },
    );
    const data = await response
      .json()
      .catch(() => ({
        success: false,
        message: "Invalid response from application service",
      }));
    return NextResponse.json(data, {
      status: response.status,
      headers: { "Cache-Control": "no-store" },
    });
  } catch {
    return NextResponse.json(
      { success: false, message: "Application service is unavailable" },
      { status: 503 },
    );
  }
}

export async function GET() {
  return forward("GET");
}


export async function POST(request: NextRequest) {
  return forward("POST", request);
}
