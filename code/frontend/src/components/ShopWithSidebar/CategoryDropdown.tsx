"use client";

import { useState } from "react";
import { ChevronDown } from "lucide-react";
import type { Category } from "@/types/category";
import { useFilterSidebarContext } from "@/app/context/FilterSidebarContext";

export default function CategoryDropdown({ categories }: { categories: Category[] }) {
  const [open, setOpen] = useState(true);
  const { categoryIds, toggleCategory } = useFilterSidebarContext();
  return (
    <div className="rounded-lg bg-white shadow-1">
      <button type="button" aria-expanded={open} aria-controls="category-options"
        onClick={() => setOpen(!open)}
        className="flex w-full items-center justify-between px-5 py-3 text-dark">
        Category <ChevronDown className={`size-5 ${open ? "rotate-180" : ""}`} />
      </button>
      {open && <div id="category-options" className="flex flex-col gap-3 px-5 py-5">
        {categories.length === 0 && <p className="text-sm text-dark-4">No categories available.</p>}
        {categories.map((category) => (
          <label key={category.id} className="flex cursor-pointer items-center gap-2 text-sm text-dark">
            <input type="checkbox" checked={categoryIds.has(category.id)}
              onChange={() => toggleCategory(category.id)} className="size-4 accent-blue" />
            <span className="flex-1">{category.name}</span>
            <span className="rounded-full bg-gray-2 px-2 text-xs">{category.products}</span>
          </label>
        ))}
      </div>}
    </div>
  );
}
