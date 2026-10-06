"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { CheckCircle2, Package, Store, Truck } from "lucide-react";
import Breadcrumb from "@/components/Common/Breadcrumb";
import ProductImage from "@/components/Common/ProductImage";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useAuth } from "@/app/context/AuthContext";
import { useCart } from "@/app/context/CartContext";
import DeliveryAddress from "./DeliveryAddress";
import ShippingMethod from "./ShippingMethod";
import { CheckoutData, CheckoutItem, PlacedOrder, checkoutApi, methodLabel, money } from "./api";

export default function Checkout() {
  const { user, isLoading: authLoading } = useAuth();
  const { items, isLoading: cartLoading, isPending: cartPending, orderPlaced } = useCart();
  const [metadata, setData] = useState<CheckoutData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [addressId, setAddressId] = useState<number | null>(null);
  const [methods, setMethods] = useState<Record<number, string>>({});
  const [fees, setFees] = useState<Record<number, Record<string, number>>>({});
  const [quoting, setQuoting] = useState(false);
  const [quoteError, setQuoteError] = useState("");
  const [payment, setPayment] = useState("PROMPTPAY");
  const [submitting, setSubmitting] = useState(false);
  const [placedOrder, setPlacedOrder] = useState<PlacedOrder | null>(null);
  const [reload, setReload] = useState(0);

  useEffect(() => {
    if (placedOrder) window.scrollTo({ top: 0, behavior: "auto" });
  }, [placedOrder]);

  useEffect(() => {
    if (authLoading) return;
    if (!user || user.role !== "CUSTOMER") { setLoading(false); return; }
    let active = true;
    setLoading(true); setError("");
    (async () => {
      try {
        const checkout = await checkoutApi<CheckoutData>();
        if (!active) return;
        setData(checkout);
        setAddressId(checkout.addresses.find(address => address.isDefault)?.addressId || checkout.addresses[0]?.addressId || null);
        setMethods(Object.fromEntries(checkout.items.map(item => [item.sellerId,
          checkout.shippingMethods.includes("STANDARD") ? "STANDARD" : checkout.shippingMethods[0]])));
        setPayment(checkout.paymentMethods.includes("PROMPTPAY") ? "PROMPTPAY" : checkout.paymentMethods[0]);
      } catch (error) { if (active) setError(error instanceof Error ? error.message : "Could not load checkout"); }
      finally { if (active) setLoading(false); }
    })();
    return () => { active = false; };
  }, [authLoading, user?.id, user?.role, reload]);

  const data = useMemo(() => metadata ? { ...metadata, items: items.map(item => ({ productId: item.id, productName: item.title, sellerId: item.sellerId!, shopName: item.sellerShopName || "", unitPrice: item.discountedPrice, quantity: item.quantity, imageUrl: item.imgs?.thumbnails?.[0] || null })) } : null, [metadata, items]);

  const shops = useMemo(() => {
    const grouped = new Map<number, CheckoutItem[]>();
    data?.items.forEach(item => grouped.set(item.sellerId, [...(grouped.get(item.sellerId) || []), item]));
    return Array.from(grouped.entries());
  }, [data]);

  useEffect(() => {
    if (!data) return;
    setMethods(current => Object.fromEntries(shops.map(([id]) => [id, current[id] ||
      (data.shippingMethods.includes("STANDARD") ? "STANDARD" : data.shippingMethods[0])])));
  }, [shops, data]);

  useEffect(() => {
    const controller = new AbortController();
    setFees({}); setQuoteError("");
    if (!data || !addressId || shops.length === 0 || placedOrder) { setQuoting(false); return; }
    setQuoting(true);
    Promise.all(shops.map(async ([sellerId]) => {
      const quotes = await Promise.all(data.shippingMethods.map(async shippingMethod => {
        const quote = await checkoutApi<{ shippingFee: number }>("quote", { sellerId, shippingMethod, addressId }, controller.signal);
        return [shippingMethod, Number(quote.shippingFee)] as const;
      }));
      return [sellerId, Object.fromEntries(quotes)] as const;
    })).then(result => { if (!controller.signal.aborted) setFees(Object.fromEntries(result)); })
      .catch(error => { if (!controller.signal.aborted) setQuoteError(error instanceof Error ? error.message : "Could not calculate shipping"); })
      .finally(() => { if (!controller.signal.aborted) setQuoting(false); });
    return () => controller.abort();
  }, [data, addressId, shops, placedOrder]);

  const subtotal = data?.items.reduce((sum, item) => sum + Number(item.unitPrice) * item.quantity, 0) || 0;
  const quotesReady = shops.length > 0 && shops.every(([id]) => Number.isFinite(fees[id]?.[methods[id]]));
  const shipping = shops.reduce((sum, [id]) => sum + (fees[id]?.[methods[id]] || 0), 0);
  const canOrder = Boolean(addressId && quotesReady && !quoting && !quoteError && !submitting && !cartPending && !cartLoading && payment);

  async function placeOrder() {
    if (!canOrder) return;
    setSubmitting(true); setError("");
    try {
      const order = await checkoutApi<PlacedOrder>("orders", { shippingAddressId: addressId,
        sellerShippingMethods: methods, paymentMethod: payment });
      setPlacedOrder(order); await orderPlaced();
    } catch (error) { setError(error instanceof Error ? error.message : "Could not place order"); }
    finally { setSubmitting(false); }
  }

  return <main><Breadcrumb title="Checkout" pages={["Checkout"]} />
    <section className="bg-gray-2 py-12 sm:py-16"><div className="mx-auto w-full max-w-[1170px] px-4 sm:px-8 xl:px-0">
      {authLoading || loading || (cartLoading && !placedOrder) ? <Card><CardContent role="status" className="py-14 text-center">Loading checkout…</CardContent></Card>
      : !user ? <Card><CardContent className="py-14 text-center"><h2 className="text-xl font-semibold text-dark">Sign in to check out</h2><p className="mt-2 text-sm">Use your customer account to choose an address and place your order.</p><Link href="/signin" className="mt-6 inline-flex rounded-lg bg-blue px-5 py-3 text-sm text-white">Sign in</Link></CardContent></Card>
      : user.role !== "CUSTOMER" ? <Card><CardContent className="py-14 text-center">Checkout is available for customer accounts.</CardContent></Card>
      : placedOrder ? <Card><CardContent className="py-10 sm:p-10"><CheckCircle2 className="mb-4 size-10 text-blue" /><h2 className="text-2xl font-semibold text-dark">Your order has been placed</h2>
        <p className="mt-2">Order group #{placedOrder.orderGroupId}</p><p className="mt-2 text-sm">Payment pending. Your order is saved; payment has not been collected.</p>
        <div className="mt-6 space-y-3">{placedOrder.subOrders.map(order => <div key={order.orderId} className="flex flex-wrap justify-between gap-3 rounded-lg border border-gray-3 p-4 text-sm"><span className="text-dark">Order #{order.orderId} · {methodLabel(order.shippingMethod)}</span><span>Shipping {money(order.shippingFee)} · {money(order.totalAmount)}</span></div>)}</div>
        <p className="mt-6 text-lg font-semibold text-dark">Total {money(placedOrder.grandTotal)}</p>
        <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:flex-wrap">
          <Link href={`/payment/${placedOrder.orderGroupId}`} className="inline-flex items-center justify-center rounded-lg bg-blue px-5 py-3 text-sm text-white hover:bg-blue-dark focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue">Pay now</Link>
          <Link href="/shop-with-sidebar" className="inline-flex items-center justify-center rounded-lg border border-gray-3 px-5 py-3 text-sm text-dark hover:bg-gray-1 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue">Continue shopping</Link>
          <Link href="/order-history" className="inline-flex items-center justify-center rounded-lg border border-gray-3 px-5 py-3 text-sm text-dark hover:bg-gray-1 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue">View order</Link>
        </div>
      </CardContent></Card>
      : <>
        {error && <div role="alert" className="mb-6 rounded-lg border border-red/20 bg-white p-4 text-sm text-red">{error}<Button type="button" variant="outline" className="ml-3" onClick={() => setReload(value => value + 1)}>Reload checkout</Button></div>}
        {!data ? <Card><CardContent className="py-12 text-center"><p>Checkout could not be loaded.</p><Link href="/cart" className="mt-4 inline-block text-blue">Return to cart</Link></CardContent></Card>
        : data.items.length === 0 ? <Card><CardContent className="py-14 text-center"><Package className="mx-auto mb-4 size-10 text-blue" /><h2 className="text-lg font-semibold text-dark">Your cart is empty</h2><Link href="/shop-with-sidebar" className="mt-6 inline-flex rounded-lg bg-blue px-5 py-3 text-sm text-white">Browse the shop</Link></CardContent></Card>
        : <div className="grid gap-7 lg:grid-cols-[minmax(0,1fr)_360px] lg:items-start">
          <div className="min-w-0 space-y-6">
            <DeliveryAddress customerId={data.customerId} addresses={data.addresses} value={addressId} disabled={submitting}
              onSelect={id => { setFees({}); setAddressId(id); }}
              onAdded={address => { setData(current => current ? { ...current, addresses: [...current.addresses, address] } : current); setAddressId(address.addressId); }}
              onUpdated={address => setData(current => current ? { ...current, addresses: current.addresses.map(a => a.addressId === address.addressId ? address : a) } : current)} />
            {shops.map(([sellerId, items]) => <Card key={sellerId}><CardHeader className="border-b border-gray-3"><CardTitle className="flex items-center gap-2"><Store className="size-5 text-blue" /><Link href={`/shop/${sellerId}`} className="hover:text-blue">{items[0].shopName || `Shop #${sellerId}`}</Link></CardTitle></CardHeader><CardContent className="pt-6">
              <div className="space-y-4">{items.map(item => <div key={item.productId} className="flex min-w-0 items-start gap-4">
                <ProductImage src={item.imageUrl} alt={item.productName} size="md" surface="soft" />
                <div className="min-w-0 flex-1"><p className="break-words text-sm font-medium text-dark">{item.productName}</p><p className="mt-1 text-xs">Qty {item.quantity} · {money(Number(item.unitPrice))} each</p></div><span className="shrink-0 text-right text-sm font-semibold text-dark">{money(Number(item.unitPrice) * item.quantity)}</span>
              </div>)}</div>
              <ShippingMethod sellerId={sellerId} methods={data.shippingMethods} value={methods[sellerId]} fees={fees[sellerId] || {}}
                loading={quoting} disabled={!addressId || submitting} onChange={method => setMethods(current => ({ ...current, [sellerId]: method }))} />
            </CardContent></Card>)}
            {quoteError && <div role="alert" className="rounded-lg border border-red/20 bg-white p-4 text-sm text-red">{quoteError}<Button variant="outline" className="ml-3" onClick={() => setData(current => current ? { ...current } : current)}>Recalculate shipping</Button></div>}
          </div>
          <div className="min-w-0 space-y-6 lg:sticky lg:top-28"><Card><CardHeader className="border-b border-gray-3"><CardTitle>Order summary</CardTitle></CardHeader><CardContent className="pt-6 text-sm">
            <div className="flex justify-between"><span>Products ({data.items.reduce((sum, item) => sum + item.quantity, 0)})</span><span className="font-medium text-dark">{money(subtotal)}</span></div>
            <div className="mt-5 border-t border-gray-3 pt-4">
              <div className="flex items-center justify-between gap-3 font-medium text-dark"><span className="inline-flex min-w-0 items-center gap-2"><Truck size={16} className="shrink-0 text-blue" aria-hidden="true" />Shipping total</span><span className="shrink-0" aria-live="polite">{quoting ? "…" : quotesReady ? money(shipping) : "—"}</span></div>
              <p className="mt-1 text-xs leading-5 text-dark-4">{quoting ? "Calculating shipping…" : "Shipping fees by shop and selected delivery method"}</p>
              <div className="mt-3 space-y-3">{shops.map(([id, items]) => <div key={id} className="flex justify-between gap-3"><span className="min-w-0 break-words"><span className="block text-dark">{items[0].shopName || `Shop #${id}`}</span><span className="mt-1 block text-xs text-dark-4">{methodLabel(methods[id] || "")}</span></span><span className="shrink-0 text-dark">{quoting ? "…" : fees[id]?.[methods[id]] === undefined ? "—" : money(fees[id][methods[id]])}</span></div>)}</div>
            </div>
            <div className="mt-5 flex justify-between border-t border-gray-3 pt-5 text-lg font-semibold text-dark"><span>Total</span><span>{quotesReady ? money(subtotal + shipping) : "—"}</span></div>
            <p className="mt-3 text-xs">Includes every item in your cart. Final totals are confirmed when your order is created.</p>
          </CardContent></Card>
          <Card><CardHeader><CardTitle>Payment method</CardTitle></CardHeader><CardContent><fieldset disabled={submitting} className="space-y-3"><legend className="sr-only">Payment method</legend>{data.paymentMethods.map(method => <label key={method} className="flex cursor-pointer items-center gap-3 rounded-lg border border-gray-3 p-3 text-sm text-dark"><input type="radio" name="payment" checked={payment === method} onChange={() => setPayment(method)} className="accent-blue" />{methodLabel(method)}</label>)}</fieldset><p className="mt-4 text-xs">Choose your payment method. Placing the order does not collect payment.</p></CardContent></Card>
          <Button type="button" disabled={!canOrder} onClick={placeOrder} className="h-12 w-full bg-blue text-white">{submitting ? "Placing order…" : "Place order"}</Button>
          <Link href="/cart" className="block text-center text-sm font-medium text-blue hover:underline">Return to cart</Link>
          </div>
        </div>}
      </>}
    </div></section>
  </main>;
}
