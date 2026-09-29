import type { Product } from "@/types/product";
import SingleGridItem from "../Shop/SingleGridItem";
import SingleListItem from "../Shop/SingleListItem";
import type { ProductStyle } from "./catalog";

export default function ProductResults({ products, style, onReset }: {
  products: Product[]; style: ProductStyle; onReset: () => void;
}) {
  if (products.length === 0) return (
    <div className="rounded-lg bg-white px-6 py-16 text-center shadow-1">
      <h2 className="font-semibold text-dark">No products found</h2>
      <p className="mt-2 text-sm text-dark-4">Try another category or price range.</p>
      <button type="button" onClick={onReset} className="mt-4 text-blue hover:underline">Clear filters</button>
    </div>
  );
  return (
    <div className={style === "grid" ? "grid grid-cols-1 gap-x-7.5 gap-y-9 sm:grid-cols-2 lg:grid-cols-3" : "flex flex-col gap-7.5"}>
      {products.map((product) => style === "grid"
        ? <SingleGridItem key={product.id} item={product} />
        : <SingleListItem key={product.id} item={product} />)}
    </div>
  );
}
