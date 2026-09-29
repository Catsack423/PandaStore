"use client";

import { useCallback, useEffect, useState } from "react";
import { Check, Clock3, ExternalLink, FileText, RefreshCw, Store, X } from "lucide-react";
import AdminShell from "./AdminShell";
import { adminRequest } from "./api";
import type { SellerApplicationRecord } from "@/types/sellerApplication";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Textarea } from "@/components/ui/textarea";
import ApplicationHistoryTable from "@/components/Seller/ApplicationHistoryTable";
import { statusColors, statusLabels } from "@/components/Seller/ApplicationHistoryTable";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";

type Decision = "approve" | "reject" | "request-docs";

function documentUrl(value: string | null | undefined) {
  try {
    const url = new URL(value || "");
    return ["http:", "https:"].includes(url.protocol) ? url.href : null;
  } catch { return null; }
}

function Detail({ label, value }: { label: string; value?: string | null }) {
  return <div className="min-w-0"><dt className="text-xs font-medium uppercase tracking-wide text-dark-4">{label}</dt><dd className="mt-1 break-words text-sm text-dark">{value || "—"}</dd></div>;
}

function ApplicationFields({ application }: { application: SellerApplicationRecord }) {
  return <>
    <section><h3 className="mb-4 text-sm font-semibold text-dark">Shop details</h3><dl className="grid gap-4 sm:grid-cols-2"><Detail label="Description" value={application.shopDescription} /><Detail label="Address" value={application.shopAddress} /><Detail label="Email" value={application.shopEmail} /><Detail label="Phone" value={application.shopPhone} /></dl></section>
    <section className="border-t border-gray-3 pt-6"><h3 className="mb-4 text-sm font-semibold text-dark">Seller identity</h3><dl className="grid gap-4 sm:grid-cols-2"><Detail label="Name" value={`${application.sellerFirstName} ${application.sellerLastName}`} /><Detail label="ID card number" value={application.idCardNumber} /></dl></section>
    <section className="border-t border-gray-3 pt-6"><h3 className="mb-4 text-sm font-semibold text-dark">Payment and documents</h3><dl className="grid gap-4 sm:grid-cols-2"><Detail label="Bank" value={application.bankName} /><Detail label="Account name" value={application.bankAccountName} /><Detail label="Account number" value={application.bankAccountNumber} /></dl><div className="mt-5 flex flex-wrap gap-2">{[["ID card", application.idCardImageUrl], ["Bank book", application.bankBookImageUrl]].map(([label, url]) => { const safeUrl = documentUrl(url); return safeUrl ? <Button key={label} variant="outline" render={<a href={safeUrl} target="_blank" rel="noopener noreferrer" />} className="border-gray-3 text-dark"><FileText className="size-4" /> {label} <ExternalLink className="size-3" /></Button> : <span key={label} className="text-sm text-dark-4">{label} unavailable</span>; })}</div></section>
  </>;
}

