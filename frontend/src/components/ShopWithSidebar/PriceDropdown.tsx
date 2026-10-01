"use client";

import { useState } from "react";
import { ChevronDown } from "lucide-react";
import RangeSlider from "react-range-slider-input";
import "react-range-slider-input/dist/style.css";
import { useFilterSidebarContext } from "@/app/context/FilterSidebarContext";

export default function PriceDropdown() {
  const [open, setOpen] = useState(true);
  const { priceStart, priceEnd, maxPrice, setPriceRange } = useFilterSidebarContext();
  return (
    <div className="rounded-lg bg-white shadow-1">
      <button type="button" aria-expanded={open} aria-controls="price-options"
        onClick={() => setOpen(!open)}
        className="flex w-full items-center justify-between px-5 py-3 text-dark">
        Price <ChevronDown className={`size-5 ${open ? "rotate-180" : ""}`} />
      </button>
      {open && <div id="price-options" className="p-6">
        <RangeSlider min={0} max={maxPrice} step="any" value={[priceStart, priceEnd]}
          onInput={([start, end]: [number, number]) => setPriceRange(Math.floor(start), Math.ceil(end))} />
        <div className="mt-4 flex justify-between text-sm text-dark">
          <span>${priceStart.toLocaleString()}</span>
          <span>${priceEnd.toLocaleString()}</span>
        </div>
      </div>}
    </div>
  );
}
