"use client";

import { useCart } from "@/app/context/CartContext";
export function useProductAvailability(product: { id: number; stock?: number | null }, quantity = 1) {
  const { items, isLoading, isPending } = useCart();
  const inCart = items.find(item => item.id === product.id);
  const stock = inCart?.stock ?? product.stock;
  const remaining = Math.max(0, (stock ?? 0) - (inCart?.quantity || 0));
  return { stock, remaining, canAdd: !isLoading && !isPending && stock != null && remaining >= quantity };
}
export default function ProductStock({ product }: { product: { id: number; stock?: number | null } }) {
  const { stock, remaining } = useProductAvailability(product);
  return <p className={`mt-2 text-xs ${stock === 0 || remaining === 0 ? "text-red" : "text-dark-4"}`}>
    {stock == null ? "Stock unavailable" : stock === 0 ? "Out of stock" : remaining === 0 ? `All ${stock} available items are in your cart` : `${stock} in stock`}
  </p>;
}
