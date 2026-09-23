"use client";

import Link from "next/link";
import { Info } from "lucide-react";
import Breadcrumb from "@/components/Common/Breadcrumb";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useAppSelector } from "@/redux/store";
import Billing from "./Billing";
import Shipping from "./Shipping";
import ShippingMethod from "./ShippingMethod";
import PaymentMethod from "./PaymentMethod";

const currency = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });

export default function Checkout() {
  const items = useAppSelector((state) => state.cartReducer.items);
  const subtotal = items.reduce((sum, item) => sum + item.discountedPrice * item.quantity, 0);

  return <main>
    <Breadcrumb title="Checkout" pages={["Checkout"]} />
    <section className="bg-gray-2 py-12 sm:py-16">
      <div className="mx-auto w-full max-w-[1170px] px-4 sm:px-8 xl:px-0">
        <div role="status" className="mb-7 flex gap-3 rounded-xl border border-blue/20 bg-blue/5 p-5 text-sm text-dark"><Info className="mt-0.5 size-5 shrink-0 text-blue" aria-hidden="true" /><div><strong>Checkout preview</strong><p className="mt-1">You can review your cart and delivery fields. Placing an order will be available when the order API is connected.</p></div></div>
        {items.length === 0 ? <Card><CardContent className="py-14 text-center"><h2 className="text-lg font-semibold text-dark">Your cart is empty</h2><p className="mt-2 text-sm">Add a product before checking out.</p><Link href="/shop-with-sidebar" className="mt-6 inline-flex h-10 items-center rounded-lg bg-blue px-5 text-sm font-medium text-white hover:bg-blue-dark">Browse the shop</Link></CardContent></Card> : <div className="grid gap-7 lg:grid-cols-[minmax(0,1fr)_360px] lg:items-start">
          <div className="space-y-7"><Billing /><Shipping /><div className="bg-white shadow-1 rounded-[10px] p-5 sm:p-8"><label htmlFor="checkout-notes" className="mb-2 block text-sm font-medium text-dark">Order notes (optional)</label><textarea id="checkout-notes" rows={4} placeholder="Delivery instructions" className="w-full rounded-lg border border-gray-3 bg-gray-1 p-4 outline-none focus:ring-2 focus:ring-blue/20" /></div></div>
          <div className="space-y-7"><Card><CardHeader className="border-b border-gray-3"><CardTitle>Your order</CardTitle></CardHeader><CardContent className="pt-6"><div className="space-y-4">{items.map((item) => <div key={item.id} className="flex justify-between gap-4 border-b border-gray-3 pb-4 text-sm"><span className="text-dark">{item.title} × {item.quantity}</span><span className="shrink-0 font-medium text-dark">{currency.format(item.discountedPrice * item.quantity)}</span></div>)}</div><div className="mt-5 flex justify-between gap-4 text-sm"><span>Shipping</span><span>Calculated when ordering opens</span></div><div className="mt-5 flex justify-between gap-4 border-t border-gray-3 pt-5 font-semibold text-dark"><span>Subtotal</span><span>{currency.format(subtotal)}</span></div></CardContent></Card><ShippingMethod /><PaymentMethod /><Button type="button" disabled className="h-11 w-full bg-blue text-white">Place order — coming soon</Button><Link href="/cart" className="block text-center text-sm font-medium text-blue hover:underline">Return to cart</Link></div>
        </div>}
      </div>
    </section>
  </main>;
}
