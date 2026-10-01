"use client";

import { useState } from "react";
import Link from "next/link";
import { ArrowUpRight, ClipboardCheck, Package, Store, Wallet } from "lucide-react";
import Breadcrumb from "@/components/Common/Breadcrumb";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { StatusBadge } from "./Shared";
import { useSellerOrders } from "./useSellerOrders";
import { sellerOrderCurrency as currency, sellerOrderSummary, orderDate, hasOrderStatusConflict, type SellerOrder } from "@/lib/sellerOrders";

function OrderList({ orders }: { orders: SellerOrder[] }) {
  return orders.length ? <div className="divide-y divide-gray-3">{orders.map(order => <Link key={order.orderId} href={`/seller/${order.orderId}`} className="grid gap-3 px-5 py-5 text-sm hover:bg-gray-1 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-blue sm:px-6 lg:grid-cols-[1fr_1fr_1.5fr_1fr_auto] lg:items-center"><span className="break-all font-medium text-dark">#{order.subOrderNumber}</span><span>{orderDate(order.createdAt)}</span><span><StatusBadge status={order.orderStatus} />{hasOrderStatusConflict(order) && <span className="mt-2 block text-xs text-red">Order and payment statuses need review</span>}</span><span className="font-medium text-dark">{currency.format(order.totalAmount)}</span><span className="inline-flex items-center gap-1 font-medium text-blue">View order <ArrowUpRight className="size-4" aria-hidden="true" /></span></Link>)}</div> : <p className="px-6 py-12 text-center text-sm">No orders in this view.</p>;
}

export default function SellerDashboard() {
  const { data, loading, refreshing, error, refresh } = useSellerOrders();
  const [view, setView] = useState<"active" | "history">("active");
  const summary = data ? sellerOrderSummary(data.orders) : null;
  const metrics = summary ? [
    { icon: Wallet, label: "Paid sales", value: currency.format(summary.paidSales), help: "Excludes unpaid and cancelled orders" },
    { icon: Package, label: "Active orders", value: summary.active.length, help: "Payment, acceptance, preparation, shipping" },
    { icon: ClipboardCheck, label: "Awaiting acceptance", value: summary.awaiting, help: "Paid orders ready for your decision" },
    { icon: ClipboardCheck, label: "Completed orders", value: summary.completed, help: "Delivered and confirmed" },
  ] : [];
  return <main>
    <Breadcrumb title="Shop Dashboard" pages={["Seller", "Dashboard"]} />
    <section className="bg-gray-2 py-12 sm:py-16"><div className="mx-auto w-full max-w-[1170px] px-4 sm:px-8 xl:px-0">
      {error && <div role="alert" className="mb-6 rounded-lg border border-red/20 bg-white p-5 text-sm"><p className="text-red">{error}</p>{data && <p className="mt-2 text-dark-4">Showing previously loaded orders. These totals may be out of date.</p>}<Button type="button" variant="outline" className="mt-3 text-blue" disabled={refreshing} onClick={refresh}>Retry</Button></div>}
      {loading ? <div role="status" aria-label="Loading shop orders"><Skeleton className="mb-7 h-20 motion-reduce:animate-none" /><div className="mb-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{[0, 1, 2, 3].map(index => <Skeleton key={index} className="h-40 motion-reduce:animate-none" />)}</div><Skeleton className="h-64 motion-reduce:animate-none" /></div> : data && summary && <>
        <div className="mb-7 flex flex-wrap items-end justify-between gap-4"><div><p className="text-sm font-medium text-blue">Sell on PandaStore</p><h2 className="mt-1 text-3xl font-semibold text-dark">{data.shop.shopName}</h2><p className="mt-2 text-sm text-dark-4">Your shop at a glance</p></div><Link href={`/shop/${data.shop.sellerId}`} className="inline-flex h-10 items-center gap-2 rounded-lg border border-gray-3 bg-white px-4 text-sm font-medium text-dark hover:border-blue focus-visible:ring-2 focus-visible:ring-blue"><Store className="size-4" aria-hidden="true" />View storefront</Link></div>
        <div className="mb-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{metrics.map(({ icon: Icon, label, value, help }) => <Card key={label}><CardContent className="p-5"><Icon className="mb-4 size-5 text-blue" aria-hidden="true" /><p className="text-sm">{label}</p><strong className="mt-1 block text-2xl text-dark">{value}</strong><p className="mt-2 text-xs text-dark-4">{help}</p></CardContent></Card>)}</div>
        <Card className="overflow-hidden"><div className="flex flex-wrap items-center justify-between gap-4 border-b border-gray-3 px-5 py-5 sm:px-6"><div><h3 className="text-lg font-semibold text-dark">Orders</h3><p className="mt-1 text-sm text-dark-4">Review incoming orders and past activity.</p></div><div className="flex rounded-lg border border-gray-3 bg-gray-1 p-1" role="group" aria-label="Order view">{(["active", "history"] as const).map(tab => <button key={tab} type="button" onClick={() => setView(tab)} aria-pressed={view === tab} className={`rounded-md px-3 py-2 text-sm font-medium focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue ${view === tab ? "bg-white text-blue shadow-sm" : "text-dark-4"}`}>{tab === "active" ? "Active" : "History"} ({summary[tab].length})</button>)}</div></div><div className="hidden grid-cols-[1fr_1fr_1.5fr_1fr_auto] gap-3 bg-gray-1 px-6 py-3 text-xs font-medium text-dark lg:grid"><span>Order</span><span>Date</span><span>Status</span><span>Total</span><span>Action</span></div><OrderList orders={summary[view]} /></Card>
        <p role="status" className="mt-4 text-sm text-dark-4">{refreshing ? "Refreshing orders…" : ""}</p>
      </>}
    </div></section>
  </main>;
}
