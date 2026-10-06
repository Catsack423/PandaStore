"use client";

import { Product } from "@/types/product";
import { createContext, useContext, useReducer, ReactNode, Dispatch } from "react";

type State = {
  products: Product[];
  isLoading: boolean;
  error: string | null;
};

export type ProductActionType =
  | { type: "FETCH_START" }
  | { type: "FETCH_SUCCESS"; payload: Product[] }
  | { type: "FETCH_ERROR"; payload: string };

const initialState: State = {
  products: [],
  isLoading: false,
  error: null,
};

interface ProductContextType extends State {
  dispatch: Dispatch<ProductActionType>;
}

const ProductContext = createContext<ProductContextType | undefined>(undefined);

function productReducer(state: State, action: ProductActionType): State {
  switch (action.type) {
    case "FETCH_START":
      return { ...state, isLoading: true, error: null };
    case "FETCH_SUCCESS":
      return { ...state, isLoading: false, products: action.payload, error: null };
    case "FETCH_ERROR":
      return { ...state, isLoading: false, error: action.payload };
    default:
      return state;
  }
}

export function ProductContextProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(productReducer, initialState);

  return (
    <ProductContext.Provider value={{ ...state, dispatch }}>
      {children}
    </ProductContext.Provider>
  );
}

export function useProductContext() {
  const context = useContext(ProductContext);
  if (!context) {
    throw new Error("useProductContext must be used within a ProductContextProvider");
  }
  return context;
}