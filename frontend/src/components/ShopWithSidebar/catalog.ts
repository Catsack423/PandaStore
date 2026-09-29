import type { Product } from "@/types/product";
import type { Category } from "@/types/category";

export type ProductSort = "latest" | "oldest" | "reviews" | "price-asc" | "price-desc";
export type ProductStyle = "grid" | "list";
export const PAGE_SIZE = 9;

export function catalogCategories(products: Product[], categories: Category[]): Category[] {
  const counts = new Map<number, number>();
  for (const product of products) {
    for (const id of Array.from(new Set(product.categoryIds ?? []))) {
      counts.set(id, (counts.get(id) ?? 0) + 1);
    }
  }
  const names = new Map(categories.map((category) => [category.id, category.name || category.title]));
  return Array.from(counts, ([id, count]) => ({
    id, name: names.get(id) || `Category ${id}`, products: count,
  })).sort((a, b) => a.name.localeCompare(b.name));
}

export function filterAndSortProducts(
  products: Product[],
  categoryIds: Set<number>,
  priceStart: number,
  priceEnd: number,
  sort: ProductSort,
): Product[] {
  const result = products.filter((product) => {
    const price = product.discountedPrice ?? product.price;
    return price >= priceStart && price <= priceEnd &&
      (categoryIds.size === 0 || (product.categoryIds ?? []).some((id) => categoryIds.has(id)));
  });
  const timestamp = (product: Product) => {
    const parsed = product.createdAt ? Date.parse(product.createdAt) : NaN;
    return Number.isFinite(parsed) ? parsed : 0;
  };
  return result.sort((a, b) => {
    switch (sort) {
      case "latest": return timestamp(b) - timestamp(a) || b.id - a.id;
      case "oldest": return timestamp(a) - timestamp(b) || a.id - b.id;
      case "reviews": return b.reviews - a.reviews || b.id - a.id;
      case "price-asc": return (a.discountedPrice ?? a.price) - (b.discountedPrice ?? b.price) || a.id - b.id;
      case "price-desc": return (b.discountedPrice ?? b.price) - (a.discountedPrice ?? a.price) || b.id - a.id;
    }
  });
}

export function paginateProducts(products: Product[], requestedPage: number) {
  const totalPages = Math.max(1, Math.ceil(products.length / PAGE_SIZE));
  const page = Math.min(Math.max(1, requestedPage), totalPages);
  const start = (page - 1) * PAGE_SIZE;
  return { page, totalPages, products: products.slice(start, start + PAGE_SIZE) };
}
