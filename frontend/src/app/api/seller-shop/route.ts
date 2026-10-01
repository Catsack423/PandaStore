import { cookies } from "next/headers";
import { NextResponse } from "next/server";

export async function GET() {
  const token = (await cookies()).get("auth_token")?.value;
  if (!token) return NextResponse.json({ success: false }, { status: 401 });
  const base = process.env.BACKEND_API_URL || "http://localhost:8080";
  const options = {
    cache: "no-store" as const,
    signal: AbortSignal.timeout(8000),
  };

  try {
    const meResponse = await fetch(`${base}/api/auth/me`, {
      ...options,
      headers: { Authorization: `Bearer ${token}` },
    });
    const me = await meResponse.json();

    if (!meResponse.ok || !me.success || me.data?.role !== "SELLER")
      return NextResponse.json({ success: false }, { status: 403 });
    const shopResponse = await fetch(
      `${base}/api/seller/shops/user/${me.data.userId}`,
      options,
    );
    
    const shop = await shopResponse.json();
    return NextResponse.json(shop, {
      status: shopResponse.status,
      headers: { "Cache-Control": "no-store" },
    });
  } catch {
    return NextResponse.json(
      { success: false, message: "Shop unavailable" },
      { status: 503 },
    );
  }
}
