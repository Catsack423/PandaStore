"use client"

import React from "react";
import Hero from "./Hero";
import Categories from "./Categories";
import NewArrival from "./NewArrivals";
import PromoBanner from "./PromoBanner";
import BestSeller from "./BestSeller";
import CounDown from "./Countdown";
import Testimonials from "./Testimonials";
import Newsletter from "../Common/Newsletter";
import { ProductContextProvider } from "@/app/context/ProductContext";
import { Product } from "@/types/product";

const Home = ({initialProducts, preview = false}:{initialProducts:Product[]; preview?: boolean}) => {
  return (
    <main>
      {preview && <div role="status" className="mx-auto mt-[180px] max-w-[1170px] rounded-lg border border-blue/20 bg-white px-5 py-4 text-sm text-dark sm:mt-[160px]">Preview catalog: these are sample products from the template while the product API is unavailable.</div>}
      <Categories />
      <ProductContextProvider>
        <NewArrival  initialProducts={initialProducts} />
      </ProductContextProvider>
    </main>
  );
};

export default Home;
