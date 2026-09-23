import type { Metadata } from "next";
import PublicShop, { type PublicShopData } from "@/components/Seller/PublicShop";
import { getSellerProducts } from "@/ServerAction/products";

export const metadata: Metadata = { title: "Shop | PandaStore" };

async function getPublicShop(shopId: number): Promise<PublicShopData | null> {
  if (shopId === 0) return null;
  try {
    const base = process.env.BACKEND_API_URL || "http://localhost:8080";
    const response = await fetch(`${base}/api/seller/shops/${shopId}`, { cache: "no-store", signal: AbortSignal.timeout(2500) });
    if (!response.ok) return null;
    const body = await response.json();
    return body?.success && body?.data?.sellerId === shopId ? body.data as PublicShopData : null;
  } catch {
    return null;
  }
}

export default async function PublicShopPage({ params }: { params: Promise<{ shopId: string }> }) {
  const { shopId } = await params;
  const id = Number(shopId);
  const validId = Number.isSafeInteger(id) && id >= 0 && String(id) === shopId;
  const shop = validId ? await getPublicShop(id) : null;
  const products = shop?.status === "ACTIVE" ? await getSellerProducts(id) : null;
  return <PublicShop shopId={validId ? id : -1} shop={shop} products={products} />;
}
