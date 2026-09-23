"use client";

import Link from "next/link";
import Image from "next/image";
import { ChevronRight, Minus, Plus, ShoppingBag, Store, Trash2 } from "lucide-react";
import { useDispatch } from "react-redux";
import Breadcrumb from "@/components/Common/Breadcrumb";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { removeAllItemsFromCart, removeItemFromCart, updateCartItemQuantity, type CartItem } from "@/redux/features/cart-slice";
import { AppDispatch, useAppSelector } from "@/redux/store";
import { templateShopName } from "@/lib/templateShops";

const currency = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });

type ShopGroup = {
  key: string;
  name: string;
  sellerId?: number;
  sample: boolean;
  items: CartItem[];
  count: number;
  subtotal: number;
};

function groupByShop(items: CartItem[]): ShopGroup[] {
  const groups = new Map<string, ShopGroup>();
  for (const item of items) {
    const sellerId = item.sellerId != null && item.sellerId > 0 ? item.sellerId : undefined;
    const previewName = templateShopName(item);
    const name = item.sellerShopName?.trim() || previewName || (sellerId ? `Store #${sellerId}` : "Store information unavailable");
    const key = sellerId ? `seller:${sellerId}` : `name:${name}`;
    let group = groups.get(key);
    if (!group) {
      group = { key, name, sellerId, sample: !sellerId && !!previewName, items: [], count: 0, subtotal: 0 };
      groups.set(key, group);
    }
    group.items.push(item);
    group.count += item.quantity;
    group.subtotal += item.discountedPrice * item.quantity;
  }
  return Array.from(groups.values());
}

