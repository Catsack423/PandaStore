import type { Product } from "@/types/product";

export function productUrl(product: Pick<Product, "id" | "sellerId">): string {
  return `/shop/${product.sellerId && product.sellerId > 0 ? product.sellerId : 0}/prouduct?id=${product.id}`;
}
