"use client";

import { useEffect, useMemo, useState } from "react";
import { X } from "lucide-react";
import type { Product } from "@/types/product";
import type { Category } from "@/types/category";
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

type Props = { products: Product[]; categories: Category[]; preview?: boolean };

export default function ShopWithSidebar({
  products,
  categories,
  preview = false,
}: Props) {
  const [style, setStyle] = useState<ProductStyle>("grid");
  
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const [sort, setSort] = useState<ProductSort>("latest");

  const { categoryIds, priceStart, priceEnd, resetFilters } =
    useFilterSidebarContext();
  const results = useMemo(
    () =>
      filterAndSortProducts(products, categoryIds, priceStart, priceEnd, sort),
    [products, categoryIds, priceStart, priceEnd, sort],
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
  const pagination = paginateProducts(
    results,
    pageSelection.results === results ? pageSelection.page : 1,
  );

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
          {preview && (
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
                    onClick={resetFilters}
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
                sort={sort}
                onSortChange={setSort}
                style={style}
                onStyleChange={setStyle}
                start={
                  results.length ? (pagination.page - 1) * PAGE_SIZE + 1 : 0
                }
                end={Math.min(pagination.page * PAGE_SIZE, results.length)}
                total={results.length}
                sidebarOpen={sidebarOpen}
                onOpenSidebar={() => setSidebarOpen(true)}
              />
              <ProductResults
                products={pagination.products}
                style={style}
                onReset={resetFilters}
              />
              <Pagination
                page={pagination.page}
                totalPages={pagination.totalPages}
                onPageChange={(page) => setPageSelection({ results, page })}
              />
            </div>
          </div>
        </div>
      </section>
    </>
  );
}
