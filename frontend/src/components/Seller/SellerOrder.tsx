"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { ArrowLeft, PackageCheck, Truck } from "lucide-react";
import Breadcrumb from "@/components/Common/Breadcrumb";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { hasOrderStatusConflict, isSellerOrderPaid, orderDate, sellerOrderCurrency as currency } from "@/lib/sellerOrders";
import { StatusBadge } from "./Shared";
import { useSellerOrders } from "./useSellerOrders";

export default function SellerOrder({ orderId }: { orderId: string }) {
  const { data, loading, refreshing, error, refresh, acting, notice, perform } = useSellerOrders(orderId);
  const order = data?.orders[0];
  const [reason, setReason] = useState("");
  const [courier, setCourier] = useState("");
  const [tracking, setTracking] = useState("");
  const conflict = order ? hasOrderStatusConflict(order) : false;
  const disabled = acting || refreshing || Boolean(error) || conflict;
  function reject(event: FormEvent<HTMLFormElement>) { event.preventDefault(); void perform("reject", { reason }); }
  function ship(event: FormEvent<HTMLFormElement>) { event.preventDefault(); void perform("ship", { courierName: courier, trackingNumber: tracking }); }

  return <main>
    <Breadcrumb title="Seller Order" pages={["Seller", "Order"]} />
    <section className="bg-gray-2 py-12 sm:py-16"><div className="mx-auto w-full max-w-[1170px] px-4 sm:px-8 xl:px-0">
      <Link href="/seller-dashboard" className="mb-5 inline-flex items-center gap-2 text-sm font-medium text-blue hover:underline focus-visible:outline focus-visible:outline-2 focus-visible:outline-blue"><ArrowLeft className="size-4" />Back to dashboard</Link>
      {error && <div role="alert" className="mb-6 rounded-lg border border-red-light-4 bg-white p-4 text-sm text-dark"><p>{data ? "Showing previously loaded data. Order actions are disabled until the latest status is verified." : "The order could not be loaded."}</p><p className="mt-1">{error}</p><Button type="button" variant="outline" className="mt-3" disabled={refreshing || acting} onClick={refresh}>Retry</Button></div>}
      {loading ? <div aria-label="Loading order" aria-busy="true" className="space-y-6"><Skeleton className="h-14 w-2/3 motion-reduce:animate-none" /><div className="grid gap-7 lg:grid-cols-[minmax(0,1fr)_340px]"><Skeleton className="h-80 motion-reduce:animate-none" /><Skeleton className="h-64 motion-reduce:animate-none" /></div></div> : order && <>
        <div className="mb-7 flex flex-wrap items-end justify-between gap-4"><div className="min-w-0"><h2 className="break-all text-2xl font-semibold text-dark">Order #{order.subOrderNumber}</h2><p className="mt-1 text-sm text-dark-4">Placed {orderDate(order.createdAt)}{order.customerName ? ` by ${order.customerName}` : ""}</p><p className="mt-1 text-sm text-dark-4">Payment: {order.paymentStatus?.replaceAll("_", " ") || "Unavailable"}</p></div><StatusBadge status={order.orderStatus} /></div>
        {notice && <p role={notice.error ? "alert" : "status"} className="mb-6 rounded-lg border border-blue/20 bg-white p-4 text-sm text-dark">{notice.text}</p>}
        {conflict && <p role="alert" className="mb-6 rounded-lg border border-red-light-4 bg-white p-4 text-sm text-dark">Payment and order status are inconsistent. Seller actions are unavailable until the order data is corrected.</p>}
        <p role="status" className="mb-4 text-sm text-dark-4">{acting ? "Updating order and verifying its latest status…" : refreshing ? "Checking the latest order status…" : ""}</p>
        <div className="grid gap-7 lg:grid-cols-[minmax(0,1fr)_340px] lg:items-start">
          <div className="space-y-6">
            <Card><CardHeader className="border-b border-gray-3"><CardTitle>Order items</CardTitle></CardHeader><CardContent className="pt-6"><div className="space-y-4">{order.items.map(item => <div key={item.orderItemId} className="flex justify-between gap-4 border-b border-gray-3 pb-4 text-sm"><div className="min-w-0"><p className="break-words font-medium text-dark">{item.productName}</p><p className="mt-1 text-dark-4">{currency.format(item.unitPrice)} × {item.quantity}</p></div><span className="shrink-0 font-medium text-dark">{currency.format(item.totalPrice)}</span></div>)}</div><dl className="mt-5 space-y-3 text-sm text-dark"><div className="flex justify-between"><dt>Subtotal</dt><dd>{currency.format(order.subtotal)}</dd></div><div className="flex justify-between"><dt>Shipping</dt><dd>{currency.format(order.shippingFee)}</dd></div><div className="flex justify-between"><dt>Discount</dt><dd>{order.sellerDiscount === null ? "Unavailable" : currency.format(order.sellerDiscount)}</dd></div><div className="flex justify-between border-t border-gray-3 pt-4 font-semibold"><dt>Order total</dt><dd>{currency.format(order.totalAmount)}</dd></div></dl>{order.rejectionReason && <div className="mt-6 rounded-lg bg-red-light-6 p-4 text-sm"><strong className="text-dark">Rejection reason</strong><p className="mt-1 break-words">{order.rejectionReason}</p></div>}</CardContent></Card>
            <Card><CardHeader><CardTitle>Delivery details</CardTitle></CardHeader><CardContent className="space-y-3 text-sm text-dark">{order.shippingAddress ? <address className="space-y-1 break-words not-italic"><p className="font-medium">{order.shippingAddress.receiverName}</p><p>{order.shippingAddress.phoneNumber}</p><p>{order.shippingAddress.addressLine}</p><p>{[order.shippingAddress.district, order.shippingAddress.province, order.shippingAddress.postalCode].filter(Boolean).join(", ")}</p></address> : <p className="text-dark-4">Shipping address unavailable.</p>}<p>Shipping method: {order.shippingMethod || "Unavailable"}</p>{order.shipment && <div className="border-t border-gray-3 pt-3"><p className="font-medium">Shipment</p><p className="mt-1 break-words">Courier: {order.shipment.courierName || "Unavailable"}</p><p className="mt-1 break-all">Tracking: {order.shipment.trackingNumber || "Unavailable"}</p><p className="mt-1">{order.shipment.shippingStatus?.replaceAll("_", " ") || "Status unavailable"}</p></div>}</CardContent></Card>
          </div>
          <div className="space-y-6">
            {isSellerOrderPaid(order) && !conflict && order.orderStatus === "WAITING_SELLER_CONFIRM" && <><Card><CardHeader><CardTitle>Accept order</CardTitle><p className="text-sm text-dark-4">Confirm that your shop can prepare these items.</p></CardHeader><CardContent><Button type="button" disabled={disabled} className="h-10 w-full gap-2 bg-blue text-white hover:bg-blue-dark" onClick={() => void perform("accept", {})}><PackageCheck className="size-4" />Accept and confirm</Button></CardContent></Card><Card><CardHeader><CardTitle>Reject order</CardTitle><p className="text-sm text-dark-4">A reason is required. The payment for this shop will be refunded.</p></CardHeader><CardContent><form onSubmit={reject}><fieldset disabled={disabled} className="space-y-3"><Label htmlFor="reject-reason">Reason</Label><Input id="reject-reason" required maxLength={255} value={reason} onChange={event => setReason(event.target.value)} placeholder="Why can't you fulfil this order?" /><Button type="submit" variant="destructive" className="h-10 w-full">Reject order</Button></fieldset></form></CardContent></Card></>}
            {isSellerOrderPaid(order) && !conflict && order.orderStatus === "PREPARING" && <Card><CardHeader><CardTitle>Mark as shipped</CardTitle><p className="text-sm text-dark-4">Add the courier and tracking number.</p></CardHeader><CardContent><form onSubmit={ship}><fieldset disabled={disabled} className="space-y-4"><div className="space-y-2"><Label htmlFor="courier">Courier</Label><Input id="courier" required maxLength={100} value={courier} onChange={event => setCourier(event.target.value)} placeholder="Courier name" /></div><div className="space-y-2"><Label htmlFor="tracking">Tracking number</Label><Input id="tracking" required maxLength={100} value={tracking} onChange={event => setTracking(event.target.value)} placeholder="Tracking number" /></div><Button type="submit" className="h-10 w-full gap-2 bg-blue text-white hover:bg-blue-dark"><Truck className="size-4" />Mark as shipped</Button></fieldset></form></CardContent></Card>}
            {["SHIPPED", "COMPLETED", "CANCELLED", "PENDING_PAYMENT"].includes(order.orderStatus) && <Card><CardContent className="py-6 text-sm">{order.orderStatus === "PENDING_PAYMENT" ? "Waiting for customer payment. Seller actions are unavailable." : order.orderStatus === "SHIPPED" ? "This order has shipped. The customer confirms delivery." : "This order is closed. No seller actions are available."}</CardContent></Card>}
          </div>
        </div>
      </>}
    </div></section>
  </main>;
}
