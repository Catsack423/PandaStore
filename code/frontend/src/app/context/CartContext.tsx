"use client";

import { createContext, useContext, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { QueryClient, QueryClientProvider, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useDispatch } from "react-redux";
import toast, { Toaster } from "react-hot-toast";
import { useAuth } from "./AuthContext";
import { useAppSelector } from "@/redux/store";
import { addItemToCart as addGuest, removeItemFromCart as removeGuest, updateCartItemQuantity as updateGuest,
  removeAllItemsFromCart as clearGuest, type CartItem } from "@/redux/features/cart-slice";

type ServerItem = { cartItemId: number; productId: number; productName: string; sellerId: number;
  unitPrice: number; quantity: number; sellerShopName: string; imageUrls: string[]; stock: number | null };
type ServerCart = { cartId: number | null; items: ServerItem[]; removedProducts?: string[] };
type Operation = { type: "add"; item: CartItem } | { type: "remove"; id: number } |
  { type: "quantity"; id: number; quantity: number } | { type: "clear" };
type CartContextValue = { items: CartItem[]; totalPrice: number; count: number; isLoading: boolean; isPending: boolean;
  error: string; refreshCart: () => Promise<unknown>; orderPlaced: () => Promise<void>;
  addItemToCart: (item: CartItem) => void; removeItemFromCart: (id: number) => void;
  updateCartItemQuantity: (item: { id: number; quantity: number }) => void; removeAllItemsFromCart: () => void };
const CartContext = createContext<CartContextValue | null>(null);

class CartRequestError extends Error {
  constructor(message: string, readonly code?: string) { super(message); }
}

async function requestCart<T>(method = "GET", path = "", body?: object, signal?: AbortSignal): Promise<T> {
  const response = await fetch(`/api/cart${path ? `/${path}` : ""}`, { method, cache: "no-store", signal,
    ...(body ? { headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) } : {}) });
  const result = await response.json().catch(() => null);
  if (!response.ok || !result?.success) throw new CartRequestError(result?.message || "Could not update your cart", result?.error?.code);
  return result.data;
}

