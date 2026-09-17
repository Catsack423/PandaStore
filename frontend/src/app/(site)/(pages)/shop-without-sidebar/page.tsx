import React from "react";
import ShopWithoutSidebar from "@/components/ShopWithoutSidebar";
import { FilterSidebarContextProvider } from "@/app/context/FilterSidebarContext";

import { Metadata } from "next";
export const metadata: Metadata = {
  title: "Shop Page | NextCommerce Nextjs E-commerce template",
  description: "This is Shop Page for NextCommerce Template",
  // other metadata
};

const ShopWithoutSidebarPage = () => {
  return (
    <main>
      <FilterSidebarContextProvider>
        <ShopWithoutSidebar />
      </FilterSidebarContextProvider>
    </main>
  );
};

export default ShopWithoutSidebarPage;
