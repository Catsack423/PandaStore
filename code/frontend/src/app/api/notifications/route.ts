import { cookies } from "next/headers";
import { NextResponse } from "next/server";

const backend = process.env.BACKEND_API_URL || "http://localhost:8080";

export async function GET() {
  const token = (await cookies()).get("auth_token")?.value;
  if (!token) {
    return NextResponse.json(
      { success: false, message: "Please sign in to see notifications" },
      { status: 401 },
    );
  }

  const headers = { Authorization: `Bearer ${token}` };
  const options = { headers, cache: "no-store" as const, signal: AbortSignal.timeout(10000) };

  try {
    const meResponse = await fetch(`${backend}/api/auth/me`, options);
    const me = await meResponse.json().catch(() => null);
    if (!meResponse.ok || !me?.success) {
      return NextResponse.json(
        { success: false, message: "Session expired. Please sign in again." },
        { status: 401 },
      );
    }

    const userId = me.data?.userId;
    if (!Number.isSafeInteger(userId) || userId < 1) {
      return NextResponse.json(
        { success: false, message: "Could not identify your account" },
        { status: 502 },
      );
    }

    const response = await fetch(`${backend}/api/notifications/users/${userId}`, options);
    const body = await response.json().catch(() => null);
    return NextResponse.json(
      body ?? { success: false, message: "Invalid notification response" },
      {
        status: body ? response.status : 502,
        headers: { "Cache-Control": "no-store" },
      },
    );
  } catch {
    return NextResponse.json(
      { success: false, message: "Notifications are temporarily unavailable" },
      { status: 503 },
    );
  }
}
