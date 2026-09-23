"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { ArrowLeft, PackageCheck, Truck } from "lucide-react";
import Breadcrumb from "@/components/Common/Breadcrumb";
import { useSellerPreview } from "@/app/context/SellerPreviewContext";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { DemoNotice, SellerNav, StatusBadge } from "./Shared";

const currency = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });

export default function SellerOrder({ orderId }: { orderId: string }) {
  const { application, orders, acceptOrder, rejectOrder, shipOrder } = useSellerPreview();
  const order = orders.find((item) => String(item.orderId) === orderId);
  const [reason, setReason] = useState("");
  const [courier, setCourier] = useState("");
  const [tracking, setTracking] = useState("");
  const [message, setMessage] = useState("");

  function reject(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage(rejectOrder(Number(orderId), reason) ? "Order rejected in this demo. The reason is shown below." : "Enter a reason of up to 255 characters while the order awaits acceptance.");
  }
  function ship(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage(shipOrder(Number(orderId), courier, tracking) ? "Shipment marked as sent in this demo." : "Enter a courier and tracking number while the order is preparing.");
  }

  return <main>
    <Breadcrumb title="Seller Order" pages={["Seller", "Order"]} />
    <section className="bg-gray-2 py-12 sm:py-16"><div className="mx-auto w-full max-w-[1170px] px-4 sm:px-8 xl:px-0">
      <SellerNav />
      <DemoNotice>This page uses example orders. Accepting, rejecting and shipping change local preview state only.</DemoNotice>
      {application?.status !== "APPROVED" ? <Card><CardContent className="py-14 text-center"><h2 className="text-xl font-semibold text-dark">Shop approval required</h2><p className="mt-2 text-sm">You can manage orders after your application is approved.</p><Link href="/seller-application" className="mt-5 inline-flex h-10 items-center rounded-lg bg-blue px-5 text-sm font-medium text-white">View application</Link></CardContent></Card> : !order ? <Card><CardContent className="py-14 text-center"><h2 className="text-xl font-semibold text-dark">Order not found in this demo</h2><Link href="/seller-dashboard" className="mt-5 inline-flex text-sm font-medium text-blue hover:underline">Back to dashboard</Link></CardContent></Card> : <>
        <Link href="/seller-dashboard" className="mb-5 inline-flex items-center gap-2 text-sm font-medium text-blue hover:underline"><ArrowLeft className="size-4" />Back to dashboard</Link>
        <div className="mb-7 flex flex-wrap items-end justify-between gap-4"><div><h2 className="text-2xl font-semibold text-dark">Order #{order.orderNumber}</h2><p className="mt-1 text-sm text-dark-4">Placed {order.createdAt} by {order.customer}</p></div><StatusBadge status={order.status} /></div>
        {message && <p role="status" className="mb-6 rounded-lg border border-blue/20 bg-blue/5 p-4 text-sm text-dark">{message}</p>}
        <div className="grid gap-7 lg:grid-cols-[minmax(0,1fr)_340px] lg:items-start">
          <Card><CardHeader className="border-b border-gray-3"><CardTitle>Order items</CardTitle></CardHeader><CardContent className="pt-6"><div className="space-y-4">{order.items.map((item) => <div key={item.name} className="flex justify-between gap-4 border-b border-gray-3 pb-4 text-sm"><div><p className="font-medium text-dark">{item.name}</p><p className="mt-1 text-dark-4">{currency.format(item.unitPrice)} × {item.quantity}</p></div><span className="font-medium text-dark">{currency.format(item.unitPrice * item.quantity)}</span></div>)}</div><div className="mt-5 flex justify-between font-semibold text-dark"><span>Order total</span><span>{currency.format(order.totalAmount)}</span></div>{order.rejectionReason && <div className="mt-6 rounded-lg bg-red-light-6 p-4 text-sm"><strong className="text-dark">Rejection reason</strong><p className="mt-1">{order.rejectionReason}</p></div>}{order.trackingNumber && <div className="mt-6 rounded-lg bg-blue/5 p-4 text-sm"><strong className="text-dark">Shipment</strong><p className="mt-1">{order.courierName} · {order.trackingNumber}</p></div>}</CardContent></Card>
          <div className="space-y-6">
            {order.status === "WAITING_SELLER_CONFIRM" && <><Card><CardHeader><CardTitle>Accept order</CardTitle><p className="text-sm text-dark-4">Confirm that your shop can prepare these items.</p></CardHeader><CardContent><Button type="button" className="h-10 w-full gap-2 bg-blue text-white hover:bg-blue-dark" onClick={() => setMessage(acceptOrder(order.orderId) ? "Order accepted in this demo. It is now preparing." : "This order can no longer be accepted.")}><PackageCheck className="size-4" />Accept and confirm</Button></CardContent></Card><Card><CardHeader><CardTitle>Reject order</CardTitle><p className="text-sm text-dark-4">Use this only while the order awaits acceptance. A reason is required.</p></CardHeader><CardContent><form onSubmit={reject} className="space-y-3"><Label htmlFor="reject-reason">Reason</Label><Input id="reject-reason" required maxLength={255} value={reason} onChange={(event) => setReason(event.target.value)} placeholder="Why can't you fulfil this order?" /><Button type="submit" variant="destructive" className="h-10 w-full">Reject order</Button></form></CardContent></Card></>}
            {order.status === "PREPARING" && <Card><CardHeader><CardTitle>Mark as shipped</CardTitle><p className="text-sm text-dark-4">Add the courier and tracking number before notifying the customer.</p></CardHeader><CardContent><form onSubmit={ship} className="space-y-4"><div className="space-y-2"><Label htmlFor="courier">Courier</Label><Input id="courier" required maxLength={100} value={courier} onChange={(event) => setCourier(event.target.value)} placeholder="Courier name" /></div><div className="space-y-2"><Label htmlFor="tracking">Tracking number</Label><Input id="tracking" required maxLength={100} value={tracking} onChange={(event) => setTracking(event.target.value)} placeholder="Tracking number" /></div><Button type="submit" className="h-10 w-full gap-2 bg-blue text-white hover:bg-blue-dark"><Truck className="size-4" />Mark as shipped</Button></form></CardContent></Card>}
            {(order.status === "SHIPPED" || order.status === "COMPLETED" || order.status === "CANCELLED" || order.status === "PENDING_PAYMENT") && <Card><CardContent className="py-6 text-sm">{order.status === "PENDING_PAYMENT" ? "Waiting for customer payment. Seller actions are unavailable." : order.status === "SHIPPED" ? "This order has shipped. The customer confirms delivery." : "This order is closed. No seller actions are available."}</CardContent></Card>}
          </div>
        </div>
      </>}
    </div></section>
  </main>;
}
