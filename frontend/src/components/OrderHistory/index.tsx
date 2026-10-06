"use client";
import { bahtCurrency } from "@/lib/currency";

import { useState, useEffect } from "react";
import Link from "next/link";
import { ChevronDown, PackageOpen, ShoppingBag, RefreshCw, Star, Check } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@/app/context/AuthContext";
import { checkoutApi, methodLabel } from "@/components/Checkout/api";
import Breadcrumb from "@/components/Common/Breadcrumb";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import ConfirmDialog from "@/components/Common/ConfirmDialog";
import ReviewModal from "@/components/Common/ReviewModal";
import OrderStatusBadge, { orderStatusLabels as statusLabels, type OrderStatus } from "@/components/Common/OrderStatusBadge";
import toast from "react-hot-toast";

export type CustomerOrder = {
  id: string;
  orderId: number;
  orderGroupId: number;
  date: string;
  status: OrderStatus;
  total: number;
  items: {
    orderItemId?: number;
    productId?: number;
    name: string;
    quantity: number;
    price: number;
    isReviewed?: boolean;
  }[];
  shippingAddress?: string;
  shippingFee: number;
  shippingMethod: string | null;
  paymentStatus: string;
  groupNumber: string;
};

const currency = bahtCurrency;
const orderColumns = "lg:grid-cols-[minmax(0,1.2fr)_minmax(0,0.8fr)_minmax(0,1.7fr)_minmax(0,0.6fr)_260px]";

function ReviewAction({ reviewed, onClick }: { reviewed: boolean; onClick?: () => void }) {
  const layout = "inline-flex h-11 min-w-[124px] shrink-0 items-center justify-center gap-2 rounded-lg px-4 text-sm font-medium";

  if (reviewed) {
    return (
      <span className={`${layout} border border-green-light-3 bg-green-light-6 text-green-dark`} title="All selected items have been reviewed.">
        <Check size={16} aria-hidden="true" /> Reviewed
      </span>
    );
  }

  return (
    <button type="button" onClick={onClick} className={`${layout} bg-blue text-white transition-colors hover:bg-blue-dark focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue`}>
      <Star size={16} aria-hidden="true" /> Review
    </button>
  );
}

type OrderGroup = {
  orderGroupId: number; groupNumber: string; createdAt: string; paymentStatus: string;
  subOrders: { orderId: number; subOrderNumber: string; orderStatus: CustomerOrder["status"];
    totalAmount: number; shippingFee: number; shippingMethod: string | null;
    items: {
      orderItemId?: number;
      productId?: number;
      productName: string;
      quantity: number;
      unitPrice: number;
      isReviewed?: boolean;
    }[] }[];
};
async function loadOrders(signal?: AbortSignal): Promise<CustomerOrder[]> {
  const groups = await checkoutApi<OrderGroup[]>("orders", undefined, signal);
  return groups.flatMap(group => group.subOrders.map(order => ({
    id: order.subOrderNumber || String(order.orderId),
    orderId: order.orderId,
    orderGroupId: group.orderGroupId,
    date: new Date(group.createdAt).toLocaleDateString("en-US", { year: "numeric", month: "short", day: "numeric" }),
    status: order.orderStatus, total: Number(order.totalAmount),
    shippingFee: Number(order.shippingFee), shippingMethod: order.shippingMethod,
    paymentStatus: group.paymentStatus, groupNumber: group.groupNumber,
    items: order.items.map(item => ({
      orderItemId: item.orderItemId,
      productId: item.productId,
      name: item.productName,
      quantity: item.quantity,
      price: Number(item.unitPrice),
      isReviewed: item.isReviewed,
    })),
  })));
}

