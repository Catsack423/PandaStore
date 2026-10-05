"use client";

import { Category } from "@/types/category";
import {
  createContext,
  useContext,
  useReducer,
  ReactNode,
  Dispatch,
} from "react";

type State = {
  category: Set<Category>;
  priceStart: number;
  priceEnd: number;
};

export type FilterSideBarActionType =
  | { type: "CLEAN ALL" }
  | { type: "ADD CATEGORY"; payload: Category }
  | { type: "REMOVE CATEGORY"; payload: Category }
  | { type: "ADD RANGE"; payload: { start: number; end: number } };

const initialState: State = {
  category: new Set<Category>(),
  priceStart: 0,
  priceEnd: 10000,
};

interface FilterSidebarContextType extends State {
  dispatch: Dispatch<FilterSideBarActionType>;
}

const FilterSidebarContext = createContext<
  FilterSidebarContextType | undefined
>(undefined);

function filterSidebarReducer(
  state: State,
  action: FilterSideBarActionType
): State {
  switch (action.type) {
    case "CLEAN ALL":
      return {
        ...state,
        category: new Set<Category>(),
        priceStart: 0,
        priceEnd: 10000,
      };
    case "ADD CATEGORY": {
      const newCategorySet = new Set<Category>(state.category);
      newCategorySet.forEach((item) => {
        const isSame =
          item === action.payload ||
          (item.id !== undefined &&
            action.payload.id !== undefined &&
            item.id === action.payload.id) ||
          (Boolean(item.name) &&
            Boolean(action.payload.name) &&
            item.name === action.payload.name) ||
          (Boolean(item.title) &&
            Boolean(action.payload.title) &&
            item.title === action.payload.title) ||
          (Boolean(item.name) &&
            Boolean(action.payload.title) &&
            item.name === action.payload.title) ||
          (Boolean(item.title) &&
            Boolean(action.payload.name) &&
            item.title === action.payload.name);
        if (isSame) {
          newCategorySet.delete(item);
        }
      });
      newCategorySet.add(action.payload);
      return {
        ...state,
        category: newCategorySet,
      };
    }
    case "REMOVE CATEGORY": {
      const newCategorySet = new Set<Category>();
      state.category.forEach((item) => {
        const isSame =
          item === action.payload ||
          (item.id !== undefined &&
            action.payload.id !== undefined &&
            item.id === action.payload.id) ||
          (Boolean(item.name) &&
            Boolean(action.payload.name) &&
            item.name === action.payload.name) ||
          (Boolean(item.title) &&
            Boolean(action.payload.title) &&
            item.title === action.payload.title) ||
          (Boolean(item.name) &&
            Boolean(action.payload.title) &&
            item.name === action.payload.title) ||
          (Boolean(item.title) &&
            Boolean(action.payload.name) &&
            item.title === action.payload.name);
        if (!isSame) {
          newCategorySet.add(item);
        }
      });
      return {
        ...state,
        category: newCategorySet,
      };
    }
    case "ADD RANGE":
      return {
        ...state,
        priceStart: action.payload.start,
        priceEnd: action.payload.end,
      };
    default:
      return state;
  }
}

export function FilterSidebarContextProvider({
  children,
}: {
  children: ReactNode;
}) {
  const [state, dispatch] = useReducer(filterSidebarReducer, initialState);

  return (
    <FilterSidebarContext.Provider value={{ ...state, dispatch }}>
      {children}
    </FilterSidebarContext.Provider>
  );
}

export function useFilterSidebarContext() {
  const context = useContext(FilterSidebarContext);
  if (!context) {
    throw new Error(
      "useFilterSidebarContext must be used within a FilterSidebarContextProvider"
    );
  }
  return context;
}
