"use server";

import { ApiResponse } from "@/types/apiresponse";
import { Product, productSchema } from "@/types/product";
import { templateShopName } from "@/lib/templateShops";
import { z } from "zod";

const backendProductSchema = z.object({
  productId: z.number().int(),
  sellerId: z.number().int().nullable(),
  sellerShopName: z.string().nullable(),
  name: z.string(),
  price: z.number(),
  reviewCount: z.number().nullable(),
  imageUrls: z.array(z.string()).nullable(),
  status: z.string().nullable().optional(),
});

function imageUrl(url: string, base: string): string {
  if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("data:")) return url;
  return new URL(url.startsWith("/") ? url : `/${url}`, base).toString();
}

function mapBackendProducts(products: z.infer<typeof backendProductSchema>[], base: string): Product[] {
  return products.map((product) => {
    const images = (product.imageUrls || []).filter(Boolean).map((url) => imageUrl(url, base));
    const displayImages = images.length ? images : ["/images/products/product-placeholder.svg"];
    return {
      id: product.productId,
      title: product.name,
      price: product.price,
      discountedPrice: product.price,
      reviews: product.reviewCount || 0,
      sellerId: product.sellerId,
      sellerShopName: product.sellerShopName,
      imgs: { thumbnails: displayImages, previews: displayImages },
    };
  });
}

async function fetchBackendProducts(path: string): Promise<z.infer<typeof backendProductSchema>[]> {
  const base = process.env.BACKEND_API_URL || "http://localhost:8080";
  const response = await fetch(`${base}${path}`, {
    cache: "no-store",
    signal: AbortSignal.timeout(2500),
  });
  if (!response.ok) throw new Error(`Product API returned ${response.status}`);
  const body = await response.json();
  if (!body?.success) throw new Error("Product API returned an error");
  return z.array(backendProductSchema).parse(body.data);
}

async function getBackendProducts(): Promise<Product[]> {
  const base = process.env.BACKEND_API_URL || "http://localhost:8080";
  return mapBackendProducts(await fetchBackendProducts("/api/products"), base);
}

export async function getSellerProducts(sellerId: number): Promise<Product[] | null> {
  try {
    const base = process.env.BACKEND_API_URL || "http://localhost:8080";
    const products = await fetchBackendProducts(`/api/products/seller/${sellerId}`);
    return mapBackendProducts(products.filter((product) => product.status === "ACTIVE"), base);
  } catch {
    return null;
  }
}

export async function getProducts(): Promise<ApiResponse<Product[]>> {
  try {
    return { success: true, message: "Products loaded", data: await getBackendProducts(), error: null };
  } catch {
    // The template catalog remains browsable while the Spring API is offline.
  }

  try {
    const response = await fetch("http://localhost:5000/products", {
      cache: "no-store",
      signal: AbortSignal.timeout(2500),
    });
    if (!response.ok) throw new Error(`Template API returned ${response.status}`);
    const body = await response.json();
    if (!body?.success) throw new Error("Template API returned an error");
    const products = z.array(productSchema).parse(body.data?.products);
    return {
      success: true,
      message: "Template products loaded",
      data: products.map((product) => ({
        ...product,
        sellerShopName: product.sellerShopName || templateShopName(product),
      })),
      error: null,
    };
  } catch (error) {
    const message = error instanceof Error ? error.message : "Product catalog unavailable";
    return { success: false, message, data: null, error: message };
  }
}
