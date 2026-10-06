"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { Check, ExternalLink, FileText, RefreshCw, X } from "lucide-react";
import AdminShell from "./AdminShell";
import { adminRequest } from "./api";
import type { SellerApplicationRecord, SellerApplicationStatus } from "@/types/sellerApplication";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import ApplicationHistoryTable, { statusColors, statusLabels } from "@/components/Seller/ApplicationHistoryTable";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";

type Decision = "approve" | "reject" | "request-docs";

function documentUrl(value: string | null | undefined) {
  try {
    const url = new URL(value || "");
    return ["http:", "https:"].includes(url.protocol) ? url.href : null;
  } catch {
    return null;
  }
}

function Detail({ label, value }: { label: string; value?: string | null }) {
  return <div className="min-w-0">
    <dt className="text-xs font-medium text-dark-4">{label}</dt>
    <dd className="mt-1 break-words text-sm font-medium leading-6 text-dark">{value || "—"}</dd>
  </div>;
}

function InformationSection({ title, description, children }: {
  title: string;
  description: string;
  children: React.ReactNode;
}) {
  return <section className="grid gap-4 border-t border-gray-3 py-6 sm:py-7 md:grid-cols-[150px_minmax(0,1fr)] md:gap-7">
    <div>
      <h3 className="text-sm font-semibold text-dark">{title}</h3>
      <p className="mt-1 text-xs leading-5 text-dark-4">{description}</p>
    </div>
    <div className="min-w-0">{children}</div>
  </section>;
}

function ApplicationFields({ application }: { application: SellerApplicationRecord }) {
  return <>
    <InformationSection title="Shop details" description="Store and contact information">
      <dl className="grid gap-x-6 gap-y-5 sm:grid-cols-2">
        <Detail label="Description" value={application.shopDescription} />
        <Detail label="Address" value={application.shopAddress} />
        <Detail label="Email" value={application.shopEmail} />
        <Detail label="Phone" value={application.shopPhone} />
      </dl>
    </InformationSection>
    <InformationSection title="Seller identity" description="Applicant's legal details">
      <dl className="grid gap-x-6 gap-y-5 sm:grid-cols-2">
        <Detail label="Name" value={application.sellerFirstName + " " + application.sellerLastName} />
        <Detail label="ID card number" value={application.idCardNumber} />
      </dl>
    </InformationSection>
    <InformationSection title="Payment and documents" description="Payout details and uploaded files">
      <dl className="grid gap-x-6 gap-y-5 sm:grid-cols-2">
        <Detail label="Bank" value={application.bankName} />
        <Detail label="Account name" value={application.bankAccountName} />
        <Detail label="Account number" value={application.bankAccountNumber} />
      </dl>
      <div className="mt-6 flex flex-wrap gap-2 border-t border-gray-3 pt-5">
        {([["ID card", application.idCardImageUrl], ["Bank book", application.bankBookImageUrl]] as const).map(([label, url]) => {
          const safeUrl = documentUrl(url);
          return safeUrl
            ? <a key={label} href={safeUrl} target="_blank" rel="noopener noreferrer" className="inline-flex h-8 items-center justify-center gap-1.5 rounded-lg border border-gray-3 bg-white px-2.5 text-sm font-medium text-dark transition-colors hover:bg-gray-1 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue">
                <FileText className="size-4 text-blue" /> {label} <ExternalLink className="size-3 text-dark-4" />
              </a>
            : <span key={label} className="text-sm text-dark-4">{label} unavailable</span>;
        })}
      </div>
    </InformationSection>
  </>;
}

