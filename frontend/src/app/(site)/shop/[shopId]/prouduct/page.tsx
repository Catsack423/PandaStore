import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { z } from "zod";
import { getCategories, getProductById } from "@/ServerAction/products";
import ProductDetail from "@/components/Shop/ProductDetail";
import shopData from "@/components/Shop/shopData";
import { templateCategories } from "@/lib/templateCategories";

export const metadata: Metadata = { title: "Product details | PandaStore" };

const reviewSchema = z.object({
  reviewId: z.number(),
  customerName: z.string().nullish(),
  rating: z.number().min(1).max(5),
  comment: z.string().nullish(),
  replyMessage: z.string().nullish(),
  createdAt: z.string().nullish(),
});

export type ProductReview = z.infer<typeof reviewSchema>;

async function getReviews(productId: number): Promise<ProductReview[] | null> {
  try {
    const base = process.env.BACKEND_API_URL || "http://localhost:8080";
    const response = await fetch(`${base}/api/reviews/product/${productId}`, { cache: "no-store", signal: AbortSignal.timeout(2500) });
    if (!response.ok) return null;
    const body = await response.json();
    return body?.success ? z.array(reviewSchema).parse(body.data) : null;
  } catch {
    return null;
  }
}

export default async function ProductPage({ params, searchParams }: {
  params: Promise<{ shopId: string }>;
  searchParams: Promise<{ id?: string | string[] }>;
}) {
  const [{ shopId }, { id }] = await Promise.all([params, searchParams]);
  const sellerId = Number(shopId);
  const productId = typeof id === "string" ? Number(id) : NaN;
  if (!Number.isSafeInteger(sellerId) || sellerId < 0 || String(sellerId) !== shopId ||
      !Number.isSafeInteger(productId) || productId <= 0 || String(productId) !== id) notFound();

  const [backendProduct, categories, reviews] = await Promise.all([
    getProductById(productId), getCategories(), getReviews(productId),
  ]);
  const sampleProduct = sellerId === 0 && !backendProduct ? shopData.find((item) => item.id === productId) : null;
  const product = backendProduct || sampleProduct;
  if (!product || product.status && product.status !== "ACTIVE" ||
      (product.sellerId ?? 0) !== sellerId) notFound();

  const categoryNames = (product.categoryIds ?? []).map((categoryId) =>
    (backendProduct ? categories : templateCategories).find((category) => category.id === categoryId)?.name || `Category ${categoryId}`,
  );
  return <ProductDetail product={product} categoryNames={categoryNames} reviews={backendProduct ? reviews : null} sample={!!sampleProduct} />;
}
