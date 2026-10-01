"use server";

import { ApiResponse } from "@/types/apiresponse";
import { Product, productSchema } from "@/types/product";
import { templateShopName } from "@/lib/templateShops";
import { templateCategoryIds } from "@/lib/templateCategories";
import type { Category } from "@/types/category";
import { z } from "zod";
import { cookies } from "next/headers";
import { revalidatePath } from "next/cache";
import { UTApi } from "uploadthing/server";
import { verifyProductImageRemoval } from "@/lib/productImageRemoval";

const backendProductSchema = z.object({
  productId: z.number().int(),
  sellerId: z.number().int().nullable(),
  sellerShopName: z.string().nullable(),
  name: z.string(),
  description: z.string().nullish(),
  price: z.number(),
  stock: z.number().int().nonnegative().nullable(),
  reviewCount: z.number().nullable(),
  averageRating: z.number().nullish(),
  imageUrls: z.array(z.string()).nullable(),
  status: z.string().nullable().optional(),
  categoryIds: z.array(z.number().int()).nullish(),
  createdAt: z.string().nullish(),
});

function imageUrl(url: string, base: string): string {
  // Demo products reuse the product artwork shipped with this storefront.
  if (/^\/images\/products\/product-[1-8]-(?:bg|sm)-[12]\.png$/.test(url))
    return url;
  if (
    url.startsWith("http://") ||
    url.startsWith("https://") ||
    url.startsWith("data:")
  )
    return url;
  return new URL(url.startsWith("/") ? url : `/${url}`, base).toString();
}

function mapBackendProducts(
  products: z.infer<typeof backendProductSchema>[],
  base: string,
): Product[] {
  return products.map((product) => {
    const images = (product.imageUrls || [])
      .filter(Boolean)
      .map((url) => imageUrl(url, base));
    const displayImages = images.length
      ? images
      : ["/images/products/product-placeholder.svg"];
    return {
      id: product.productId,
      categoryIds: product.categoryIds ?? [],
      createdAt: product.createdAt,
      stock: product.stock,
      status: product.status,
      title: product.name,
      description: product.description,
      averageRating: product.averageRating,
      price: product.price,
      discountedPrice: product.price,
      reviews: product.reviewCount || 0,
      sellerId: product.sellerId,
      sellerShopName: product.sellerShopName,
      imgs: { thumbnails: displayImages, previews: displayImages },
    };
  });
}

export type ProductSearch = {
  items: Product[];
  page: number;
  totalItems: number;
  totalPages: number;
};

export type ShopCatalogSummary = { maxPrice: number; categoryCounts: Record<string, number> };

export async function getShopCatalogSummary(): Promise<ShopCatalogSummary> {
  const base = process.env.BACKEND_API_URL || "http://localhost:8080";
  const response = await fetch(`${base}/api/products/catalog-summary`, {
    cache: "no-store",
    signal: AbortSignal.timeout(2500),
  });
  if (!response.ok) throw new Error(`Product summary returned ${response.status}`);
  const body = await response.json();
  if (!body?.success) throw new Error("Product summary failed");
  return z.object({ maxPrice: z.number(), categoryCounts: z.record(z.string(), z.number()) }).parse(body.data);
}

export async function searchProducts(params: {
  keyword?: string;
  categoryIds?: number[];
  minPrice?: number;
  maxPrice?: number;
  sort?: string;
  page?: number;
  size?: number;
}): Promise<ProductSearch> {
  const base = process.env.BACKEND_API_URL || "http://localhost:8080";
  const query = new URLSearchParams();
  if (params.keyword) query.set("keyword", params.keyword);
  params.categoryIds?.forEach((id) => query.append("categoryIds", String(id)));
  if (params.minPrice != null) query.set("minPrice", String(params.minPrice));
  if (params.maxPrice != null) query.set("maxPrice", String(params.maxPrice));
  if (params.sort) query.set("sort", params.sort);
  query.set("page", String(params.page ?? 0));
  query.set("size", String(params.size ?? 9));
  const response = await fetch(`${base}/api/products/search?${query}`, {
    cache: "no-store",
    signal: AbortSignal.timeout(5000),
  });
  if (!response.ok) throw new Error(`Product search returned ${response.status}`);
  const body = await response.json();
  if (!body?.success) throw new Error("Product search failed");
  const data = z.object({
    items: z.array(backendProductSchema),
    page: z.number().int(),
    totalItems: z.number().int(),
    totalPages: z.number().int(),
  }).parse(body.data);
  return { ...data, items: mapBackendProducts(data.items, base) };
}