export default function AdminApplications() {
  const [applications, setApplications] = useState<SellerApplicationRecord[]>([]);
  const [history, setHistory] = useState<SellerApplicationRecord[]>([]);
  const [viewing, setViewing] = useState<SellerApplicationRecord | null>(null);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [historyError, setHistoryError] = useState("");
  const [historyLoading, setHistoryLoading] = useState(true);
  const [notice, setNotice] = useState("");
  const [decision, setDecision] = useState<Decision | null>(null);
  const [message, setMessage] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const selected = applications.find((item) => item.applicationId === selectedId) ?? null;

  const reload = useCallback(async () => {
    setLoading(true);
    setHistoryLoading(true);
    setError("");
    setHistoryError("");
    const [pendingResult, historyResult] = await Promise.allSettled([
      adminRequest<SellerApplicationRecord[]>("applications"),
      adminRequest<SellerApplicationRecord[]>("applications/history"),
    ]);
    if (pendingResult.status === "fulfilled") {
      const data = pendingResult.value;
      setApplications(data);
      setSelectedId((current) => data.some((item) => item.applicationId === current) ? current : data[0]?.applicationId ?? null);
    } else setError(pendingResult.reason instanceof Error ? pendingResult.reason.message : "Could not load applications");
    if (historyResult.status === "fulfilled") setHistory(historyResult.value);
    else setHistoryError(historyResult.reason instanceof Error ? historyResult.reason.message : "Could not load application history");
    setLoading(false);
    setHistoryLoading(false);
  }, []);

  useEffect(() => { void reload(); }, [reload]);

  async function submitDecision() {
    if (!selected || !decision || submitting) return;
    const note = message.trim();
    if (decision !== "approve" && !note) { setError("Enter a reason or document request before continuing."); return; }
    setSubmitting(true);
    setError("");
    try {
      await adminRequest<void>(`applications/${selected.applicationId}/${decision}`, {
        method: "PUT",
        ...(decision === "approve" ? {} : {
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(decision === "reject" ? { reason: note } : { message: note }),
        }),
      });
      setNotice(`${selected.shopName}: ${decision === "approve" ? "approved" : decision === "reject" ? "rejected" : "documents requested"}.`);
      setDecision(null);
      setMessage("");
      await reload();
    } catch (cause) { setError(cause instanceof Error ? cause.message : "Could not update application"); }
    finally { setSubmitting(false); }
  }

  return <AdminShell title="Seller applications">
    <div className="space-y-5">
      <Card className="border-gray-3 bg-white shadow-1"><CardContent className="flex flex-wrap items-center justify-between gap-3 p-5 sm:p-6">
        <div><p className="text-xs font-semibold uppercase tracking-[0.16em] text-blue">Review queue</p><p className="mt-1 text-xl font-semibold text-dark">{loading ? "Loading…" : `${applications.length} pending ${applications.length === 1 ? "application" : "applications"}`}</p></div>
        <Button type="button" variant="outline" onClick={() => void reload()} disabled={loading || submitting} className="border-gray-3 text-dark hover:bg-gray-1"><RefreshCw className="size-4" /> Refresh</Button>
      </CardContent></Card>
      {notice && <p role="status" className="rounded-lg border border-blue/20 bg-blue/5 px-4 py-3 text-sm text-dark">{notice}</p>}
      {error && <p role="alert" className="rounded-lg border border-red/20 bg-red-light-6 px-4 py-3 text-sm text-red">{error}</p>}
      {!loading && applications.length === 0 && <Card className="border-gray-3 bg-white shadow-1"><CardContent className="py-14 text-center"><Check className="mx-auto size-8 text-blue" /><h2 className="mt-4 text-lg font-semibold text-dark">All caught up</h2><p className="mt-1 text-sm text-dark-4">New seller applications will appear here.</p></CardContent></Card>}
      {applications.length > 0 && <div className="grid items-start gap-5 xl:grid-cols-[270px_minmax(0,1fr)]">
        <Card className="min-w-0 border-gray-3 bg-white shadow-1"><CardHeader className="border-b border-gray-3 p-5"><CardTitle className="text-base text-dark">Awaiting review</CardTitle></CardHeader><CardContent className="p-2">
          <div className="max-h-[650px] space-y-1 overflow-y-auto">{applications.map((item) => <button key={item.applicationId} type="button" onClick={() => { setSelectedId(item.applicationId); setDecision(null); setMessage(""); setError(""); }} aria-pressed={item.applicationId === selectedId} className={`w-full rounded-lg border px-3 py-3 text-left transition-colors ${item.applicationId === selectedId ? "border-blue/30 bg-blue/5" : "border-transparent hover:bg-gray-1"}`}><span className="flex items-center gap-2 text-sm font-semibold text-dark"><Store className="size-4 shrink-0 text-blue" /> <span className="truncate">{item.shopName}</span></span><span className="mt-1 block pl-6 text-xs text-dark-4">{item.sellerFirstName} {item.sellerLastName}</span><span className="mt-1 flex items-center gap-1 pl-6 text-xs text-dark-4"><Clock3 className="size-3" /> {new Date(item.createdAt).toLocaleDateString()}</span></button>)}</div>
        </CardContent></Card>
        {selected && <Card className="min-w-0 border-gray-3 bg-white shadow-1"><CardHeader className="border-b border-gray-3 p-5 sm:p-6"><p className="text-xs font-semibold uppercase tracking-[0.16em] text-blue">Application #{selected.applicationId}</p><CardTitle className="mt-1 text-xl text-dark">{selected.shopName}</CardTitle><p className="text-sm text-dark-4">Submitted {new Date(selected.createdAt).toLocaleString()}</p></CardHeader><CardContent className="space-y-7 p-5 sm:p-6">
          <ApplicationFields application={selected} />
          <section className="border-t border-gray-3 pt-6"><h3 className="text-sm font-semibold text-dark">Decision</h3><p className="mt-1 text-sm text-dark-4">Choose an action, then confirm it below.</p><div className="mt-4 flex flex-wrap gap-2"><Button type="button" onClick={() => { setDecision("approve"); setError(""); }} className="bg-blue text-white hover:bg-blue-dark"><Check className="size-4" /> Approve</Button><Button type="button" variant="outline" onClick={() => { setDecision("request-docs"); setError(""); }} className="border-gray-3 text-dark"><FileText className="size-4" /> Request documents</Button><Button type="button" variant="destructive" onClick={() => { setDecision("reject"); setError(""); }}><X className="size-4" /> Reject</Button></div>
            {decision && <div className="mt-5 rounded-lg border border-gray-3 bg-gray-1 p-4"><p className="text-sm font-semibold text-dark">{decision === "approve" ? `Approve ${selected.shopName}?` : decision === "reject" ? "Reason for rejection" : "Documents needed"}</p>{decision !== "approve" && <Textarea value={message} onChange={(event) => setMessage(event.target.value)} maxLength={1000} placeholder={decision === "reject" ? "Explain why this application was rejected" : "Tell the seller which documents to provide"} className="mt-3 min-h-24 border-gray-3 bg-white" aria-label={decision === "reject" ? "Rejection reason" : "Document request"} />}<div className="mt-4 flex flex-wrap gap-2"><Button type="button" onClick={() => void submitDecision()} disabled={submitting || (decision !== "approve" && !message.trim())} className="bg-blue text-white hover:bg-blue-dark">{submitting ? "Saving…" : "Confirm decision"}</Button><Button type="button" variant="outline" onClick={() => { setDecision(null); setMessage(""); }} disabled={submitting} className="border-gray-3 text-dark">Cancel</Button></div></div>}
          </section>
        </CardContent></Card>}
      </div>}
      {historyError ? <Card><CardContent className="flex flex-wrap items-center justify-between gap-3 p-6"><p role="alert" className="text-sm text-red">Could not load application history: {historyError}</p><Button type="button" variant="outline" onClick={() => void reload()} disabled={loading || historyLoading}>Try again</Button></CardContent></Card>
        : <ApplicationHistoryTable applications={history} emptyMessage={historyLoading ? "Loading application history..." : "No applications yet."} onView={setViewing} />}
    </div>
    <Dialog open={viewing !== null} onOpenChange={(open) => { if (!open) setViewing(null); }}>
      {viewing && <DialogContent className="sm:max-w-[760px]">
        <DialogHeader className="border-b border-gray-3 pb-5">
          <DialogTitle className="text-xl font-semibold text-dark">{viewing.shopName}</DialogTitle>
          <DialogDescription className="text-sm text-dark-4">Application #{viewing.applicationId} · Submitted {new Date(viewing.createdAt).toLocaleString("en-US")}</DialogDescription>
        </DialogHeader>
        <div className="mt-6 space-y-7">
          <section><h3 className="mb-3 text-sm font-semibold text-dark">Review status</h3><span className={`inline-flex rounded-full px-3 py-1 text-xs font-medium ${statusColors[viewing.status]}`}>{statusLabels[viewing.status]}</span>{viewing.adminNote && <p className="mt-3 rounded-lg border border-gray-3 bg-gray-1 p-3 text-sm text-dark">{viewing.adminNote}</p>}</section>
          <ApplicationFields application={viewing} />
        </div>
      </DialogContent>}
    </Dialog>
  </AdminShell>;
}
