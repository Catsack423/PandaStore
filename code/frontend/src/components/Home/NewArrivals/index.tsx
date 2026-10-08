"use client";

import { useTransition } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/app/context/AuthContext";
import SingleGridItem from "@/components/Shop/SingleGridItem";
import type { Product } from "@/types/product";
import { HOME_PRODUCT_LIMIT } from "../constants";
import NewArrivalsSkeleton from "./Skeleton";

export default function NewArrival({ products, loadError = false }: { products: Product[]; loadError?: boolean }) {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();
  const { user, isLoading } = useAuth();
  const readOnly = isLoading || (user != null && user.role !== "CUSTOMER");

  if (isPending) return <NewArrivalsSkeleton />;

  if (loadError) return (
    <div role="alert" className="rounded-lg border border-gray-3 bg-gray-1 px-6 py-16 text-center">
      <h2 className="font-semibold text-dark">Could not load products</h2>
      <p className="mt-2 text-sm text-dark-4">Try again to load the latest arrivals.</p>
      <button type="button" onClick={() => startTransition(() => router.refresh())} className="mt-5 rounded-md bg-blue px-6 py-2.5 text-sm font-medium text-white hover:bg-blue-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue focus-visible:ring-offset-2">
        Retry
      </button>
    </div>
  );

  if (!products.length) return (
    <div role="status" className="rounded-lg border border-gray-3 bg-gray-1 px-6 py-16 text-center">
      <h2 className="font-semibold text-dark">No products available yet</h2>
      <p className="mt-2 text-sm text-dark-4">New products will appear here when shops publish them.</p>
    </div>
  );

  return (
    <div className="grid grid-cols-1 gap-x-7.5 gap-y-9 sm:grid-cols-2 xl:grid-cols-4">
      {products.slice(0, HOME_PRODUCT_LIMIT).map(product => (
        <SingleGridItem key={product.id} item={product} readOnly={readOnly} showImageSkeleton />
      ))}
    </div>
  );
}
