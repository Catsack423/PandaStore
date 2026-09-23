"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ClipboardList, Store } from "lucide-react";
import Breadcrumb from "@/components/Common/Breadcrumb";
import { useAuth } from "@/app/context/AuthContext";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { SellerApplicationRecord, SellerApplicationStatus } from "@/types/sellerApplication";

const statusLabels: Record<SellerApplicationStatus, string> = {
  PENDING: "Under review", APPROVED: "Approved", REJECTED: "Rejected", NEED_MORE_DOC: "Action needed",
};
const statusColors: Record<SellerApplicationStatus, string> = {
  PENDING: "bg-yellow-light-4 text-yellow-dark", APPROVED: "bg-green-light-6 text-green",
  REJECTED: "bg-red-light-6 text-red", NEED_MORE_DOC: "bg-blue/10 text-blue",
};

export default function SellerApplication() {
  const { user, isLoading, refreshUser } = useAuth();
  const [applications, setApplications] = useState<SellerApplicationRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reload, setReload] = useState(0);

  useEffect(() => {
    if (!user) { setLoading(false); return; }
    let active = true;
    setLoading(true);
    fetch("/api/seller-applications", { cache: "no-store" })
      .then(async (response) => {
        const body = await response.json();
        if (!response.ok || !body.success || !Array.isArray(body.data)) throw new Error(body.message || "Could not load applications");
        if (active) {
          setApplications(body.data);
          setError("");
          if (body.data[0]?.status === "APPROVED" && user.role === "CUSTOMER") void refreshUser();
        }
      })
      .catch((reason) => { if (active) setError(reason instanceof Error ? reason.message : "Could not load applications"); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [user?.id, reload]);

  const latest = applications[0];
  const canApply = !latest || latest.status === "REJECTED" || latest.status === "NEED_MORE_DOC";
  const applyLabel = latest?.status === "NEED_MORE_DOC" ? "Update application" : latest?.status === "REJECTED" ? "Revise and reapply" : "Apply to sell";

  return <main>
    <Breadcrumb title="Seller Applications" pages={["Seller Applications"]} />
    <section className="bg-gray-2 py-12 sm:py-16"><div className="mx-auto w-full max-w-[1170px] px-4 sm:px-8 xl:px-0">
      <div className="mb-7 flex flex-wrap items-end justify-between gap-4"><div><p className="text-sm font-medium text-blue">Sell on PandaStore</p><h1 className="mt-1 text-3xl font-semibold text-dark">Seller applications</h1><p className="mt-2 text-sm text-dark-4">Apply with your existing customer account and follow each review decision here.</p></div>
        {user && !loading && !error && canApply && <Link href="/seller-application/apply" className="inline-flex h-11 items-center rounded-lg bg-blue px-5 text-sm font-medium text-white hover:bg-blue-dark">{applyLabel}</Link>}
      </div>
      {isLoading ? <Card><CardContent className="py-16 text-center">Checking your account...</CardContent></Card>
        : !user ? <Card><CardContent className="flex flex-col items-center px-6 py-14 text-center"><Store className="mb-4 size-10 text-blue" /><h2 className="text-xl font-semibold text-dark">Start with a customer account</h2><p className="mt-2 max-w-md text-sm">Sign in or create a customer account before applying to open a shop.</p><div className="mt-6 flex gap-3"><Link href="/signin?callbackUrl=%2Fseller-application" className="rounded-lg bg-blue px-5 py-3 text-sm font-medium text-white">Sign in</Link><Link href="/signup" className="rounded-lg border border-gray-3 px-5 py-3 text-sm font-medium text-dark">Create account</Link></div></CardContent></Card>
        : loading ? <Card><CardContent className="py-16 text-center">Loading applications...</CardContent></Card>
        : error ? <Card><CardContent className="py-12 text-center"><p role="alert" className="text-red">{error}</p><Button type="button" variant="outline" onClick={() => setReload((value) => value + 1)} className="mt-4">Try again</Button></CardContent></Card>
        : <div className="space-y-6">
          {latest && <Card><CardHeader><CardTitle>Latest application · #{latest.applicationId}</CardTitle></CardHeader><CardContent className="space-y-4"><div className="flex flex-wrap items-center gap-3"><h2 className="text-xl font-semibold text-dark">{latest.shopName}</h2><span className={`rounded-full px-3 py-1 text-xs font-medium ${statusColors[latest.status]}`}>{statusLabels[latest.status]}</span></div>
            <p className="text-sm">{latest.status === "PENDING" ? "Your application is being reviewed." : latest.status === "APPROVED" ? "Your shop has been approved." : latest.status === "NEED_MORE_DOC" ? "The review team needs updated documents. Use the form to submit a revised application." : "Review the decision and submit a corrected application."}</p>
            {latest.adminNote && <div className="rounded-lg border border-yellow/30 bg-yellow-light-4 p-4 text-sm"><strong className="text-dark">Review note</strong><p className="mt-1">{latest.adminNote}</p></div>}
            {canApply && <Link href="/seller-application/apply" className="inline-flex h-10 items-center rounded-lg bg-blue px-5 text-sm font-medium text-white hover:bg-blue-dark">{applyLabel}</Link>}
            {latest.status === "APPROVED" && <Link href="/seller-dashboard" className="inline-flex h-10 items-center rounded-lg bg-blue px-5 text-sm font-medium text-white hover:bg-blue-dark">Open dashboard</Link>}
          </CardContent></Card>}
          {!latest && <Card><CardContent className="flex flex-col items-center py-14 text-center"><ClipboardList className="mb-4 size-10 text-blue" /><h2 className="text-xl font-semibold text-dark">No applications yet</h2><p className="mt-2 text-sm">Your customer account is ready. Complete the shop application to get started.</p><Link href="/seller-application/apply" className="mt-6 rounded-lg bg-blue px-5 py-3 text-sm font-medium text-white">Apply to sell</Link></CardContent></Card>}
          <Card><CardHeader><CardTitle>Application history</CardTitle></CardHeader><CardContent className="p-0"><div className="overflow-x-auto"><table className="w-full min-w-[660px] text-left text-sm"><thead className="bg-gray-1 text-dark-4"><tr><th className="px-6 py-4">Application</th><th className="px-6 py-4">Shop</th><th className="px-6 py-4">Submitted</th><th className="px-6 py-4">Status</th><th className="px-6 py-4">Review note</th></tr></thead><tbody className="divide-y divide-gray-3">{applications.map((item) => <tr key={item.applicationId}><td className="px-6 py-5 font-medium text-dark">#{item.applicationId}</td><td className="px-6 py-5">{item.shopName}</td><td className="px-6 py-5">{new Date(item.createdAt).toLocaleDateString("en-US")}</td><td className="px-6 py-5"><span className={`rounded-full px-3 py-1 text-xs ${statusColors[item.status]}`}>{statusLabels[item.status]}</span></td><td className="max-w-[250px] px-6 py-5">{item.adminNote || "—"}</td></tr>)}</tbody></table></div></CardContent></Card>
        </div>}
    </div></section>
  </main>;
}
