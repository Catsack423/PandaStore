import ShopWithSidebar from "@/components/ShopWithSidebar";
import type { Metadata } from "next";
import { getProducts, getCategories, getShopCatalogSummary, searchProducts } from "@/ServerAction/products";
import { FilterSidebarContextProvider } from "@/app/context/FilterSidebarContext";
import shopData from "@/components/Shop/shopData";
import { templateCategories } from "@/lib/templateCategories";
import { catalogCategories } from "@/components/ShopWithSidebar/catalog";
import type { ProductSort } from "@/components/ShopWithSidebar/catalog";

export const metadata: Metadata = { title: "Shop | PandaStore" };

type Search = Record<string, string | string[] | undefined>;
const sorts = new Set(["latest", "oldest", "reviews", "price-asc", "price-desc"]);
const first = (value: string | string[] | undefined) => Array.isArray(value) ? value[0] : value;
const positive = (value: string | undefined) => {
  const number = Number(value);
  return value && Number.isInteger(number) && number > 0 ? number : null;
};
const price = (value: string | undefined) => {
  const number = Number(value);
  return value !== undefined && Number.isFinite(number) && number >= 0 ? number : null;
};

export default async function ShopWithSidebarPage({ searchParams }: { searchParams: Promise<Search> }) {
  const raw = await searchParams;
  const keyword = (first(raw.q) || "").trim();
  const ids = (Array.isArray(raw.categoryId) ? raw.categoryId : raw.categoryId ? [raw.categoryId] : [])
    .map(positive).filter((id): id is number => id !== null);
  const categoryIds = Array.from(new Set(ids));
  const minPrice = price(first(raw.minPrice));
  const maxPriceFilter = price(first(raw.maxPrice));
  const requestedPage = positive(first(raw.page)) ?? 1;
  const sort: ProductSort = sorts.has(first(raw.sort) || "") ? first(raw.sort)! as ProductSort : "latest";
  const activeSearch = Boolean(keyword || categoryIds.length || minPrice != null || maxPriceFilter != null || raw.sort);

  const [summary, apiCategories, search] = await Promise.all([
    getShopCatalogSummary().catch(() => null), getCategories(),
    searchProducts({ keyword, categoryIds, minPrice: minPrice ?? undefined,
      maxPrice: maxPriceFilter ?? undefined, sort, page: requestedPage - 1, size: 9 })
      .then((data) => ({ data, error: false }))
      .catch(() => ({ data: null, error: true })),
  ]);
  const effectivePage = search.data ? Math.min(requestedPage, Math.max(1, search.data.totalPages)) : requestedPage;
  const pageData = search.data && effectivePage !== requestedPage
    ? await searchProducts({ keyword, categoryIds, minPrice: minPrice ?? undefined,
      maxPrice: maxPriceFilter ?? undefined, sort, page: effectivePage - 1, size: 9 }).catch(() => null)
    : search.data;

  // The full catalog is needed only when the search API or summary is unavailable.
  const catalog = !summary || (!pageData && !activeSearch) ? await getProducts() : null;
  const fallbackProducts = catalog?.success && catalog.data ? catalog.data : shopData;
  const localCatalog = !pageData && !activeSearch;
  const previewNotice = localCatalog &&
    (!catalog?.success || catalog.message === "Template products loaded");
  const products = pageData?.items ?? (localCatalog ? fallbackProducts : []);
  const extraCategories = summary ? Object.keys(summary.categoryCounts)
    .map(Number).filter((id) => Number.isInteger(id) && id > 0 &&
      !apiCategories.some((category) => category.id === id))
    .map((id) => ({ id, name: `Category ${id}` })) : [];
  const categories = summary && !localCatalog
    ? catalogCategories([], [...apiCategories, ...extraCategories]).map((category) => ({ ...category,
        products: summary.categoryCounts[String(category.id)] ?? 0 }))
    : catalogCategories(
        catalog?.message === "Products loaded" || previewNotice ? fallbackProducts : [],
        previewNotice ? templateCategories : apiCategories,
      );
  const maxPrice = Math.max(1, Math.ceil(summary && !localCatalog ? summary.maxPrice :
    fallbackProducts.reduce((max, product) =>
      Math.max(max, product.discountedPrice ?? product.price), 0)));
  const filterKey = JSON.stringify({ categoryIds, minPrice, maxPriceFilter, maxPrice });

  return <main>
    <FilterSidebarContextProvider key={filterKey} maxPrice={maxPrice}
      initialCategoryIds={categoryIds} initialPriceStart={minPrice ?? 0}
      initialPriceEnd={maxPriceFilter ?? maxPrice}>
      <ShopWithSidebar products={products} categories={categories} preview={localCatalog}
        showPreviewNotice={previewNotice} searchError={!pageData && !localCatalog} pageData={pageData}
        requestedPage={requestedPage}
        criteria={{ keyword, categoryIds, minPrice, maxPrice: maxPriceFilter, sort, page: effectivePage }} />
    </FilterSidebarContextProvider>
  </main>;
}
