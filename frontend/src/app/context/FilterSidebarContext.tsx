"use client";

import { createContext, useContext, useReducer, type ReactNode } from "react";

type State = { categoryIds: Set<number>; priceStart: number; priceEnd: number; maxPrice: number };
type Action =
  | { type: "toggleCategory"; id: number }
  | { type: "setPriceRange"; start: number; end: number }
  | { type: "reset" };

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case "toggleCategory": {
      const categoryIds = new Set(state.categoryIds);
      if (categoryIds.has(action.id)) categoryIds.delete(action.id);
      else categoryIds.add(action.id);
      return { ...state, categoryIds };
    }
    case "setPriceRange":
      return { ...state, priceStart: action.start, priceEnd: action.end };
    case "reset":
      return { ...state, categoryIds: new Set(), priceStart: 0, priceEnd: state.maxPrice };
  }
}

type FilterContext = State & {
  toggleCategory: (id: number) => void;
  setPriceRange: (start: number, end: number) => void;
  resetFilters: () => void;
};
const FilterSidebarContext = createContext<FilterContext | undefined>(undefined);

export function FilterSidebarContextProvider({
  children, maxPrice, initialCategoryIds = [], initialPriceStart = 0, initialPriceEnd,
}: { children: ReactNode; maxPrice: number; initialCategoryIds?: number[];
  initialPriceStart?: number; initialPriceEnd?: number }) {
  const [state, dispatch] = useReducer(reducer, {
    categoryIds: new Set(initialCategoryIds), priceStart: initialPriceStart,
    priceEnd: initialPriceEnd ?? maxPrice, maxPrice,
  });
  return (
    <FilterSidebarContext.Provider value={{
      ...state,
      toggleCategory: (id) => dispatch({ type: "toggleCategory", id }),
      setPriceRange: (start, end) => dispatch({ type: "setPriceRange", start, end }),
      resetFilters: () => dispatch({ type: "reset" }),
    }}>
      {children}
    </FilterSidebarContext.Provider>
  );
}

export function useFilterSidebarContext() {
  const context = useContext(FilterSidebarContext);
  if (!context) throw new Error("Filters require a FilterSidebarContextProvider");
  return context;
}
