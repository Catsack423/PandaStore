"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { z } from "zod";
import { useAuth } from "@/app/context/AuthContext";
import { sellerOrderSchema, sellerShopSchema, sellerActionSchemas, hasOrderStatusConflict, isSellerOrderPaid, type SellerOrderAction, type SellerOrderData } from "@/lib/sellerOrders";

export function useSellerOrders(orderId?: string) {
  const { user, isLoading } = useAuth();
  const router = useRouter();
  const eligible = !isLoading && user?.role === "SELLER" && user.status === "ACTIVE";
  const userId = user?.id;
  const [data, setData] = useState<SellerOrderData | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");
  const [acting, setActing] = useState(false);
  const [notice, setNotice] = useState<{ text: string; error: boolean } | null>(null);
  const mounted = useRef(false);
  const generation = useRef(0);
  const epoch = useRef(0);
  const request = useRef<AbortController | null>(null);
  const actionRequest = useRef<AbortController | null>(null);
  const actingRef = useRef(false);
  const resource = useRef<SellerOrderData | null>(null);
  const url = `/api/seller-orders${orderId ? `/${encodeURIComponent(orderId)}` : ""}`;

  const load = useCallback(async (force = false): Promise<boolean> => {
    if (!eligible || !userId || !mounted.current || (!force && (request.current || actingRef.current))) return false;
    if (force) request.current?.abort();
    const sequence = ++generation.current;
    const controller = new AbortController();
    request.current = controller;
    const timeout = window.setTimeout(() => controller.abort(), 45000);
    setRefreshing(true);
    try {
      const response = await fetch(url, { cache: "no-store", signal: controller.signal });
      const body = await response.json();
      if (!mounted.current || sequence !== generation.current) return false;
      if (response.status === 401 || response.status === 403) {
        resource.current = null; setData(null);
        router.replace(response.status === 401 ? `/signin?callbackUrl=${encodeURIComponent(orderId ? `/seller/${orderId}` : "/seller-dashboard")}` : "/seller-application");
      }
      if (!response.ok || !body.success) throw new Error(body.message || "Orders could not be loaded. Please try again.");
      const shop = sellerShopSchema.parse(body.data?.shop);
      const orders = orderId ? [sellerOrderSchema.parse(body.data?.order)] : z.array(sellerOrderSchema).parse(body.data?.orders);
      if (orders.some(order => order.sellerId !== shop.sellerId) || (orderId && orders[0].orderId !== Number(orderId))) throw new Error("Invalid shop order response");
      const next = { shop, orders };
      resource.current = next; setData(next); setError("");
      return true;
    } catch (failure) {
      if (mounted.current && sequence === generation.current) setError(failure instanceof z.ZodError ? "Invalid order data. Please retry." : controller.signal.aborted ? "Loading timed out. Please retry." : failure instanceof Error ? failure.message : "Orders could not be loaded. Please try again.");
      return false;
    } finally {
      window.clearTimeout(timeout);
      if (sequence === generation.current) {
        request.current = null;
        if (mounted.current) { setLoading(false); setRefreshing(false); }
      }
    }
  }, [eligible, orderId, router, url, userId]);

  useEffect(() => {
    if (!isLoading && !eligible) router.replace(user ? "/seller-application" : "/signin");
  }, [isLoading, eligible, user, router]);

  useEffect(() => {
    mounted.current = true;
    epoch.current++;
    actingRef.current = false; setActing(false); setRefreshing(false);
    resource.current = null; setData(null); setLoading(true); setError(""); setNotice(null);
    if (eligible) void load();
    const refreshVisible = () => { if (document.visibilityState === "visible") void load(); };
    const interval = window.setInterval(refreshVisible, 60000);
    window.addEventListener("focus", refreshVisible);
    document.addEventListener("visibilitychange", refreshVisible);
    const invalidate = () => { generation.current++; epoch.current++; };
    return () => {
      mounted.current = false; invalidate();
      request.current?.abort(); request.current = null; actionRequest.current?.abort();
      window.clearInterval(interval);
      window.removeEventListener("focus", refreshVisible);
      document.removeEventListener("visibilitychange", refreshVisible);
    };
  }, [load, eligible]);

  async function perform(action: SellerOrderAction, input: unknown) {
    const order = resource.current?.orders[0];
    if (!eligible || !orderId || !order || actingRef.current || request.current || error || hasOrderStatusConflict(order) || !isSellerOrderPaid(order)) return;
    if (order.orderStatus !== (action === "ship" ? "PREPARING" : "WAITING_SELLER_CONFIRM")) return;
    const parsed = sellerActionSchemas[action].safeParse(input);
    if (!parsed.success) { setNotice({ text: parsed.error.issues[0].message, error: true }); return; }
    actingRef.current = true; setActing(true); setNotice(null);
    const actionEpoch = epoch.current;
    const controller = new AbortController(); actionRequest.current = controller;
    const timeout = window.setTimeout(() => controller.abort(), 45000);
    let outcome = "";
    let failed = false;
    try {
      const response = await fetch(`${url}/${action}`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(parsed.data), signal: controller.signal });
      const body = await response.json();
      if (!response.ok || !body.success) throw new Error(body.message || "The action could not be confirmed.");
      outcome = action === "accept" ? "Order accepted. Preparing items." : action === "reject" ? "Order rejected." : "Order marked as shipped.";
    } catch (failure) {
      failed = true;
      outcome = controller.signal.aborted ? "The request timed out. Check the latest order status before trying again." : failure instanceof Error ? failure.message : "The action could not be confirmed. Check the latest order status.";
    } finally {
      window.clearTimeout(timeout);
      if (actionRequest.current === controller) actionRequest.current = null;
      // Always reconcile with the server; never repeat an uncertain POST automatically.
      if (mounted.current && epoch.current === actionEpoch) {
        const verified = await load(true);
        const latest = resource.current?.orders[0];
        if (verified && latest && latest.orderStatus !== order.orderStatus && failed) outcome += " The order status has changed; the latest data is now shown.";
        if (mounted.current && epoch.current === actionEpoch) { setNotice({ text: outcome, error: failed }); setActing(false); actingRef.current = false; }
      }
    }
  }

  return { data, loading: loading || !eligible, refreshing, error, refresh: () => void load(true), acting, notice, perform };
}
