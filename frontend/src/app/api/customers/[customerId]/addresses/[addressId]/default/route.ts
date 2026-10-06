import { cookies } from "next/headers";
import { NextResponse } from "next/server";

export async function PUT(_request: Request, context: { params: Promise<{ customerId: string; addressId: string }> }) {
  const { customerId, addressId } = await context.params;
  if (!/^[1-9]\d*$/.test(customerId) || !/^[1-9]\d*$/.test(addressId))
    return NextResponse.json({ success: false, message: "Invalid address" }, { status: 400 });
  const token = (await cookies()).get("auth_token")?.value;
  if (!token) return NextResponse.json({ success: false, message: "Please sign in first" }, { status: 401 });
  try {
    const response = await fetch(`${process.env.BACKEND_API_URL || "http://localhost:8080"}/api/customers/${customerId}/addresses/${addressId}/default`, {
      method: "PUT", headers: { Authorization: `Bearer ${token}` }, cache: "no-store", signal: AbortSignal.timeout(20000),
    });
    return NextResponse.json(await response.json(), { status: response.status });
  } catch {
    return NextResponse.json({ success: false, message: "Could not update default address" }, { status: 503 });
  }
}
