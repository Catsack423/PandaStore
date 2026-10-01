import { Suspense } from "react";
import type { Metadata } from "next";
import Home from "@/components/Home";
import NewArrival from "@/components/Home/NewArrivals";
import NewArrivalsSkeleton from "@/components/Home/NewArrivals/Skeleton";
import { HOME_PRODUCT_LIMIT } from "@/components/Home/constants";
import { searchProducts } from "@/ServerAction/products";

export const metadata: Metadata = {
  title: "PandaStore | New Arrivals",
  description: "Discover the latest products from shops on PandaStore.",
};

async function LatestProducts() {
  try {
    const result = await searchProducts({ sort: "latest", page: 0, size: HOME_PRODUCT_LIMIT });
    return <NewArrival products={result.items} />;
  } catch {
    return <NewArrival products={[]} loadError />;
  }
}

export default function HomePage() {
  return (
    <Home>
      <Suspense fallback={<NewArrivalsSkeleton />}>
        <LatestProducts />
      </Suspense>
    </Home>
  );
}
