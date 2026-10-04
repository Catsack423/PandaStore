"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ClipboardList, FileCheck2, Store } from "lucide-react";
import Breadcrumb from "@/components/Common/Breadcrumb";
import { useAuth, type User } from "@/app/context/AuthContext";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Spinner } from "@/components/ui/spinner";
import ApplicationHistoryTable, { getReviewNote, statusColors, statusDescriptions, statusLabels } from "./ApplicationHistoryTable";
import type { SellerApplicationRecord } from "@/types/sellerApplication";

export default function SellerApplication() {
  const { user, isLoading, refreshUser } = useAuth();
  const router = useRouter();
  const userId = user?.id;
  const auth = useRef({ user, refreshUser });
  const redirecting = useRef(false);
  const [applications, setApplications] = useState<SellerApplicationRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reload, setReload] = useState(0);
  const [approvalAccount, setApprovalAccount] = useState<{ userId: string; previousUser: User | null } | null>(null);

  useEffect(() => {
    auth.current = { user, refreshUser };
  }, [user, refreshUser]);

  useEffect(() => {
    if (isLoading) return;
    redirecting.current = false;
    setApprovalAccount(null);
    if (!userId) { setApplications([]); setLoading(false); return; }
    let active = true;
    let inFlight = false;
    let latestStatus: SellerApplicationRecord["status"] | undefined;
    let awaitingAccess = false;
    const controller = new AbortController();

    async function loadApplications(initial = false) {
      if (!active || inFlight || redirecting.current || (!initial && (document.visibilityState !== "visible" || (latestStatus !== "PENDING" && !awaitingAccess)))) return;
      inFlight = true;
      if (initial) setLoading(true);
      try {
        const response = await fetch("/api/seller-applications", { cache: "no-store", signal: controller.signal });
        const body = await response.json();
        if (!response.ok || !body.success || !Array.isArray(body.data)) throw new Error(body.message || "Could not load applications");
        if (!active) return;
        const wasPending = latestStatus === "PENDING";
        latestStatus = body.data[0]?.status;
        setApplications(body.data);
        setError("");
        if (latestStatus === "APPROVED" && (wasPending || awaitingAccess || auth.current.user?.role === "CUSTOMER")) {
          awaitingAccess = true;
          const previousUser = auth.current.user;
          await auth.current.refreshUser();
          if (active) setApprovalAccount({ userId, previousUser });
        } else {
          awaitingAccess = false;
          setApprovalAccount(null);
        }
      } catch (reason) {
        if (active) setError(reason instanceof Error ? reason.message : "Could not load applications");
      } finally {
        inFlight = false;
        if (active && initial) setLoading(false);
      }
    }

    void loadApplications(true);
    const interval = window.setInterval(() => void loadApplications(), 60_000);
    const onFocus = () => void loadApplications();
    window.addEventListener("focus", onFocus);
    document.addEventListener("visibilitychange", onFocus);
    return () => {
      active = false;
      controller.abort();
      window.clearInterval(interval);
      window.removeEventListener("focus", onFocus);
      document.removeEventListener("visibilitychange", onFocus);
    };
  }, [userId, isLoading, reload]);

  useEffect(() => {
    // A failed refresh keeps the same identity object; only a newly verified seller can enter.
    if (!redirecting.current && !isLoading && approvalAccount && approvalAccount.userId === user?.id && user !== approvalAccount.previousUser && user?.role === "SELLER" && user.status === "ACTIVE") {
      redirecting.current = true;
      router.replace("/seller-dashboard");
    }
  }, [approvalAccount, user, isLoading, router]);

  const latest = applications[0];
  const canApply = !latest || latest.status === "REJECTED" || latest.status === "NEED_MORE_DOC";
  const applyLabel = latest?.status === "NEED_MORE_DOC" ? "Update application" : latest?.status === "REJECTED" ? "Revise and reapply" : "Apply to sell";

  return <main>
    <Breadcrumb title="Seller Applications" pages={["Seller Applications"]} />
    <section className="bg-gray-2 py-12 sm:py-16"><div className="mx-auto w-full max-w-[1050px] px-4 sm:px-8 xl:px-0">
      <h2 className="mb-7 text-2xl font-semibold text-dark">Seller applications</h2>
      {isLoading ? <Card><CardContent className="py-16 text-center">Checking your account...</CardContent></Card>
        : !user ? <Card><CardContent className="flex flex-col items-center px-6 py-14 text-center"><Store className="mb-4 size-10 text-blue" /><h2 className="text-xl font-semibold text-dark">Start with a customer account</h2><p className="mt-2 max-w-md text-sm">Sign in or create a customer account before applying to open a shop.</p><div className="mt-6 flex gap-3"><Link href="/signin?callbackUrl=%2Fseller-application" className="rounded-lg bg-blue px-5 py-3 text-sm font-medium text-white">Sign in</Link><Link href="/signup" className="rounded-lg border border-gray-3 px-5 py-3 text-sm font-medium text-dark">Create account</Link></div></CardContent></Card>
        : loading ? <Card><CardContent className="py-16 text-center">Loading applications...</CardContent></Card>
        : error ? <Card><CardContent className="py-12 text-center"><p role="alert" className="text-red">{error}</p><Button type="button" variant="outline" onClick={() => setReload((value) => value + 1)} className="mt-4">Try again</Button></CardContent></Card>
        : <div className="space-y-6">
          {latest && (latest.status === "PENDING" ?
            <Card className="overflow-hidden border-yellow/60">
              <CardContent className="p-0">
                <div className="flex flex-col items-start gap-5 bg-yellow-light-4 px-6 py-7 sm:flex-row sm:px-8">
                  <span className="flex size-12 shrink-0 items-center justify-center rounded-full border border-yellow/50 bg-white text-yellow-dark-2">
                    <Spinner className="size-6" aria-label="Application under review" />
                  </span>
                  <div>
                    <h2 className="text-2xl font-semibold leading-tight text-dark">Waiting for review</h2>
                    <p className="mt-2 max-w-xl text-sm leading-6 text-dark-3">{statusDescriptions.PENDING}</p>
                  </div>
                </div>
                <div className="flex flex-wrap items-center justify-between gap-x-6 gap-y-2 border-t border-yellow/30 px-6 py-4 text-sm sm:px-8">
                  <p className="font-medium text-dark">{latest.shopName}</p>
                  <p className="text-dark-3">Application #{latest.applicationId} · Submitted {new Date(latest.createdAt).toLocaleDateString("en-US")}</p>
                </div>
              </CardContent>
            </Card>
            : <Card><CardHeader><CardTitle>Latest application · #{latest.applicationId}</CardTitle></CardHeader><CardContent className="space-y-4"><div className="flex flex-wrap items-center gap-3"><h2 className="text-xl font-semibold text-dark">{latest.shopName}</h2><span className={`rounded-full px-3 py-1 text-xs font-medium ${statusColors[latest.status]}`}>{statusLabels[latest.status]}</span></div>
              <p className="text-sm leading-6">{statusDescriptions[latest.status]}</p>
              {(latest.adminNote || canApply) && <div className="rounded-lg border border-yellow/30 bg-yellow-light-4 p-4 text-sm"><strong className="text-dark">{latest.status === "REJECTED" ? "เหตุผลที่ไม่ผ่านการอนุมัติ" : latest.status === "NEED_MORE_DOC" ? "สิ่งที่ต้องแก้ไข" : "Review note"}</strong><p className="mt-2 whitespace-pre-line break-words leading-6">{getReviewNote(latest)}</p></div>}
              {canApply && <div><Link href="/seller-application/apply" className="inline-flex min-h-[44px] items-center rounded-lg bg-blue px-5 py-2 text-sm font-medium text-white hover:bg-blue-dark">{applyLabel}</Link><p className="mt-2 text-xs leading-5 text-dark-4">แบบฟอร์มจะเติมข้อมูลจากคำขอล่าสุดให้ แก้ไขเฉพาะข้อมูลหรือเอกสารที่ต้องการ แล้วส่งให้ตรวจสอบอีกครั้ง</p></div>}
              {latest.status === "APPROVED" && user?.role === "SELLER" && <Link href="/seller-dashboard" className="inline-flex h-10 items-center rounded-lg bg-blue px-5 text-sm font-medium text-white hover:bg-blue-dark">Open dashboard</Link>}
            </CardContent></Card>)}
          {!latest && <Card><CardContent className="flex flex-col items-center py-14 text-center"><ClipboardList className="mb-4 size-10 text-blue" /><h2 className="text-xl font-semibold text-dark">No applications yet</h2><p className="mt-2 text-sm">Your customer account is ready. Complete the shop application to get started.</p><Link href="/seller-application/apply" className="mt-6 rounded-lg bg-blue px-5 py-3 text-sm font-medium text-white">Apply to sell</Link></CardContent></Card>}
          {canApply && <Card><CardHeader><CardTitle>เตรียมข้อมูลก่อนสมัคร</CardTitle><p className="text-sm leading-6 text-dark-4">กรอกแบบฟอร์ม 2 ขั้นตอน แล้วส่งให้ทีมงานตรวจสอบ</p></CardHeader><CardContent className="grid gap-5 sm:grid-cols-3">
            <div className="min-w-0"><Store size={24} className="mb-3 text-blue" aria-hidden="true" /><h3 className="font-medium text-dark">1. ข้อมูลร้านค้า</h3><p className="mt-2 text-sm leading-6">ชื่อและรายละเอียดร้าน เบอร์โทร อีเมล และที่อยู่ร้านค้า</p></div>
            <div className="min-w-0"><FileCheck2 size={24} className="mb-3 text-blue" aria-hidden="true" /><h3 className="font-medium text-dark">2. ข้อมูลผู้ขายและบัญชี</h3><p className="mt-2 text-sm leading-6">ชื่อจริง เลขบัตรประชาชน ข้อมูลบัญชีธนาคาร พร้อมภาพบัตรประชาชนและสมุดบัญชี</p></div>
            <div className="min-w-0"><ClipboardList size={24} className="mb-3 text-blue" aria-hidden="true" /><h3 className="font-medium text-dark">ส่งและติดตามผล</h3><p className="mt-2 text-sm leading-6">ตรวจสอบข้อมูลก่อนส่ง แล้วกลับมาดูสถานะและหมายเหตุจากทีมงานในหน้านี้</p></div>
          </CardContent></Card>}
          <ApplicationHistoryTable applications={applications} />
        </div>}
    </div></section>
  </main>;
}
