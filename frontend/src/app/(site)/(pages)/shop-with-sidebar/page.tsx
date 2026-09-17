import React from "react";
import ShopWithSidebar from "@/components/ShopWithSidebar";

import { Metadata } from "next";
import { getProducts } from "@/ServerAction/products";
import { ProductContextProvider } from "@/app/context/ProductContext";
import { FilterSidebarContextProvider } from "@/app/context/FilterSidebarContext";

export const metadata: Metadata = {
  title: "Shop Page | NextCommerce Nextjs E-commerce template",
  description: "This is Shop Page for NextCommerce Template",
};

async function ShopWithSidebarPage() {
  const response = await getProducts();
  console.log("loadingสำเร็จ", response);
  const initialProducts =
    response.success && response.data ? response.data : [];

  return (
    <main>
      <ProductContextProvider>
        <FilterSidebarContextProvider>
          <ShopWithSidebar initData={initialProducts} />
        </FilterSidebarContextProvider>
      </ProductContextProvider>
    </main>
  );
}

export default ShopWithSidebarPage;
