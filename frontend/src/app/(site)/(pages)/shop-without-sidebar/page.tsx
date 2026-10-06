import type { Metadata } from "next";
import ShopWithoutSidebar from "@/components/ShopWithoutSidebar";
import { getProducts } from "@/ServerAction/products";
import shopData from "@/components/Shop/shopData";

export const metadata: Metadata = { title: "Shop | PandaStore" };

export default async function ShopWithoutSidebarPage() {
  const response = await getProducts();
  return <main><ShopWithoutSidebar products={response.success ? response.data : shopData} preview={!response.success || response.message === "Template products loaded"} /></main>;
}