function OrderRow({ order, onRefresh }: { order: CustomerOrder; onRefresh: () => void }) {
  const [expanded, setExpanded] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [showConfirmDelivered, setShowConfirmDelivered] = useState(false);
  const [showCancelOrder, setShowCancelOrder] = useState(false);
  const [reviewingBatch, setReviewingBatch] = useState<{ orderItemIds: number[]; productNames: string[] } | null>(null);

  const [reviewedItemIds, setReviewedItemIds] = useState<Set<number>>(() => {
    if (typeof window !== "undefined") {
      try {
        const saved = localStorage.getItem("pandastore_reviewed_items");
        if (saved) return new Set(JSON.parse(saved).map(Number));
      } catch {}
    }
    return new Set();
  });

  const markItemsAsReviewed = (ids: number[]) => {
    setReviewedItemIds(prev => {
      const next = new Set(prev);
      ids.forEach(id => next.add(id));
      try {
        localStorage.setItem("pandastore_reviewed_items", JSON.stringify(Array.from(next)));
      } catch {}
      return next;
    });
  };

  const isItemReviewed = (item: { orderItemId?: number; isReviewed?: boolean }) => {
    if (item.isReviewed) return true;
    if (item.orderItemId && reviewedItemIds.has(item.orderItemId)) return true;
    return false;
  };

  useEffect(() => {
    if (order.status !== "COMPLETED") return;
    order.items.forEach(async (item) => {
      if (!item.orderItemId || item.isReviewed || reviewedItemIds.has(item.orderItemId)) return;
      try {
        const res = await fetch(`/api/reviews/check-eligibility?orderItemId=${item.orderItemId}`);
        const data = await res.json().catch(() => null);
        if (res.ok && data?.success && data?.data === false) {
          markItemsAsReviewed([item.orderItemId]);
        }
      } catch {}
    });
  }, [order.status, order.items]);

  const allReviewed = order.items.length > 0 && order.items.every(i => isItemReviewed(i));

  async function handleConfirmDelivered() {
    setActionLoading(true);
    try {
      const res = await fetch(`/api/sub-orders/${order.orderId}/confirm-delivered`, { method: "POST" });
      const data = await res.json().catch(() => null);
      if (!res.ok || !data?.success) {
        toast.error(data?.message || "Failed to confirm delivery");
      } else {
        toast.success("Delivery confirmed successfully!");
        onRefresh();
        const unreviewed = order.items.filter(i => !isItemReviewed(i) && i.orderItemId);
        if (unreviewed.length > 0) {
          setReviewingBatch({
            orderItemIds: unreviewed.map(i => i.orderItemId!),
            productNames: unreviewed.map(i => i.name),
          });
        }
      }
    } catch {
      toast.error("Could not connect to service. Please try again.");
    } finally {
      setActionLoading(false);
    }
  }

  async function handleCancelOrder(reason?: string) {
    if (!reason || !reason.trim()) {
      toast.error("Please enter a reason for cancellation");
      return;
    }
    setActionLoading(true);
    try {
      const res = await fetch(`/api/sub-orders/${order.orderId}/cancel`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ reason: reason.trim() }),
      });
      const data = await res.json().catch(() => null);
      if (!res.ok || !data?.success) {
        toast.error(data?.message || "Failed to cancel order");
      } else {
        toast.success("Order cancelled successfully");
        onRefresh();
      }
    } catch {
      toast.error("Could not connect to service. Please try again.");
    } finally {
      setActionLoading(false);
    }
  }

  return (
    <div className="border-t border-gray-3">
      <div className={`grid gap-4 px-5 py-5 text-sm sm:px-7 lg:items-center ${orderColumns}`}>
        <div className="min-w-0"><span className="block text-xs text-dark-4 lg:hidden">Order</span><strong className="break-all font-medium text-dark">#{order.id}</strong></div>
        <div><span className="block text-xs text-dark-4 lg:hidden">Date</span><span className="text-dark">{order.date}</span></div>
        <div className="min-w-0">
          <span className="block text-xs text-dark-4 lg:hidden">Order status</span>
          <OrderStatusBadge status={order.status} />
        </div>
        <div className="lg:text-right"><span className="block text-xs text-dark-4 lg:hidden">Total</span><span className="whitespace-nowrap font-medium text-dark">{currency.format(order.total)}</span></div>
        <div className="flex flex-wrap items-center gap-2 lg:justify-end">
          {order.status === "PENDING_PAYMENT" && order.paymentStatus === "PENDING" && <Link href={`/payment/${order.orderGroupId}`} aria-label={`Pay now for order group ${order.orderGroupId}`} className="inline-flex h-9 items-center justify-center rounded-lg bg-blue px-3 text-sm text-white hover:bg-blue-dark focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue">Pay now</Link>}
          {(order.status === "PENDING_PAYMENT" || order.status === "WAITING_SELLER_CONFIRM" || order.status === "PREPARING") && (
            <Button variant="outline" className="h-9 min-w-[112px] rounded-lg border-red/30 bg-white px-3 text-red hover:border-red/50 hover:bg-red/5 hover:text-red" disabled={actionLoading} onClick={() => setShowCancelOrder(true)}>
              Cancel
            </Button>
          )}
          {order.status === "SHIPPED" && (
            <Button size="sm" className="h-9 px-3 bg-green-dark hover:bg-green text-white font-medium shadow-sm" disabled={actionLoading} onClick={() => setShowConfirmDelivered(true)}>
              Confirm Delivery
            </Button>
          )}
          {order.status === "COMPLETED" && (
            allReviewed ? (
              <ReviewAction reviewed />
            ) : (
              <ReviewAction
                reviewed={false}
                onClick={() => {
                  const unreviewed = order.items.filter(i => !isItemReviewed(i) && i.orderItemId);
                  if (unreviewed.length > 0) {
                    setReviewingBatch({
                      orderItemIds: unreviewed.map(i => i.orderItemId!),
                      productNames: unreviewed.map(i => i.name),
                    });
                  }
                }}
              />
            )
          )}
          <Button variant="outline" className="h-9 min-w-[112px] rounded-lg px-3" aria-expanded={expanded} aria-controls={`order-${order.id}`} onClick={() => setExpanded(!expanded)}>
            Details <ChevronDown className={`ml-1 h-4 w-4 transition-transform motion-reduce:transition-none ${expanded ? "rotate-180" : ""}`} />
          </Button>
        </div>
      </div>
      {expanded && <div id={`order-${order.id}`} className="border-t border-gray-3 bg-gray-1 px-5 py-6 sm:px-7">
        <h3 className="mb-4 font-medium text-dark">Items in this order</h3>
        <div className="space-y-3">
          {order.items.map((item, index) => (
            <div key={`${item.name}-${index}`} className="flex flex-wrap items-center justify-between gap-3 text-sm rounded-lg bg-white p-3 border border-gray-3">
              <div className="min-w-0 flex-1">
                <span className="font-medium text-dark">{item.name}</span>
                <span className="ml-2 text-xs text-dark-4">× {item.quantity}</span>
              </div>
              <div className="flex max-w-full flex-wrap items-center gap-3">
                <span className="font-medium text-dark">{currency.format(item.price * item.quantity)}</span>
                {order.status === "COMPLETED" && item.orderItemId && (
                  isItemReviewed(item) ? (
                    <ReviewAction reviewed />
                  ) : (
                    <ReviewAction
                      reviewed={false}
                      onClick={() => setReviewingBatch({
                        orderItemIds: [item.orderItemId!],
                        productNames: [item.name],
                      })}
                    />
                  )
                )}
              </div>
            </div>
          ))}
        </div>
        <div className="mt-5 space-y-2 border-t border-gray-3 pt-4 text-sm">
          <p>Order group: {order.groupNumber}</p>
          <p>Payment: {order.paymentStatus.replaceAll("_", " ")}</p>
          <div className="flex justify-between gap-5"><span>Shipping{order.shippingMethod ? ` · ${methodLabel(order.shippingMethod)}` : ""}</span><span>{currency.format(order.shippingFee)}</span></div>
          <div className="flex justify-between gap-5 font-medium text-dark"><span>Total</span><span>{currency.format(order.total)}</span></div>
        </div>
        {order.shippingAddress && <p className="mt-6 border-t border-gray-3 pt-4 text-sm"><strong className="text-dark">Shipping address</strong><br />{order.shippingAddress}</p>}
      </div>}

      <ConfirmDialog
        isOpen={showConfirmDelivered}
        onClose={() => setShowConfirmDelivered(false)}
        onConfirm={handleConfirmDelivered}
        title="Confirm Order Delivery"
        description="Are you sure you have received all items in this order? This will mark the order as delivered and completed."
        confirmText="Yes, I Received It"
        cancelText="Not Yet"
        variant="primary"
        icon="check"
        loading={actionLoading}
      />

      <ConfirmDialog
        isOpen={showCancelOrder}
        onClose={() => setShowCancelOrder(false)}
        onConfirm={handleCancelOrder}
        title="Cancel Order"
        description="Please provide a reason for cancelling this order so the seller knows why."
        confirmText="Confirm Cancellation"
        cancelText="Keep Order"
        variant="danger"
        icon="alert"
        showInput={true}
        inputLabel="Reason for cancellation"
        inputPlaceholder="e.g. Changed my mind, ordered by mistake..."
        inputDefaultValue="I changed my mind"
        inputRequired={true}
        loading={actionLoading}
      />

      {reviewingBatch && (
        <ReviewModal
          isOpen={true}
          orderItemIds={reviewingBatch.orderItemIds}
          productNames={reviewingBatch.productNames}
          onClose={() => setReviewingBatch(null)}
          onSuccess={(reviewedIds) => {
            markItemsAsReviewed(reviewedIds);
            setReviewingBatch(null);
            onRefresh();
          }}
        />
      )}
    </div>
  );
}

