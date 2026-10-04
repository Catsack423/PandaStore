import { cookies } from "next/headers";
import { NextResponse } from "next/server";

export async function DELETE(
  _request: Request,
  context: { params: Promise<{ customerId: string; addressId: string }> },
) {
  const { customerId, addressId } = await context.params;
  if (!/^[1-9]\d*$/.test(customerId) || !/^[1-9]\d*$/.test(addressId)) {
    return NextResponse.json(
      { success: false, message: "Invalid address" },
      { status: 400 },
    );
  }

  const token = (await cookies()).get("auth_token")?.value;
  if (!token) {
    return NextResponse.json(
      { success: false, message: "Please sign in first" },
      { status: 401 },
    );
  }

  try {
    const backend = process.env.BACKEND_API_URL || "http://localhost:8080";
    const response = await fetch(
      `${backend}/api/customers/${customerId}/addresses/${addressId}`,
      {
        method: "DELETE",
        headers: { Authorization: `Bearer ${token}` },
        cache: "no-store",
        signal: AbortSignal.timeout(20000),
      },
    );
    const data = await response.json().catch(() => null);
    return NextResponse.json(data || { success: response.ok }, {
      status: response.status,
    });
  } catch {
    return NextResponse.json(
      { success: false, message: "Could not delete address" },
      { status: 503 },
    );
  }
}
