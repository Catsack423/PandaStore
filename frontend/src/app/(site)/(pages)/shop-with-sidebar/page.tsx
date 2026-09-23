import React from "react";
import ShopWithSidebar from "@/components/ShopWithSidebar";

import { Metadata } from "next";
import { getProducts } from "@/ServerAction/products";
import { ProductContextProvider } from "@/app/context/ProductContext";
import { FilterSidebarContextProvider } from "@/app/context/FilterSidebarContext";
import shopData from "@/components/Shop/shopData";

export const metadata: Metadata = {
  title: "Shop Page | NextCommerce Nextjs E-commerce template",
  description: "This is Shop Page for NextCommerce Template",
};

async function ShopWithSidebarPage() {
  const response = await getProducts();
  const initialProducts =
    response.success && response.data ? response.data : shopData;

  return (
    <main>
      <ProductContextProvider>
        <FilterSidebarContextProvider>
          <ShopWithSidebar initData={initialProducts} preview={!response.success} />
        </FilterSidebarContextProvider>
      </ProductContextProvider>
    </main>
  );
}

export default ShopWithSidebarPage;