export default function AdminApplications() {
  const dialogTitleRef = useRef<HTMLHeadingElement>(null);
  const dialogContentRef = useRef<HTMLDivElement>(null);
  const returnFocusRef = useRef<HTMLElement | null>(null);
  const filterRef = useRef<HTMLSelectElement>(null);
  const submitLock = useRef(false);
  const [applications, setApplications] = useState<SellerApplicationRecord[]>([]);
  const [viewing, setViewing] = useState<SellerApplicationRecord | null>(null);
  const [statusFilter, setStatusFilter] = useState<SellerApplicationStatus | "ALL">("ALL");
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [decisionError, setDecisionError] = useState("");
  const [notice, setNotice] = useState("");
  const [decision, setDecision] = useState<Decision | null>(null);
  const [message, setMessage] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const pendingCount = applications.filter((item) => item.status === "PENDING").length;
  const filteredApplications = statusFilter === "ALL"
    ? applications
    : applications.filter((item) => item.status === statusFilter);

  const reload = useCallback(async () => {
    setLoading(true);
    setLoadError("");
    try {
      const data = await adminRequest<SellerApplicationRecord[]>("applications/history");
      setApplications([...data].sort((a, b) =>
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime() || b.applicationId - a.applicationId
      ));
    } catch (cause) {
      setLoadError(cause instanceof Error ? cause.message : "Could not load applications");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void reload(); }, [reload]);

  useEffect(() => {
    if (!decision) return;
    const frame = window.requestAnimationFrame(() => {
      const content = dialogContentRef.current;
      content?.scrollTo({ top: content.scrollHeight, behavior: "smooth" });
    });
    return () => window.cancelAnimationFrame(frame);
  }, [decision]);

  function openApplication(application: SellerApplicationRecord) {
    returnFocusRef.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    setViewing(application);
    setDecision(null);
    setMessage("");
    setDecisionError("");
  }

  function closeApplication() {
    if (submitLock.current) return;
    setViewing(null);
    setDecision(null);
    setMessage("");
    setDecisionError("");
  }

  function chooseDecision(nextDecision: Decision) {
    setDecision(nextDecision);
    setMessage("");
    setDecisionError("");
  }

  async function submitDecision() {
    if (!viewing || viewing.status !== "PENDING" || !decision || submitLock.current) return;
    const note = message.trim();
    if (decision !== "approve" && (!note || note.length > 1000)) {
      setDecisionError("Enter a reason or document request of up to 1,000 characters.");
      return;
    }

    submitLock.current = true;
    setSubmitting(true);
    setDecisionError("");
    try {
      await adminRequest<void>("applications/" + viewing.applicationId + "/" + decision, {
        method: "PUT",
        ...(decision === "approve" ? {} : {
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(decision === "reject" ? { reason: note } : { message: note }),
        }),
      });
      const nextStatus: SellerApplicationStatus = decision === "approve" ? "APPROVED" : decision === "reject" ? "REJECTED" : "NEED_MORE_DOC";
      // Keep a completed review out of the pending list even if the refresh fails.
      setApplications((current) => current.map((item) => item.applicationId === viewing.applicationId
        ? { ...item, status: nextStatus, adminNote: decision === "approve" ? item.adminNote : note }
        : item));
      setNotice(viewing.shopName + ": " + (decision === "approve" ? "approved" : decision === "reject" ? "rejected" : "documents requested") + ".");
      setViewing(null);
      setDecision(null);
      setMessage("");
      await reload();
    } catch (cause) {
      setDecisionError(cause instanceof Error ? cause.message : "Could not update application. Please try again.");
    } finally {
      submitLock.current = false;
      setSubmitting(false);
    }
  }

  return <AdminShell title="Seller applications">
    <section aria-labelledby="applications-heading" className="overflow-hidden rounded-2xl border border-gray-3 bg-white shadow-1">
      <header className="flex flex-col justify-between gap-5 px-5 py-6 sm:px-7 lg:flex-row lg:items-center">
        <div>
          <div className="flex flex-wrap items-center gap-3">
            <h2 id="applications-heading" className="text-xl font-semibold tracking-tight text-dark">All applications</h2>
            <span className="rounded-full bg-blue/10 px-3 py-1 text-xs font-semibold text-blue" aria-live="polite">
              {loading ? "Loading…" : pendingCount + " awaiting review"}
            </span>
          </div>
          <p className="mt-2 text-sm text-dark-4">Review seller details, documents, and application status.</p>
        </div>
        <div className="flex flex-wrap items-end gap-3">
          <div className="min-w-0 flex-1 sm:flex-none">
            <label htmlFor="application-status-filter" className="mb-1.5 block text-xs font-medium text-dark-4">Filter by status</label>
            <select
              ref={filterRef}
              id="application-status-filter"
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value as SellerApplicationStatus | "ALL")}
              className="h-10 w-full rounded-lg border border-gray-3 bg-white px-3 text-sm text-dark focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue sm:min-w-[185px]"
            >
              <option value="ALL">All statuses</option>
              <option value="PENDING">Under review</option>
              <option value="NEED_MORE_DOC">Action needed</option>
              <option value="APPROVED">Approved</option>
              <option value="REJECTED">Rejected</option>
            </select>
          </div>
          <Button type="button" variant="outline" onClick={() => void reload()} disabled={loading || submitting} className="h-10 border-gray-3 px-3 text-dark hover:bg-gray-1">
            <RefreshCw className="size-4" /> Refresh
          </Button>
        </div>
      </header>

      {(notice || loadError) && <div className="space-y-2 border-t border-gray-3 px-5 py-4 sm:px-7">
        {notice && <p role="status" className="rounded-lg border border-blue/20 bg-blue/5 px-4 py-3 text-sm text-dark">{notice}</p>}
        {loadError && <div role="alert" className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-red/20 bg-red-light-6 px-4 py-3 text-sm text-red">
          <p>Could not refresh applications: {loadError}</p>
          <Button type="button" variant="outline" onClick={() => void reload()} disabled={loading || submitting} className="border-red/20 bg-white text-red">Try again</Button>
        </div>}
      </div>}

      <div className="border-t border-gray-3" aria-busy={loading}>
        {loading && applications.length === 0
          ? <p role="status" className="px-6 py-16 text-center text-sm text-dark-4">Loading applications…</p>
          : !(loadError && applications.length === 0) && <ApplicationHistoryTable
              key={statusFilter}
              applications={filteredApplications}
              emptyMessage={statusFilter === "ALL" ? "No applications yet." : "No applications with this status."}
              onView={openApplication}
              getViewLabel={(application) => application.status === "PENDING" ? "Review application" : "View application"}
              viewDisabled={loading || submitting}
              embedded
            />}
      </div>
    </section>

    <Dialog open={viewing !== null} onOpenChange={(open) => { if (!open) closeApplication(); }}>
      {viewing && <DialogContent
        ref={dialogContentRef}
        initialFocus={dialogTitleRef}
        finalFocus={() => returnFocusRef.current?.isConnected && !returnFocusRef.current.matches(":disabled") ? returnFocusRef.current : filterRef.current}
        showCloseButton={!submitting}
        className="sm:max-w-[800px]"
        aria-busy={submitting}
      >
        <DialogHeader className="pb-5">
          <DialogTitle ref={dialogTitleRef} tabIndex={-1} className="break-words text-xl font-semibold text-dark">{viewing.shopName}</DialogTitle>
          <DialogDescription className="text-sm text-dark-4">Application #{viewing.applicationId} · Submitted {new Date(viewing.createdAt).toLocaleString("en-US")}</DialogDescription>
          <div><span className={"inline-flex rounded-full px-3 py-1 text-xs font-medium " + statusColors[viewing.status]}>{statusLabels[viewing.status]}</span></div>
        </DialogHeader>
        {viewing.adminNote && <div className="mb-5 rounded-lg border border-gray-3 bg-gray-1 p-4"><p className="text-xs font-semibold text-dark">Review note</p><p className="mt-1 whitespace-pre-wrap break-words text-sm text-dark">{viewing.adminNote}</p></div>}
        <ApplicationFields application={viewing} />

        {viewing.status === "PENDING" && <section aria-labelledby="review-decision-heading" className="border-t border-gray-3 pt-5">
          <h3 id="review-decision-heading" className="text-sm font-semibold text-dark">Review decision</h3>
          <p className="mt-1 text-sm text-dark-4">Choose an action, then confirm it below.</p>
          <div className="mt-4 grid grid-cols-1 gap-2 sm:grid-cols-3">
            <Button type="button" disabled={submitting} aria-pressed={decision === "approve"} onClick={() => chooseDecision("approve")} className="h-10 w-full justify-center bg-blue px-4 text-white hover:bg-blue-dark"><Check className="size-4" /> Approve</Button>
            <Button type="button" variant="outline" disabled={submitting} aria-pressed={decision === "request-docs"} onClick={() => chooseDecision("request-docs")} className="h-10 w-full justify-center border-gray-3 px-4 text-dark"><FileText className="size-4" /> Request documents</Button>
            <Button type="button" variant="outline" disabled={submitting} aria-pressed={decision === "reject"} onClick={() => chooseDecision("reject")} className="h-10 w-full justify-center border-red/20 px-4 text-red hover:bg-red-light-6"><X className="size-4" /> Reject</Button>
          </div>
          {decision && <div className="mt-5 rounded-lg border border-gray-3 bg-gray-1 p-4">
            <p className="text-sm font-semibold text-dark">{decision === "approve" ? "Approve " + viewing.shopName + "?" : decision === "reject" ? "Reason for rejection" : "Documents needed"}</p>
            {decision !== "approve" && <Textarea
              value={message}
              disabled={submitting}
              onChange={(event) => setMessage(event.target.value)}
              maxLength={1000}
              placeholder={decision === "reject" ? "Explain why this application was rejected" : "Tell the seller which documents to provide"}
              className="mt-3 min-h-24 border-gray-3 bg-white"
              aria-label={decision === "reject" ? "Rejection reason" : "Document request"}
              aria-describedby={decisionError ? "review-decision-error" : undefined}
            />}
            {decisionError && <p id="review-decision-error" role="alert" className="mt-3 text-sm text-red">{decisionError}</p>}
            <div className="mt-4 flex flex-wrap gap-2">
              <Button type="button" onClick={() => void submitDecision()} disabled={submitting || (decision !== "approve" && !message.trim())} className="h-10 bg-blue px-4 text-white hover:bg-blue-dark">{submitting ? "Saving…" : "Confirm decision"}</Button>
              <Button type="button" variant="outline" onClick={() => { setDecision(null); setMessage(""); setDecisionError(""); }} disabled={submitting} className="h-10 border-gray-3 px-4 text-dark">Cancel</Button>
            </div>
          </div>}
        </section>}
      </DialogContent>}
    </Dialog>
  </AdminShell>;
}
