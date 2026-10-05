"use server";

import Home from "@/components/Home";
import { Metadata } from "next";

import { getProducts } from "@/ServerAction/products";
import { useProductContext } from "../context/ProductContext";

 const metadata: Metadata = {
  title: "NextCommerce | Nextjs E-commerce template",
  description: "This is Home for NextCommerce Template",
  // other metadata
};

export default async function HomePage() {
 
  const response = await getProducts();
  console.log("loadingสำเร็จ",response);
  
  const initialProducts =
    response.success && response.data ? response.data : [];

  
  return (
    <>
      <Home initialProducts={initialProducts}></Home>
    </>
  );
}
