"use client";

import Link from "next/link";
import Image from "next/image";
import { Minus, Plus, ShoppingBag, Trash2 } from "lucide-react";
import { useDispatch } from "react-redux";
import Breadcrumb from "@/components/Common/Breadcrumb";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { removeAllItemsFromCart, removeItemFromCart, updateCartItemQuantity } from "@/redux/features/cart-slice";
import { AppDispatch, useAppSelector } from "@/redux/store";

const currency = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });

export default function Cart() {
  const items = useAppSelector((state) => state.cartReducer.items);
  const dispatch = useDispatch<AppDispatch>();
  const subtotal = items.reduce((sum, item) => sum + item.discountedPrice * item.quantity, 0);
  const count = items.reduce((sum, item) => sum + item.quantity, 0);

  return <main>
    <Breadcrumb title="Shopping Cart" pages={["Cart"]} />
    <section className="bg-gray-2 py-12 sm:py-16">
      <div className="mx-auto w-full max-w-[1170px] px-4 sm:px-8 xl:px-0">
        <div className="mb-8 flex flex-wrap items-end justify-between gap-4"><div><h2 className="text-2xl font-semibold text-dark">Your cart</h2><p className="mt-2 text-sm text-dark-4">{count} {count === 1 ? "item" : "items"} ready to review</p></div>{items.length > 0 && <Button variant="ghost" className="text-dark-4 hover:text-red" onClick={() => dispatch(removeAllItemsFromCart())}><Trash2 className="mr-2 size-4" />Clear cart</Button>}</div>
        {items.length === 0 ? <Card><CardContent className="flex min-h-[330px] flex-col items-center justify-center px-6 py-14 text-center"><div className="mb-5 flex size-16 items-center justify-center rounded-full bg-blue/10 text-blue"><ShoppingBag className="size-8" /></div><h3 className="text-lg font-semibold text-dark">Your cart is empty</h3><p className="mt-2 max-w-sm text-sm text-dark-4">Browse the shop and add something you like.</p><Link href="/shop-with-sidebar" className="mt-6 inline-flex h-10 items-center justify-center rounded-lg bg-blue px-5 text-sm font-medium text-white hover:bg-blue-dark">Continue shopping</Link></CardContent></Card> : <div className="grid gap-7 lg:grid-cols-[minmax(0,1fr)_330px] lg:items-start">
          <Card className="overflow-hidden"><CardHeader className="border-b border-gray-3"><CardTitle>Items in your cart</CardTitle></CardHeader><CardContent className="p-0">
            {items.map((item) => <div key={item.id} className="flex flex-col gap-5 border-b border-gray-3 p-5 last:border-b-0 sm:flex-row sm:items-center sm:p-6">
              <div className="flex min-w-0 flex-1 gap-4"><div className="flex size-20 shrink-0 items-center justify-center rounded-lg bg-gray-2">{item.imgs?.thumbnails?.[0] ? <Image src={item.imgs.thumbnails[0]} alt="" width={72} height={72} className="max-h-18 w-auto object-contain" /> : <ShoppingBag className="size-7 text-dark-4" />}</div><div className="min-w-0"><p className="font-medium text-dark">{item.title}</p><p className="mt-1 text-sm text-dark-4">{currency.format(item.discountedPrice)} each</p>{item.price > item.discountedPrice && <p className="mt-1 text-xs text-dark-4 line-through">{currency.format(item.price)}</p>}</div></div>
              <div className="flex items-center justify-between gap-3 sm:justify-end"><div className="flex h-10 items-center rounded-lg border border-gray-3"><Button type="button" variant="ghost" size="icon" aria-label={`Decrease ${item.title} quantity`} disabled={item.quantity <= 1} onClick={() => dispatch(updateCartItemQuantity({ id: item.id, quantity: item.quantity - 1 }))}><Minus className="size-4" /></Button><span className="w-8 text-center text-sm text-dark" aria-live="polite">{item.quantity}</span><Button type="button" variant="ghost" size="icon" aria-label={`Increase ${item.title} quantity`} onClick={() => dispatch(updateCartItemQuantity({ id: item.id, quantity: item.quantity + 1 }))}><Plus className="size-4" /></Button></div><strong className="min-w-[82px] text-right text-sm font-semibold text-dark">{currency.format(item.discountedPrice * item.quantity)}</strong><Button type="button" variant="ghost" size="icon" aria-label={`Remove ${item.title} from cart`} onClick={() => dispatch(removeItemFromCart(item.id))}><Trash2 className="size-4 text-dark-4" /></Button></div>
            </div>)}
          </CardContent></Card>
          <Card><CardHeader className="border-b border-gray-3"><CardTitle>Order summary</CardTitle></CardHeader><CardContent className="space-y-5 pt-6"><div className="flex justify-between gap-4 text-sm"><span>Subtotal ({count} items)</span><span className="font-medium text-dark">{currency.format(subtotal)}</span></div><div className="flex justify-between gap-4 text-sm"><span>Shipping</span><span className="text-dark-4">Calculated at checkout</span></div><div className="flex justify-between gap-4 border-t border-gray-3 pt-5 font-semibold text-dark"><span>Subtotal</span><span>{currency.format(subtotal)}</span></div><Link href="/checkout" className="flex h-11 items-center justify-center rounded-lg bg-blue px-5 text-sm font-medium text-white hover:bg-blue-dark">Continue to checkout</Link><Link href="/shop-with-sidebar" className="block text-center text-sm font-medium text-blue hover:underline">Continue shopping</Link></CardContent></Card>
        </div>}
      </div>
    </section>
  </main>;
}
