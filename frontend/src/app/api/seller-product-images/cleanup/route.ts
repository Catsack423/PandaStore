import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";
import { removeSellerProductImage } from "@/ServerAction/products";

export async function POST(request: NextRequest) {
  if (request.headers.get("origin") !== request.nextUrl.origin)
    return NextResponse.json({ success: false }, { status: 403 });
  try {
    const images = z.array(z.object({ key: z.string().min(1).max(512), removalToken: z.string().regex(/^[a-f0-9]{64}$/) })).min(1).max(5).parse(await request.json());
    const results = [];
    for (const image of images) results.push({ key: image.key, ...(await removeSellerProductImage(image)) });
    return NextResponse.json({ results }, { headers: { "Cache-Control": "no-store" } });
  } catch {
    return NextResponse.json({ success: false, message: "Invalid cleanup request" }, { status: 400 });
  }
}
