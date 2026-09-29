import { LayoutGrid, List, SlidersHorizontal } from "lucide-react";
import CustomSelect from "./CustomSelect";
import type { ProductSort, ProductStyle } from "./catalog";

const options = [
  { label: "Latest Products", value: "latest" },
  { label: "Most Reviewed", value: "reviews" },
  { label: "Oldest Products", value: "oldest" },
  { label: "Price: Low to High", value: "price-asc" },
  { label: "Price: High to Low", value: "price-desc" },
];

type Props = {
  sort: ProductSort; onSortChange: (sort: ProductSort) => void;
  style: ProductStyle; onStyleChange: (style: ProductStyle) => void;
  start: number; end: number; total: number;
  sidebarOpen: boolean; onOpenSidebar: () => void;
};

export default function ProductToolbar(props: Props) {
  return (
    <div className="mb-6 flex flex-wrap items-center justify-between gap-3 rounded-lg bg-white p-3 shadow-1">
      <div className="flex flex-wrap items-center gap-4">
        <CustomSelect options={options} value={props.sort} onChange={(value) => props.onSortChange(value as ProductSort)} />
        <p role="status" className="text-sm text-dark">Showing {props.start}–{props.end} of {props.total} products</p>
      </div>
      <div className="flex items-center gap-2">
        <button type="button" aria-controls="product-filters" aria-expanded={props.sidebarOpen}
          onClick={props.onOpenSidebar} className="flex h-9 items-center gap-1 rounded-md border border-gray-3 px-3 text-sm text-dark xl:hidden">
          <SlidersHorizontal className="size-4" /> Filters
        </button>
        {(["grid", "list"] as const).map((style) => {
          const Icon = style === "grid" ? LayoutGrid : List;
          return <button key={style} type="button" aria-label={`Show ${style} view`} aria-pressed={props.style === style}
            onClick={() => props.onStyleChange(style)}
            className={`flex size-9 items-center justify-center rounded-md border ${props.style === style ? "border-blue bg-blue text-white" : "border-gray-3 bg-gray-1 text-dark"}`}>
            <Icon className="size-4" />
          </button>;
        })}
      </div>
    </div>
  );
}
