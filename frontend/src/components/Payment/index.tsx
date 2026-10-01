"use client";

import { useRef, useState, useTransition } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { CheckCircle2, Loader2, QrCode } from "lucide-react";
import Breadcrumb from "@/components/Common/Breadcrumb";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { money } from "@/components/Checkout/api";
import { confirmPayment, isPendingPayment, paymentStatusLabel, type PaymentOrder } from "./api";

const focus = "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue";
const primary = `h-11 rounded-lg bg-blue px-5 text-white hover:bg-blue-dark ${focus}`;
const outline = `h-11 rounded-lg border border-gray-3 bg-white px-5 text-dark hover:bg-gray-1 ${focus}`;

export default function Payment({ initialOrder, qrSource, loadError = false }: {
  initialOrder: PaymentOrder | null; qrSource: string; loadError?: boolean;
}) {
  const router = useRouter();
  const [updatedOrder, setOrder] = useState<PaymentOrder | null>(null);
  const order = updatedOrder || initialOrder;
  const [processing, setProcessing] = useState(false);
  const inFlight = useRef(false);
  const [refreshing, startRefresh] = useTransition();
  const [error, setError] = useState("");
  const [confirmed, setConfirmed] = useState(false);
  const [qrFailed, setQrFailed] = useState(false);
  const pending = order && isPendingPayment(order);
  const busy = processing || refreshing;

  async function submit() {
    if (!order || !pending || inFlight.current) return;
    inFlight.current = true;
    setProcessing(true);
    setError("");
    try {
      const result = await confirmPayment(order.orderGroupId);
      setOrder(result);
      setConfirmed(true);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Could not update your order. Please try again.");
    } finally {
      inFlight.current = false;
      setProcessing(false);
    }
  }

  return <main>
    <div className="pt-6 sm:pt-8 xl:pt-7"><Breadcrumb title="Payment" pages={["Payment"]} /></div>
    <section className="bg-gray-2 px-4 py-12 sm:px-8 sm:py-16">
      <Card className="mx-auto w-full max-w-xl rounded-xl border-gray-3 shadow-1">
        <CardContent className="p-6 sm:p-10">
          {loadError || !order ? <div role="alert" className="text-center">
            <h2 className="text-xl font-semibold text-dark">Could not load your payment</h2>
            <p className="mt-3 text-sm text-dark-4">The order service is unavailable. Please try again.</p>
            <Button className={`mt-6 ${primary}`} disabled={refreshing} onClick={() => startRefresh(() => router.refresh())}>Try again</Button>
          </div> : <>
            <div className="text-center">
              {pending ? <QrCode aria-hidden="true" className="mx-auto mb-4 h-8 w-8 text-blue" />
                : <CheckCircle2 aria-hidden="true" className="mx-auto mb-4 h-8 w-8 text-blue" />}
              <h2 className="text-2xl font-semibold text-dark">{pending ? "Scan to pay" : confirmed ? "Payment successful (demo)" : paymentStatusLabel(order)}</h2>
              <p className="mt-3 text-sm text-dark">Order group #{order.orderGroupId}</p>
              <p className="mt-1 break-all text-xs text-dark-4">{order.groupNumber}</p>
            </div>
            <dl className="mt-6 space-y-3 border-y border-gray-3 py-5 text-sm">
              <div className="flex justify-between gap-4"><dt>Products</dt><dd>{money(Number(order.totalProductsAmount))}</dd></div>
              <div className="flex justify-between gap-4"><dt>Shipping</dt><dd>{money(Number(order.totalShippingFee))}</dd></div>
              <div className="flex items-center justify-between gap-4 font-semibold text-dark"><dt>{pending ? "Total to pay" : "Order total"}</dt><dd className="text-2xl">{money(Number(order.grandTotal))}</dd></div>
            </dl>
            {pending ? <>
              <div className="mx-auto my-7 flex aspect-square w-full max-w-[260px] items-center justify-center overflow-hidden rounded-lg border border-gray-3 bg-gray-1">
                {qrFailed ? <p role="status" className="px-6 text-center text-sm text-dark-4">QR code image not found</p>
                  : <Image src={qrSource} alt="Payment QR code" width={260} height={260} unoptimized className="h-full w-full object-contain" onError={() => setQrFailed(true)} />}
              </div>
              <p className="text-center text-sm text-dark-4">This is a demo. No real payment is processed.</p>
              {error && <div role="alert" className="mt-5 rounded-lg border border-red/20 bg-red/5 p-4 text-sm text-red">
                <p>{error}</p><Button variant="outline" className={`mt-3 ${outline}`} disabled={busy} onClick={() => startRefresh(() => router.refresh())}>Refresh order status</Button>
              </div>}
              <div className="mt-7 flex flex-col gap-3 sm:flex-row" aria-busy={busy}>
                <Button variant="outline" className={`w-full sm:flex-1 ${outline}`} disabled aria-label="Cancel order" aria-describedby="payment-cancellation-note">
                  Cancel order
                </Button>
                <Button className={`w-full sm:flex-1 ${primary}`} disabled={busy} aria-label="Confirm payment" onClick={() => void submit()}>
                  {processing && <Loader2 aria-hidden="true" className="mr-2 h-4 w-4 animate-spin motion-reduce:animate-none" />}Confirm payment
                </Button>
              </div>
              <p id="payment-cancellation-note" className="mt-3 text-center text-xs text-dark-4">Cancellation is not available while payment is pending.</p>
              {processing && <p role="status" className="mt-3 text-center text-sm text-dark-4">Confirming payment…</p>}
            </> : <div role="status" className="mt-6 text-center">
              <p className="text-sm text-dark-4">{confirmed ? "Your payment is recorded. The seller can now confirm your order." : "This order is no longer pending payment."}</p>
              <Link href="/order-history" className={`mt-6 inline-flex items-center justify-center text-sm font-medium ${primary}`}>View Order History</Link>
            </div>}
          </>}
        </CardContent>
      </Card>
    </section>
  </main>;
}
