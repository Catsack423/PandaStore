import Link from "next/link";
import { Store } from "lucide-react";
import type { Product } from "@/types/product";

export default function ProductStore({ product }: { product: Product }) {
  const name = product.sellerShopName?.trim();
  const content = <><Store aria-hidden="true" className="size-4 shrink-0" /><span className="truncate">{name || "Store unavailable"}</span></>;

  return <div className="mt-2 flex min-w-0 items-center text-sm text-dark-4">
    {name && product.sellerId != null && product.sellerId > 0
      ? <Link href={`/shop/${product.sellerId}`} className="inline-flex min-w-0 items-center gap-1.5 hover:text-blue focus-visible:rounded focus-visible:outline focus-visible:outline-2 focus-visible:outline-blue" aria-label={`Visit ${name} shop`}>{content}</Link>
      : <span className="inline-flex min-w-0 items-center gap-1.5" title={name ? "Sample store" : undefined}>{content}</span>}
  </div>;
}
