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

const Home = ({initialProducts}:{initialProducts:Product[]}) => {
  return (
    <main>
      <Categories />
      <ProductContextProvider>
        <NewArrival  initialProducts={initialProducts} />
      </ProductContextProvider>
    </main>
  );
};

export default Home;
