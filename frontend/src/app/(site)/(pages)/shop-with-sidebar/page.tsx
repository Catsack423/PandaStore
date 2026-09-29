import React from "react";
import ShopWithSidebar from "@/components/ShopWithSidebar";

import { Metadata } from "next";
import { getProducts, getCategories } from "@/ServerAction/products";
import { FilterSidebarContextProvider } from "@/app/context/FilterSidebarContext";
import shopData from "@/components/Shop/shopData";
import { templateCategories } from "@/lib/templateCategories";
import { catalogCategories } from "@/components/ShopWithSidebar/catalog";

export const metadata: Metadata = {
  title: "Shop Page | NextCommerce Nextjs E-commerce template",
  description: "This is Shop Page for NextCommerce Template",
};

async function ShopWithSidebarPage() {
  const [response, apiCategories] = await Promise.all([
    getProducts(),
    getCategories(),
  ]);
  const initialProducts =
    response.success && response.data ? response.data : shopData;
  const preview =
    !response.success || response.message === "Template products loaded";
  const categories = catalogCategories(
    initialProducts,
    preview ? templateCategories : apiCategories,
  );
  const maxPrice = Math.max(
    1,
    Math.ceil(
      initialProducts.reduce(
        (max, product) =>
          Math.max(max, product.discountedPrice ?? product.price),
        0,
      ),
    ),
  );

  return (
    <main>
      <FilterSidebarContextProvider key={maxPrice} maxPrice={maxPrice}>
        <ShopWithSidebar
          products={initialProducts}
          categories={categories}
          preview={preview}
        />
      </FilterSidebarContextProvider>
    </main>
  );
}

export default ShopWithSidebarPage;