export async function getProductById(
  productId: number,
): Promise<Product | null> {
  try {
    const base = process.env.BACKEND_API_URL || "http://localhost:8080";
    const response = await fetch(`${base}/api/products/${productId}`, {
      cache: "no-store",
      signal: AbortSignal.timeout(2500),
    });
    if (!response.ok) return null;
    const body = await response.json();
    if (!body?.success) return null;
    const data = backendProductSchema.parse(body.data);
    return mapBackendProducts([data], base)[0];
  } catch {
    return null;
  }
}

async function fetchBackendProducts(
  path: string,
): Promise<z.infer<typeof backendProductSchema>[]> {
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

export async function getSellerProducts(
  sellerId: number,
): Promise<Product[] | null> {
  try {
    const base = process.env.BACKEND_API_URL || "http://localhost:8080";
    const products = await fetchBackendProducts(
      `/api/products/seller/${sellerId}`,
    );
    return mapBackendProducts(
      products.filter(
        (product) => product.status === "ACTIVE" && (product.stock ?? 0) > 0,
      ),
      base,
    );
  } catch {
    return null;
  }
}

export async function getProducts(): Promise<ApiResponse<Product[]>> {
  try {
    return {
      success: true,
      message: "Products loaded",
      data: await getBackendProducts(),
      error: null,
    };
  } catch {
    // The template catalog remains browsable while the Spring API is offline.
  }

  try {
    const response = await fetch("http://localhost:5000/products", {
      cache: "no-store",
      signal: AbortSignal.timeout(2500),
    });
    if (!response.ok)
      throw new Error(`Template API returned ${response.status}`);
    const body = await response.json();
    if (!body?.success) throw new Error("Template API returned an error");
    const products = z.array(productSchema).parse(body.data?.products);
    return {
      success: true,
      message: "Template products loaded",
      data: products.map((product) => ({
        ...product,
        categoryIds: product.categoryIds ?? templateCategoryIds(product),
        sellerShopName: product.sellerShopName || templateShopName(product),
      })),
      error: null,
    };
  } catch (error) {
    const message =
      error instanceof Error ? error.message : "Product catalog unavailable";
    return { success: false, message, data: null, error: message };
  }
}

export async function getCategories(options?: { throwOnError?: boolean }): Promise<Category[]> {
  try {
    const base = process.env.BACKEND_API_URL || "http://localhost:8080";
    const response = await fetch(`${base}/api/categories`, {
      cache: "no-store",
      signal: AbortSignal.timeout(2500),
    });
    if (!response.ok) throw new Error("Categories are unavailable. Please try again.");
    const body = await response.json();
    if (!body?.success) throw new Error("Categories are unavailable. Please try again.");
    return z
      .array(
        z.object({ categoryId: z.number().int(), categoryName: z.string() }),
      )
      .parse(body.data)
      .map((category) => ({
        id: category.categoryId,
        name: category.categoryName,
      }));
  } catch (error) {
    if (options?.throwOnError) throw error;
    return [];
  }
}

async function requireSellerShop() {
  const token = (await cookies()).get("auth_token")?.value;
  if (!token) throw new Error("Please sign in first.");
  const base = process.env.BACKEND_API_URL || "http://localhost:8080";
  const options = { cache: "no-store" as const, headers: { Authorization: `Bearer ${token}` }, signal: AbortSignal.timeout(8000) };
  const meResponse = await fetch(`${base}/api/auth/me`, options);
  const me = await meResponse.json();
  if (!meResponse.ok || !me.success || me.data?.role !== "SELLER" || me.data?.status !== "ACTIVE")
    throw new Error("An active Seller account is required.");
  const shopResponse = await fetch(`${base}/api/seller/shops/user/${me.data.userId}`, options);
  const shop = await shopResponse.json();
  if (!shopResponse.ok || !shop.success || shop.data?.status !== "ACTIVE" || !Number.isSafeInteger(shop.data?.sellerId) || shop.data.sellerId < 1)
    throw new Error("An active shop is required to add products.");
  return { token, base, sellerId: shop.data.sellerId as number, userId: String(me.data.userId) };
}

export async function getSellerProductAccess(): Promise<boolean> {
  try { await requireSellerShop(); return true; } catch { return false; }
}

export async function removeSellerProductImage(input: { key: string; removalToken: string }): Promise<ApiResponse<null>> {
  try {
    const data = z.object({ key: z.string().min(1).max(512), removalToken: z.string().regex(/^[a-f0-9]{64}$/) }).parse(input);
    const { userId, sellerId } = await requireSellerShop();
    if (!verifyProductImageRemoval(data.key, userId, data.removalToken))
      return { success: false, message: "You can only remove product images you uploaded.", data: null, error: "FORBIDDEN" };
    // Cleanup must not delete images if a save completed while the page was closing.
    const products = await fetchBackendProducts(`/api/products/seller/${sellerId}`);
    const referenced = products.some(product => product.imageUrls?.some(url => {
      try { return decodeURIComponent(new URL(url).pathname.split("/").pop() || "") === data.key; } catch { return false; }
    }));
    if (referenced)
      return { success: false, message: "This image is already used by a saved product.", data: null, error: "IMAGE_IN_USE" };
    try {
      const result = await new UTApi().deleteFiles(data.key);
      if (!result.success) throw new Error("Deletion failed");
    } catch {
      return { success: false, message: "Unable to delete image from Cloud. Please try again.", data: null, error: "DELETE_FAILED" };
    }
    return { success: true, message: "Image removed from Cloud", data: null, error: null };
  } catch (error) {
    const message = error instanceof z.ZodError ? "Invalid image removal request." : error instanceof Error ? error.message : "Unable to remove image. Please try again.";
    return { success: false, message, data: null, error: message };
  }
}

const sellerProductInput = z.object({
  name: z.string().trim().min(1, "Enter a product name.").max(200),
  description: z.string(),
  price: z.string().regex(/^\d{1,10}(\.\d{1,2})?$/, "Enter a price with up to two decimal places.").refine(value => Number(value) >= 0.01 && Number(value) <= 9999999999.99, "Price must be between 0.01 and 9,999,999,999.99."),
  stock: z.number().int().min(1).max(2147483647),
  shippingInfo: z.string().max(255),
  categoryIds: z.array(z.number().int().positive().safe()).min(1, "Select at least one category."),
  imageUrls: z.array(z.string().url().max(255).refine(value => value.startsWith("https://"), "Upload a product image first.")).min(1).max(5),
});

export async function createSellerProduct(input: z.infer<typeof sellerProductInput>): Promise<ApiResponse<Product>> {
  try {
    const data = sellerProductInput.parse(input);
    const { token, base, sellerId } = await requireSellerShop();
    const response = await fetch(`${base}/api/products?sellerId=${sellerId}`, {
      method: "POST", headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: JSON.stringify({ ...data, price: Number(data.price), categoryIds: Array.from(new Set(data.categoryIds)) }),
      cache: "no-store", signal: AbortSignal.timeout(15000),
    });
    const body = await response.json();
    if (!response.ok || !body.success) throw new Error(body.message || "Unable to add product. Please try again.");
    const product = mapBackendProducts([backendProductSchema.parse(body.data)], base)[0];
    revalidatePath(`/shop/${sellerId}`);
    return { success: true, message: "Product added", data: product, error: null };
  } catch (error) {
    const message = error instanceof z.ZodError ? error.issues[0].message : error instanceof Error ? error.message : "Unable to add product. Please try again.";
    return { success: false, message, data: null, error: message };
  }
}