export default function OrderHistory({ showBackToAccount = false }: { showBackToAccount?: boolean }) {
  const { user, isLoading: authLoading } = useAuth();
  const customer = user?.role === "CUSTOMER";
  const history = useQuery({ queryKey: ["customer-orders", user?.id],
    queryFn: ({ signal }) => loadOrders(signal), enabled: !authLoading && customer,
    staleTime: 0, gcTime: 0, refetchInterval: 30000, refetchIntervalInBackground: false,
    refetchOnWindowFocus: "always", refetchOnReconnect: "always", retry: 1 });
  const orders = customer ? history.data || [] : [];
  const [status, setStatus] = useState("all");
  const visibleOrders = status === "all" ? orders : orders.filter((order) => order.status === status);

  return <main>
    <div className="pt-12 sm:pt-8 xl:pt-0"><Breadcrumb title="Order History" pages={["Order History"]} /></div>
    <section className="bg-gray-2 py-12 sm:py-16">
      <div className="mx-auto w-full max-w-[1170px] px-4 sm:px-8 xl:px-0">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div><h2 className="text-2xl font-semibold text-dark">Your orders</h2><p className="mt-2 text-sm text-dark-4">Track purchases and review order details here.</p></div>
          {showBackToAccount && <Link href="/my-account" className="text-sm font-medium text-blue hover:underline">Back to My Account</Link>}
        </div>
        <Card className="overflow-hidden rounded-xl border-gray-3 shadow-1">
          <div className="flex flex-col gap-4 border-b border-gray-3 px-5 py-5 sm:flex-row sm:items-center sm:justify-between sm:px-7">
            <div className="flex items-center gap-3"><h3 className="font-medium text-dark">Order history</h3>
              {customer && <Button variant="outline" size="sm" onClick={() => void history.refetch()} disabled={history.isFetching} aria-label="Refresh orders"><RefreshCw className={`size-4 ${history.isFetching ? "animate-spin" : ""}`} />Refresh</Button>}
            </div>
            <label className="flex items-center gap-2 text-sm text-dark"><span>Status</span><select value={status} onChange={(event) => setStatus(event.target.value)} className="h-9 rounded-lg border border-gray-3 bg-white px-3 outline-none focus:ring-2 focus:ring-blue/30"><option value="all">All orders</option>{Object.entries(statusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
          </div>
          <div className={`hidden gap-4 bg-gray-1 px-7 py-4 text-xs font-medium text-dark lg:grid ${orderColumns}`}><span>Order</span><span>Date</span><span>Order status</span><span className="text-right">Total</span><span className="text-right">Action</span></div>
          {authLoading || (customer && history.isPending) ? <p role="status" className="px-7 py-16 text-center text-sm">Loading your orders…</p>
          : !customer ? <CardContent className="py-16 text-center"><h3 className="font-semibold text-dark">{user ? "Order history is available for customer accounts" : "Sign in to view your orders"}</h3>{!user && <Link href="/signin" className="mt-4 inline-block text-blue hover:underline">Sign in</Link>}</CardContent>
          : history.isError ? <CardContent role="alert" className="py-16 text-center"><p className="text-red">{history.error.message}</p><Button variant="outline" className="mt-4" onClick={() => void history.refetch()} disabled={history.isFetching}>Try again</Button></CardContent>
          : orders.length > 0 ? <>
            {visibleOrders.length > 0 ? visibleOrders.map((order) => <OrderRow key={order.id} order={order} onRefresh={() => void history.refetch()} />) : <p className="px-7 py-10 text-center text-sm">No orders with this status.</p>}
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
