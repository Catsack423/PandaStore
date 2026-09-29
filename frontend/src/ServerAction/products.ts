"use server";

import { ApiResponse } from "@/types/apiresponse";
import { Product, productSchema } from "@/types/product";
import { templateShopName } from "@/lib/templateShops";
import { templateCategoryIds } from "@/lib/templateCategories";
import type { Category } from "@/types/category";
import { z } from "zod";

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

export async function getCategories(): Promise<Category[]> {
  try {
    const base = process.env.BACKEND_API_URL || "http://localhost:8080";
    const response = await fetch(`${base}/api/categories`, {
      cache: "no-store",
      signal: AbortSignal.timeout(2500),
    });
    if (!response.ok) return [];
    const body = await response.json();
    if (!body?.success) return [];
    return z
      .array(
        z.object({ categoryId: z.number().int(), categoryName: z.string() }),
      )
      .parse(body.data)
      .map((category) => ({
        id: category.categoryId,
        name: category.categoryName,
      }));
  } catch {
    return [];
  }
}