export default function Cart() {
  const items = useAppSelector((state) => state.cartReducer.items);
  const dispatch = useDispatch<AppDispatch>();
  const groups = groupByShop(items);
  const subtotal = groups.reduce((sum, group) => sum + group.subtotal, 0);
  const count = groups.reduce((sum, group) => sum + group.count, 0);

  return <main>
    <Breadcrumb title="Shopping Cart" pages={["Cart"]} />
    <section className="bg-gray-2 py-12 sm:py-16">
      <div className="mx-auto w-full max-w-[1170px] px-4 sm:px-8 xl:px-0">
        <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
          <div><h2 className="text-2xl font-semibold text-dark">Your cart</h2><p className="mt-2 text-sm text-dark-4">{count} {count === 1 ? "item" : "items"} from {groups.length} {groups.length === 1 ? "store" : "stores"}</p></div>
          {items.length > 0 && <Button type="button" variant="ghost" className="text-dark-4 hover:text-red" onClick={() => dispatch(removeAllItemsFromCart())}><Trash2 className="mr-2 size-4" />Clear cart</Button>}
        </div>

        {items.length === 0 ? <Card><CardContent className="flex min-h-[330px] flex-col items-center justify-center px-6 py-14 text-center"><div className="mb-5 flex size-16 items-center justify-center rounded-full bg-blue/10 text-blue"><ShoppingBag className="size-8" /></div><h3 className="text-lg font-semibold text-dark">Your cart is empty</h3><p className="mt-2 max-w-sm text-sm text-dark-4">Browse the shop and add something you like.</p><Link href="/shop-with-sidebar" className="mt-6 inline-flex h-10 items-center justify-center rounded-lg bg-blue px-5 text-sm font-medium text-white hover:bg-blue-dark">Continue shopping</Link></CardContent></Card> : <div className="grid gap-7 lg:grid-cols-[minmax(0,1fr)_330px] lg:items-start">
          <div className="space-y-5">
            {groups.map((group) => <Card key={group.key} className="overflow-hidden">
              <CardHeader className="border-b border-gray-3 bg-white px-5 py-5 sm:px-6">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div className="flex min-w-0 items-center gap-3">
                    <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-blue/10 text-blue"><Store className="size-5" /></div>
                    <div className="min-w-0">
                      {group.sellerId ? <Link href={`/shop/${group.sellerId}`} className="inline-flex items-center gap-1 font-semibold text-dark hover:text-blue">{group.name}<ChevronRight className="size-4 shrink-0" /></Link> : <CardTitle className="text-base">{group.name}</CardTitle>}
                      <p className="mt-0.5 text-xs text-dark-4">{group.count} {group.count === 1 ? "item" : "items"}{group.sample ? " · Sample store" : ""}</p>
                    </div>
                  </div>
                  <span className="text-xs font-medium text-dark-4">Store subtotal {currency.format(group.subtotal)}</span>
                </div>
              </CardHeader>
              <CardContent className="divide-y divide-gray-3 p-0">
                {group.items.map((item) => <div key={item.id} className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center sm:justify-between sm:gap-6 sm:p-6">
                  <div className="flex min-w-0 flex-1 gap-4">
                    <div className="flex size-20 shrink-0 items-center justify-center rounded-lg bg-gray-2 sm:size-24">
                      {item.imgs?.thumbnails?.[0] ? <Image src={item.imgs.thumbnails[0]} alt="" width={88} height={88} className="max-h-[80px] w-auto object-contain" /> : <ShoppingBag className="size-7 text-dark-4" />}
                    </div>
                    <div className="min-w-0"><p className="font-medium leading-6 text-dark">{item.title}</p><p className="mt-2 text-sm font-semibold text-blue">{currency.format(item.discountedPrice)}</p>{item.price > item.discountedPrice && <p className="mt-1 text-xs text-dark-4 line-through">{currency.format(item.price)}</p>}</div>
                  </div>
                  <div className="flex items-center justify-between gap-3 sm:justify-end">
                    <div className="flex h-10 items-center rounded-lg border border-gray-3 bg-white">
                      <Button type="button" variant="ghost" size="icon" aria-label={`Decrease ${item.title} quantity`} disabled={item.quantity <= 1} onClick={() => dispatch(updateCartItemQuantity({ id: item.id, quantity: item.quantity - 1 }))}><Minus className="size-4" /></Button>
                      <span className="w-8 text-center text-sm text-dark" aria-live="polite">{item.quantity}</span>
                      <Button type="button" variant="ghost" size="icon" aria-label={`Increase ${item.title} quantity`} onClick={() => dispatch(updateCartItemQuantity({ id: item.id, quantity: item.quantity + 1 }))}><Plus className="size-4" /></Button>
                    </div>
                    <strong className="min-w-[86px] text-right text-sm font-semibold text-dark">{currency.format(item.discountedPrice * item.quantity)}</strong>
                    <Button type="button" variant="ghost" size="icon" aria-label={`Remove ${item.title} from cart`} onClick={() => dispatch(removeItemFromCart(item.id))}><Trash2 className="size-4 text-dark-4" /></Button>
                  </div>
                </div>)}
              </CardContent>
            </Card>)}
          </div>
          <Card className="lg:sticky lg:top-6"><CardHeader className="border-b border-gray-3"><CardTitle>Order summary</CardTitle><p className="text-sm text-dark-4">Checkout includes every item in your cart.</p></CardHeader><CardContent className="space-y-5 pt-6">
            <div className="flex justify-between gap-4 text-sm"><span>Items ({count})</span><span className="font-medium text-dark">{currency.format(subtotal)}</span></div>
            <div className="flex justify-between gap-4 text-sm"><span>Stores</span><span className="font-medium text-dark">{groups.length}</span></div>
            <div className="flex justify-between gap-4 text-sm"><span>Shipping</span><span className="text-right text-dark-4">Calculated at checkout</span></div>
            <div className="flex justify-between gap-4 border-t border-gray-3 pt-5 font-semibold text-dark"><span>Total before shipping</span><span>{currency.format(subtotal)}</span></div>
            <Link href="/checkout" className="flex h-11 items-center justify-center rounded-lg bg-blue px-5 text-sm font-medium text-white hover:bg-blue-dark">Checkout all {count} {count === 1 ? "item" : "items"}</Link>
            <Link href="/shop-with-sidebar" className="block text-center text-sm font-medium text-blue hover:underline">Continue shopping</Link>
          </CardContent></Card>
        </div>}
      </div>
    </section>
  </main>;
}
