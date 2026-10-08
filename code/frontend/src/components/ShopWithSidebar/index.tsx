"use client";

import { useDeferredValue, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { X } from "lucide-react";
import type { Product } from "@/types/product";
import type { Category } from "@/types/category";
import type { ProductSearch } from "@/ServerAction/products";
import { useFilterSidebarContext } from "@/app/context/FilterSidebarContext";
import Breadcrumb from "../Common/Breadcrumb";
import CategoryDropdown from "./CategoryDropdown";
import PriceDropdown from "./PriceDropdown";
import ProductToolbar from "./ProductToolbar";
import ProductResults from "./ProductResults";
import Pagination from "./Pagination";
import {
  filterAndSortProducts,
  paginateProducts,
  PAGE_SIZE,
  type ProductSort,
  type ProductStyle,
} from "./catalog";

type Criteria = { keyword: string; categoryIds: number[]; minPrice: number | null;
  maxPrice: number | null; sort: ProductSort; page: number };
type Props = { products: Product[]; categories: Category[]; preview?: boolean;
  showPreviewNotice?: boolean; searchError?: boolean; pageData?: ProductSearch | null;
  requestedPage: number; criteria: Criteria };

function shopUrl(criteria: Criteria) {
  const query = new URLSearchParams();
  if (criteria.keyword) query.set("q", criteria.keyword);
  criteria.categoryIds.forEach((id) => query.append("categoryId", String(id)));
  if (criteria.minPrice != null) query.set("minPrice", String(criteria.minPrice));
  if (criteria.maxPrice != null) query.set("maxPrice", String(criteria.maxPrice));
  if (criteria.sort !== "latest") query.set("sort", criteria.sort);
  if (criteria.page > 1) query.set("page", String(criteria.page));
  return `/shop-with-sidebar${query.size ? `?${query}` : ""}`;
}

export default function ShopWithSidebar({
  products,
  categories,
  preview = false,
  showPreviewNotice = false,
  searchError = false,
  pageData,
  requestedPage,
  criteria,
}: Props) {
  const router = useRouter();
  useEffect(() => {
    if (requestedPage !== criteria.page) router.replace(shopUrl(criteria));
  }, [requestedPage, criteria.page, router]);
  const deferredPage = useDeferredValue(pageData);
  const [style, setStyle] = useState<ProductStyle>("grid");
  
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const { categoryIds, priceStart, priceEnd, maxPrice, resetFilters } =
    useFilterSidebarContext();
  const selection = useMemo(() => Array.from(categoryIds).sort((a, b) => a - b), [categoryIds]);
  const selectionKey = selection.join(",");
  const criteriaKey = [...criteria.categoryIds].sort((a, b) => a - b).join(",");
  useEffect(() => {
    if (preview) return;
    const min = priceStart > 0 ? priceStart : null;
    const max = priceEnd < maxPrice ? priceEnd : null;
    if (selectionKey === criteriaKey && min === criteria.minPrice && max === criteria.maxPrice) return;
    const timer = setTimeout(() => router.push(shopUrl({ ...criteria, categoryIds: selection,
      minPrice: min, maxPrice: max, page: 1 })), 300);
    return () => clearTimeout(timer);
  }, [selectionKey, criteriaKey, priceStart, priceEnd, maxPrice,
    criteria.minPrice, criteria.maxPrice, preview, router]);
  const changeSort = (sort: ProductSort) => router.push(shopUrl({ ...criteria, sort, page: 1 }));
  const clearFilters = () => {
    resetFilters();
    if (!preview) router.push(shopUrl({ ...criteria, keyword: "", categoryIds: [],
      minPrice: null, maxPrice: null, page: 1 }));
  };
  const results = useMemo(
    () =>
      preview ? filterAndSortProducts(products, categoryIds, priceStart, priceEnd, criteria.sort) : [],
    [preview, products, categoryIds, priceStart, priceEnd, criteria.sort],
  );

  // A page selection only applies to the results it was made for.
  // New filters, sorting, or server data start at page one.
  const [pageSelection, setPageSelection] = useState<{
    results: Product[] | null;
    page: number;
  }>({
    results: null,
    page: 1,
  });
  const pagination = paginateProducts(results,
    pageSelection.results === results ? pageSelection.page : criteria.page);
  const shownProducts = preview ? pagination.products : deferredPage?.items ?? [];
  const currentPage = preview ? pagination.page : (deferredPage?.page ?? 0) + 1;
  const totalPages = preview ? pagination.totalPages : deferredPage?.totalPages ?? 0;
  const totalItems = preview ? results.length : deferredPage?.totalItems ?? 0;
  useEffect(() => {
    if (preview && requestedPage !== currentPage) {
      router.replace(shopUrl({ ...criteria, page: currentPage }));
    }
  }, [preview, requestedPage, currentPage, router]);
  const changePage = (page: number) => {
    if (preview) setPageSelection({ results, page });
    router.push(shopUrl({ ...criteria, page }));
  };

  useEffect(() => {
    if (!sidebarOpen) return;
    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") setSidebarOpen(false);
    };
    document.addEventListener("keydown", handleEscape);
    return () => document.removeEventListener("keydown", handleEscape);
  }, [sidebarOpen]);

  return (
    <>
      <Breadcrumb
        title="Explore All Products"
        pages={["shop", "/", "shop with sidebar"]}
      />
      <section className="relative overflow-hidden bg-[#f3f4f6] pb-20 pt-5 lg:pt-20 xl:pt-28">
        <div className="mx-auto w-full max-w-[1170px] px-4 sm:px-8 xl:px-0">
          {showPreviewNotice && (
            <div
              role="status"
              className="mb-6 rounded-lg border border-blue/20 bg-white px-5 py-4 text-sm text-dark"
            >
              Preview catalog: these are sample products while the product API
              is unavailable.
            </div>
          )}
          <div className="flex gap-7.5">
            {sidebarOpen && (
              <button
                type="button"
                aria-label="Close filters"
                onClick={() => setSidebarOpen(false)}
                className="fixed inset-0 z-[10000] bg-dark/70 xl:hidden"
              />
            )}
            <aside
              id="product-filters"
              aria-label="Product filters"
              className={`fixed left-0 top-0 z-[10001] w-full max-w-[310px] duration-200 xl:static xl:z-1 xl:max-w-[270px] xl:translate-x-0 ${sidebarOpen ? "h-screen translate-x-0 overflow-y-auto bg-white p-5 xl:h-auto xl:overflow-visible xl:bg-transparent xl:p-0" : "invisible -translate-x-full xl:visible"}`}
            >
              <div className="mb-5 flex items-center justify-between xl:hidden">
                <h2 className="font-semibold text-dark">Filters</h2>
                <button
                  type="button"
                  aria-label="Close filters"
                  onClick={() => setSidebarOpen(false)}
                  className="flex size-9 items-center justify-center rounded-md hover:bg-gray-1"
                >
                  <X className="size-5" />
                </button>
              </div>
              <div className="flex flex-col gap-6">
                <div className="flex items-center justify-between rounded-lg bg-white px-5 py-4 shadow-1">
                  <p>Filters:</p>
                  <button
                    type="button"
                    onClick={clearFilters}
                    className="text-blue"
                  >
                    Clear all
                  </button>
                </div>
                <CategoryDropdown categories={categories} />
                <PriceDropdown />
              </div>
            </aside>
            <div className="min-w-0 w-full xl:max-w-[870px]">
              <ProductToolbar
                sort={criteria.sort}
                onSortChange={changeSort}
                style={style}
                onStyleChange={setStyle}
                start={
                  totalItems ? (currentPage - 1) * PAGE_SIZE + 1 : 0
                }
                end={Math.min(currentPage * PAGE_SIZE, totalItems)}
                total={totalItems}
                sidebarOpen={sidebarOpen}
                onOpenSidebar={() => setSidebarOpen(true)}
              />
              {searchError ? <div role="alert" className="rounded-lg bg-white px-6 py-12 text-center shadow-1">
                <p className="font-semibold text-dark">Could not load products</p>
                <button type="button" onClick={() => router.refresh()} className="mt-3 text-blue hover:underline">Try again</button>
              </div> : <>
                {deferredPage !== pageData && <p role="status" className="mb-3 text-sm text-dark-4">Updating products…</p>}
                <ProductResults products={shownProducts} style={style} onReset={clearFilters} />
                <Pagination page={currentPage} totalPages={totalPages} totalItems={totalItems}
                  onPageChange={changePage} />
              </>}
            </div>
          </div>
        </div>
      </section>
    </>
  );
}
