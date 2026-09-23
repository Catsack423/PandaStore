"use server";

import Home from "@/components/Home";
import { Metadata } from "next";

import { getProducts } from "@/ServerAction/products";
import shopData from "@/components/Shop/shopData";
import { useProductContext } from "../context/ProductContext";

 const metadata: Metadata = {
  title: "NextCommerce | Nextjs E-commerce template",
  description: "This is Home for NextCommerce Template",
  // other metadata
};

export default async function HomePage() {
 
  const response = await getProducts();
  
  const initialProducts =
    response.success && response.data ? response.data : shopData;

  
  return (
    <>
      <Home initialProducts={initialProducts} preview={!response.success}></Home>
    </>
  );
}
