"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ArrowUpRight, ClipboardCheck, Package, Store, Wallet } from "lucide-react";
import Breadcrumb from "@/components/Common/Breadcrumb";
import { useSellerPreview, type SellerOrderPreview } from "@/app/context/SellerPreviewContext";
import { Card, CardContent } from "@/components/ui/card";
import { DemoNotice, SellerNav, StatusBadge } from "./Shared";
import { useAuth } from "@/app/context/AuthContext";

const currency = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });
const paidStatuses = new Set(["WAITING_SELLER_CONFIRM", "PREPARING", "SHIPPED", "COMPLETED"]);
const activeStatuses = new Set(["WAITING_SELLER_CONFIRM", "PREPARING", "SHIPPED"]);

function OrderList({ orders }: { orders: SellerOrderPreview[] }) {
  return orders.length ? <div className="divide-y divide-gray-3">{orders.map((order) => <Link key={order.orderId} href={`/seller/${order.orderId}?demo=1`} className="grid gap-3 px-5 py-5 text-sm hover:bg-gray-1 sm:px-6 lg:grid-cols-[1fr_1fr_1.5fr_1fr_auto] lg:items-center"><span className="font-medium text-dark">#{order.orderNumber}</span><span>{order.createdAt}</span><span><StatusBadge status={order.status} /></span><span className="font-medium text-dark">{currency.format(order.totalAmount)}</span><span className="inline-flex items-center gap-1 font-medium text-blue">View order <ArrowUpRight className="size-4" /></span></Link>)}</div> : <p className="px-6 py-12 text-center text-sm">No orders in this view.</p>;
}

export default function SellerDashboard() {
  const { user } = useAuth();
  const { application, orders } = useSellerPreview();
  const [realShop, setRealShop] = useState<{ sellerId: number; shopName: string } | null>(null);
  useEffect(() => {
    if (user?.role !== "SELLER") return;
    fetch("/api/seller-shop", { cache: "no-store" }).then((response) => response.json()).then((body) => {
      if (body.success && body.data?.sellerId) setRealShop(body.data);
    }).catch(() => {});
  }, [user?.role]);
  const [view, setView] = useState<"active" | "history">("active");
  const approved = application?.status === "APPROVED" || user?.role === "SELLER";
  const shopName = realShop?.shopName || application?.shopName || "Your shop";
  const shopId = realShop?.sellerId ?? application?.sellerId;
  const visibleOrders = application?.status === "APPROVED" ? orders : [];
  const paidSales = visibleOrders.filter((order) => paidStatuses.has(order.status)).reduce((sum, order) => sum + order.totalAmount, 0);
  const active = visibleOrders.filter((order) => activeStatuses.has(order.status));
  const history = visibleOrders.filter((order) => order.status === "COMPLETED" || order.status === "CANCELLED");
  const awaiting = visibleOrders.filter((order) => order.status === "WAITING_SELLER_CONFIRM").length;

  return <main>
    <Breadcrumb title="Shop Dashboard" pages={["Seller", "Dashboard"]} />
    <section className="bg-gray-2 py-12 sm:py-16"><div className="mx-auto w-full max-w-[1170px] px-4 sm:px-8 xl:px-0">
      <SellerNav />
      {application?.status === "APPROVED" && <DemoNotice>Sales and orders shown here are example data. Seller actions stay in this browser session and never reach the backend.</DemoNotice>}
      {!approved ? <Card className="mx-auto max-w-2xl"><CardContent className="px-6 py-14 text-center"><div className="mx-auto mb-5 flex size-16 items-center justify-center rounded-full bg-blue/10 text-blue"><Store className="size-8" /></div><h2 className="text-xl font-semibold text-dark">Dashboard opens after approval</h2><p className="mx-auto mt-2 max-w-md text-sm">{application ? "Your current application status does not allow shop management yet." : "Start a seller application to see the approval flow."}</p><Link href="/seller-application" className="mt-6 inline-flex h-10 items-center rounded-lg bg-blue px-5 text-sm font-medium text-white hover:bg-blue-dark">View application</Link></CardContent></Card> : <>
        {user?.role === "SELLER" && !application && <p role="status" className="mb-6 rounded-lg border border-blue/20 bg-white px-5 py-4 text-sm text-dark">Your shop is approved. Order totals and history will appear when the seller order API is connected; no sample orders are shown.</p>}
        <div className="mb-7 flex flex-wrap items-end justify-between gap-4"><div><h2 className="text-2xl font-semibold text-dark">{shopName}</h2><p className="mt-1 text-sm text-dark-4">Your shop at a glance</p></div>{shopId != null && <Link href={`/shop/${shopId}`} className="inline-flex h-10 items-center gap-2 rounded-lg border border-gray-3 bg-white px-4 text-sm font-medium text-dark hover:border-blue"><Store className="size-4" />View storefront</Link>}</div>
        <div className="mb-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <Card><CardContent className="p-5"><Wallet className="mb-4 size-5 text-blue" /><p className="text-sm">Paid sales</p><strong className="mt-1 block text-2xl text-dark">{currency.format(paidSales)}</strong><p className="mt-2 text-xs text-dark-4">Excludes unpaid and cancelled orders</p></CardContent></Card>
          <Card><CardContent className="p-5"><Package className="mb-4 size-5 text-blue" /><p className="text-sm">Active orders</p><strong className="mt-1 block text-2xl text-dark">{active.length}</strong><p className="mt-2 text-xs text-dark-4">Acceptance, preparation, shipping</p></CardContent></Card>
          <Card><CardContent className="p-5"><ClipboardCheck className="mb-4 size-5 text-blue" /><p className="text-sm">Awaiting acceptance</p><strong className="mt-1 block text-2xl text-dark">{awaiting}</strong><p className="mt-2 text-xs text-dark-4">Orders ready for your decision</p></CardContent></Card>
          <Card><CardContent className="p-5"><ClipboardCheck className="mb-4 size-5 text-blue" /><p className="text-sm">Completed orders</p><strong className="mt-1 block text-2xl text-dark">{visibleOrders.filter((order) => order.status === "COMPLETED").length}</strong><p className="mt-2 text-xs text-dark-4">Delivered and confirmed</p></CardContent></Card>
        </div>
        <Card className="overflow-hidden"><div className="flex flex-wrap items-center justify-between gap-4 border-b border-gray-3 px-5 py-5 sm:px-6"><div><h3 className="text-lg font-semibold text-dark">Orders</h3><p className="mt-1 text-sm text-dark-4">Review incoming orders and past activity.</p></div><div className="flex rounded-lg border border-gray-3 bg-gray-1 p-1"><button type="button" onClick={() => setView("active")} className={`rounded-md px-3 py-2 text-sm font-medium ${view === "active" ? "bg-white text-blue shadow-sm" : "text-dark-4"}`}>Active ({active.length})</button><button type="button" onClick={() => setView("history")} className={`rounded-md px-3 py-2 text-sm font-medium ${view === "history" ? "bg-white text-blue shadow-sm" : "text-dark-4"}`}>History ({history.length})</button></div></div><div className="hidden grid-cols-[1fr_1fr_1.5fr_1fr_auto] gap-3 bg-gray-1 px-6 py-3 text-xs font-medium text-dark lg:grid"><span>Order</span><span>Date</span><span>Status</span><span>Total</span><span>Action</span></div><OrderList orders={view === "active" ? active : history} /></Card>
      </>}
    </div></section>
  </main>;
}