export function CartQueryProvider({ children }: { children: React.ReactNode }) {
  const [client] = useState(() => new QueryClient());
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

export function CartProvider({ children }: { children: React.ReactNode }) {
  const { user, isLoading: authLoading } = useAuth();
  const queryClient = useQueryClient();
  const dispatch = useDispatch();
  const guestItems = useAppSelector(state => state.cartReducer.items);
  const [reloading, setReloading] = useState(false);
  const reloadTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  useEffect(() => () => { if (reloadTimer.current) clearTimeout(reloadTimer.current); }, []);
  const key = ["cart", user?.id || "guest"];
  const customer = user?.role === "CUSTOMER";
  const channel = `pandastore-cart-updated:${user?.id || "guest"}`;
  const notifyOtherTabs = () => { try { localStorage.setItem(channel, `${Date.now()}:${Math.random()}`); } catch { /* Polling still keeps other tabs current. */ } };

  const activeUser = useRef(user?.id);
  activeUser.current = user?.id;
  async function synchronizeStock(signal?: AbortSignal) {
    const owner = activeUser.current;
    const data = await requestCart<ServerCart>("POST", "sync", undefined, signal);
    if (activeUser.current === owner && data.removedProducts?.length) {
      toast(`${data.removedProducts.join(", ")} removed from your cart because it is out of stock.`,
        { id: "cart-stock-removed", duration: 5000 });
    }
    return data;
  }
  const mutation = useMutation({
    mutationKey: key, scope: { id: `cart:${user?.id}` }, retry: false,
    onMutate: ({ owner }: { owner: string; operation: Operation }) => queryClient.cancelQueries({ queryKey: ["cart", owner] }),
    mutationFn: async ({ owner, operation }: { owner: string; operation: Operation }) => {
      if (activeUser.current !== owner) throw new Error("Your account changed. Please try again.");
      if (operation.type === "add") {
        await requestCart("POST", "items", { productId: operation.item.id, quantity: operation.item.quantity });
      } else if (operation.type === "clear") {
        await requestCart("DELETE", "items");
      } else {
        // Server item IDs differ from product IDs. Resolve again for each serialized mutation.
        const current = queryClient.getQueryData<ServerCart>(["cart", owner]) || await requestCart<ServerCart>();
        const item = current.items.find(item => item.productId === operation.id);
        if (!item) throw new Error("This product is no longer in your cart. Please refresh.");
        await requestCart(operation.type === "remove" ? "DELETE" : "PATCH", `items/${item.cartItemId}`,
          operation.type === "quantity" ? { quantity: operation.quantity } : undefined);
      }
      return { owner, data: await synchronizeStock() };
    },
    onSuccess: async ({ owner, data }) => {
      await queryClient.cancelQueries({ queryKey: ["cart", owner] });
      if (activeUser.current === owner) { queryClient.setQueryData(["cart", owner], data); notifyOtherTabs(); }
    },
    onError: (error, { owner }) => {
      if (activeUser.current !== owner) return;
      const availabilityError = error instanceof CartRequestError &&
        ["OUT_OF_STOCK", "INSUFFICIENT_STOCK", "PRODUCT_UNAVAILABLE"].includes(error.code || "");
      toast.error(availabilityError ? `${error.message} Refreshing the page…` : error.message,
        { id: "cart-update-error", duration: 4000 });
      void queryClient.invalidateQueries({ queryKey: ["cart", owner] }); notifyOtherTabs();
      if (availabilityError) {
        setReloading(true);
        reloadTimer.current = setTimeout(() => window.location.reload(), 1500);
      }
    },
  });
  const cart = useQuery({ queryKey: key, queryFn: ({ signal }) => synchronizeStock(signal),
    enabled: !authLoading && customer, staleTime: 0, gcTime: 0,
    refetchInterval: mutation.isPending ? false : 30000, refetchIntervalInBackground: false,
    refetchOnWindowFocus: "always", refetchOnReconnect: "always", retry: 1 });
  const refreshCart = useCallback(() => !authLoading && customer ? cart.refetch() : Promise.resolve(),
    [authLoading, customer, cart.refetch]);

  const previousUser = useRef(user?.id);
  useEffect(() => {
    const previous = previousUser.current;
    if (previous !== user?.id && previous) {
      void queryClient.cancelQueries({ queryKey: ["cart", previous] });
      queryClient.removeQueries({ queryKey: ["cart", previous] });
    }
    previousUser.current = user?.id;
  }, [user?.id, queryClient]);
  useEffect(() => {
    const update = (event: StorageEvent) => { if (customer && event.key === channel) void queryClient.invalidateQueries({ queryKey: ["cart", user!.id] }); };
    window.addEventListener("storage", update);
    return () => window.removeEventListener("storage", update);
  }, [customer, channel, user?.id, queryClient]);

  function act(operation: Operation) {
    if (mutation.isPending || reloading) return;
    if (authLoading) { toast.error("Please wait while your account loads"); return; }
    if (operation.type === "add" || operation.type === "quantity") {
      const current = items.find(item => item.id === (operation.type === "add" ? operation.item.id : operation.id));
      const stock = current?.stock ?? (operation.type === "add" ? operation.item.stock : undefined);
      const quantity = operation.type === "add" ? (current?.quantity || 0) + operation.item.quantity : operation.quantity;
      if (stock == null || quantity > stock || quantity <= 0) {
        toast.error(stock == null ? "Stock information is unavailable" : `Only ${stock} items are available`); return;
      }
    }
    if (!user) {
      if (operation.type === "add") dispatch(addGuest(operation.item));
      if (operation.type === "remove") dispatch(removeGuest(operation.id));
      if (operation.type === "quantity") dispatch(updateGuest({ id: operation.id, quantity: operation.quantity }));
      if (operation.type === "clear") dispatch(clearGuest());
      return;
    }
    if (!customer) { toast.error("Please use a customer account to shop"); return; }
    mutation.mutate({ owner: user.id, operation });
  }
  const items: CartItem[] = useMemo(() => user ? (customer ? (cart.data?.items || []).map(item => ({
    id: item.productId, title: item.productName, price: Number(item.unitPrice), discountedPrice: Number(item.unitPrice),
    quantity: item.quantity, stock: item.stock, sellerId: item.sellerId, sellerShopName: item.sellerShopName,
    imgs: { thumbnails: item.imageUrls || [], previews: item.imageUrls || [] },
  })) : []) : guestItems, [user?.id, customer, cart.data, guestItems]);
  async function orderPlaced() {
    queryClient.setQueryData<ServerCart>(key, { cartId: cart.data?.cartId || null, items: [] });
    dispatch(clearGuest()); notifyOtherTabs();
    await queryClient.invalidateQueries({ queryKey: key });
  }
  return <CartContext.Provider value={{ items, totalPrice: items.reduce((sum, item) => sum + item.discountedPrice * item.quantity, 0),
    count: items.reduce((sum, item) => sum + item.quantity, 0), isLoading: authLoading || (customer && cart.isPending),
    isPending: mutation.isPending || reloading, error: customer && cart.error ? cart.error.message : "", refreshCart, orderPlaced,
    addItemToCart: item => act({ type: "add", item }), removeItemFromCart: id => act({ type: "remove", id }),
    updateCartItemQuantity: item => act({ type: "quantity", ...item }), removeAllItemsFromCart: () => act({ type: "clear" })
  }}>
    {children}
  </CartContext.Provider>;
}

export function useCart() {
  const cart = useContext(CartContext);
  if (!cart) throw new Error("useCart must be used within CartProvider");
  return cart;
}
