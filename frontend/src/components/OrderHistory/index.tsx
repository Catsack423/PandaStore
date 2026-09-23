"use client";

import { useState } from "react";
import Link from "next/link";
import { ChevronDown, PackageOpen, ShoppingBag } from "lucide-react";
import Breadcrumb from "@/components/Common/Breadcrumb";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";

export type CustomerOrder = {
  id: string;
  date: string;
  status: "placed" | "processing" | "shipped" | "delivered" | "cancelled";
  total: number;
  items: { name: string; quantity: number; price: number }[];
  shippingAddress?: string;
};

const statusLabels: Record<CustomerOrder["status"], string> = {
  placed: "Order placed",
  processing: "Processing",
  shipped: "Shipped",
  delivered: "Delivered",
  cancelled: "Cancelled",
};
const steps = ["Order placed", "Processing", "Shipped", "Delivered"];
const currency = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });

function OrderRow({ order }: { order: CustomerOrder }) {
  const [expanded, setExpanded] = useState(false);
  const currentStep = ["placed", "processing", "shipped", "delivered"].indexOf(order.status);

  return (
    <div className="border-t border-gray-3">
      <div className="grid gap-4 px-5 py-5 text-sm sm:px-7 lg:grid-cols-[1fr_1fr_2fr_1fr_auto] lg:items-center">
        <div><span className="block text-xs text-dark-4 lg:hidden">Order</span><strong className="font-medium text-dark">#{order.id}</strong></div>
        <div><span className="block text-xs text-dark-4 lg:hidden">Date</span><span className="text-dark">{order.date}</span></div>
        <div>
          <span className="block text-xs text-dark-4 lg:hidden">Order status</span>
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-1.5" aria-hidden="true">
              {steps.map((step, index) => <span key={step} className={`h-2.5 w-2.5 rounded-full ${order.status !== "cancelled" && index <= currentStep ? "bg-blue" : "bg-gray-4"}`} />)}
            </div>
            <span className="text-dark">{statusLabels[order.status]}</span>
          </div>
        </div>
        <div><span className="block text-xs text-dark-4 lg:hidden">Total</span><span className="font-medium text-dark">{currency.format(order.total)}</span></div>
        <Button variant="outline" aria-expanded={expanded} aria-controls={`order-${order.id}`} onClick={() => setExpanded(!expanded)}>
          Details <ChevronDown className={`ml-1 transition-transform ${expanded ? "rotate-180" : ""}`} />
        </Button>
      </div>
      {expanded && <div id={`order-${order.id}`} className="border-t border-gray-3 bg-gray-1 px-5 py-6 sm:px-7">
        <h3 className="mb-4 font-medium text-dark">Items in this order</h3>
        <div className="space-y-3">{order.items.map((item, index) => <div key={`${item.name}-${index}`} className="flex justify-between gap-5 text-sm"><span>{item.name} × {item.quantity}</span><span className="font-medium text-dark">{currency.format(item.price * item.quantity)}</span></div>)}</div>
        {order.shippingAddress && <p className="mt-6 border-t border-gray-3 pt-4 text-sm"><strong className="text-dark">Shipping address</strong><br />{order.shippingAddress}</p>}
      </div>}
    </div>
  );
}

export default function OrderHistory({ orders }: { orders: CustomerOrder[] }) {
  const [status, setStatus] = useState("all");
  const visibleOrders = status === "all" ? orders : orders.filter((order) => order.status === status);

  return <main>
    <Breadcrumb title="Order History" pages={["Order History"]} />
    <section className="bg-gray-2 py-12 sm:py-16">
      <div className="mx-auto w-full max-w-[1170px] px-4 sm:px-8 xl:px-0">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div><h2 className="text-2xl font-semibold text-dark">Your orders</h2><p className="mt-2 text-sm text-dark-4">Track purchases and review order details here.</p></div>
          <Link href="/my-account" className="text-sm font-medium text-blue hover:underline">Back to My Account</Link>
        </div>
        <Card className="overflow-hidden rounded-xl border-gray-3 shadow-1">
          <div className="flex flex-col gap-4 border-b border-gray-3 px-5 py-5 sm:flex-row sm:items-center sm:justify-between sm:px-7">
            <h3 className="font-medium text-dark">Order history</h3>
            <label className="flex items-center gap-2 text-sm text-dark"><span>Status</span><select value={status} onChange={(event) => setStatus(event.target.value)} className="h-9 rounded-lg border border-gray-3 bg-white px-3 outline-none focus:ring-2 focus:ring-blue/30"><option value="all">All orders</option>{Object.entries(statusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
          </div>
          <div className="hidden grid-cols-[1fr_1fr_2fr_1fr_auto] gap-4 bg-gray-1 px-7 py-4 text-xs font-medium text-dark lg:grid"><span>Order</span><span>Date</span><span>Order status</span><span>Total</span><span>Action</span></div>
          {orders.length > 0 ? <>
            {visibleOrders.length > 0 ? visibleOrders.map((order) => <OrderRow key={order.id} order={order} />) : <p className="px-7 py-10 text-center text-sm">No orders with this status.</p>}
          </> : <CardContent className="flex min-h-[330px] flex-col items-center justify-center px-6 py-14 text-center">
            <div className="mb-5 flex size-16 items-center justify-center rounded-full bg-blue/10 text-blue"><PackageOpen className="size-8" aria-hidden="true" /></div>
            <h3 className="text-lg font-semibold text-dark">No orders yet</h3>
            <p className="mt-2 max-w-sm text-sm text-dark-4">When you place an order, you can follow its progress and review the items here.</p>
            <Link href="/shop-with-sidebar" className="mt-6 inline-flex h-10 items-center gap-2 rounded-lg bg-blue px-5 text-sm font-medium text-white hover:bg-blue-dark focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue"><ShoppingBag className="size-4" aria-hidden="true" />Browse the shop</Link>
          </CardContent>}
        </Card>
      </div>
    </section>
  </main>;
}
