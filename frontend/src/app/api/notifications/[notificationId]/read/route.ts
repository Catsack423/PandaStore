import { cookies } from "next/headers";
import { NextResponse } from "next/server";

const backend = process.env.BACKEND_API_URL || "http://localhost:8080";

export async function PATCH(
  _request: Request,
  { params }: { params: Promise<{ notificationId: string }> },
) {
  const { notificationId } = await params;
  if (!/^[1-9]\d*$/.test(notificationId)) {
    return NextResponse.json({ success: false, message: "Invalid notification" }, { status: 400 });
  }

  const token = (await cookies()).get("auth_token")?.value;
  if (!token) {
    return NextResponse.json(
      { success: false, message: "Please sign in to see notifications" },
      { status: 401 },
    );
  }

  try {
    const response = await fetch(`${backend}/api/notifications/${notificationId}/read`, {
      method: "PATCH",
      headers: { Authorization: `Bearer ${token}` },
      cache: "no-store",
      signal: AbortSignal.timeout(10000),
    });
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
      { success: false, message: "Could not update notification" },
      { status: 503 },
    );
  }
}
