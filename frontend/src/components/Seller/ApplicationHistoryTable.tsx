"use client";

import { useState } from "react";
import { ChevronLeft, ChevronRight, Eye } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import type { SellerApplicationRecord, SellerApplicationStatus } from "@/types/sellerApplication";

export const statusLabels: Record<SellerApplicationStatus, string> = {
  PENDING: "Under review", APPROVED: "Approved", REJECTED: "Rejected", NEED_MORE_DOC: "Action needed",
};

export const statusDescriptions: Record<SellerApplicationStatus, string> = {
  PENDING: "Your application is under review. You do not need to apply again. This page checks for status updates automatically.",
  APPROVED: "Your application has been approved. Manage your shop from the Seller dashboard.",
  REJECTED: "Your application was not approved. Read the feedback below, then update your details or documents before applying again.",
  NEED_MORE_DOC: "The review team needs more information or documents. Check the requested changes below, then submit your updated application.",
};

export function getReviewNote(application: SellerApplicationRecord): string {
  if (application.adminNote?.trim()) return application.adminNote;
  if (!["REJECTED", "NEED_MORE_DOC"].includes(application.status)) return "No additional review notes.";
  return application.status === "REJECTED"
    ? "No rejection reason was provided. Check your shop details and documents before applying again."
    : "No specific document changes were provided. Check that your details are complete and document images are readable before resubmitting.";
}

export const statusColors: Record<SellerApplicationStatus, string> = {
  PENDING: "bg-yellow-light-4 text-yellow-dark", APPROVED: "bg-green-light-6 text-green",
  REJECTED: "bg-red-light-6 text-red", NEED_MORE_DOC: "bg-blue/10 text-blue",
};

const PAGE_SIZE = 10;

export default function ApplicationHistoryTable({ applications, emptyMessage, onView, embedded = false, getViewLabel, viewDisabled = false }: {
  applications: SellerApplicationRecord[];
  emptyMessage?: string;
  onView?: (application: SellerApplicationRecord) => void;
  embedded?: boolean;
  getViewLabel?: (application: SellerApplicationRecord) => string;
  viewDisabled?: boolean;
}) {
  const [page, setPage] = useState(1);
  const totalPages = Math.max(1, Math.ceil(applications.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages);
  const start = (currentPage - 1) * PAGE_SIZE;
  const visibleApplications = applications.slice(start, start + PAGE_SIZE);
  const visiblePages = Array.from(new Set([1, currentPage - 1, currentPage, currentPage + 1, totalPages]))
    .filter((value) => value >= 1 && value <= totalPages)
    .sort((a, b) => a - b);

  const content = <>
      <div className="overflow-x-auto">
        <table className={`w-full text-left text-sm ${embedded ? "min-w-[960px]" : onView ? "min-w-[780px]" : "min-w-[660px]"}`}>
          <thead className="bg-gray-1 text-dark-4"><tr>
            <th className="px-6 py-4">Application</th>
            <th className="px-6 py-4">Shop</th>
            <th className="px-6 py-4">Submitted</th>
            <th className="px-6 py-4">Status</th>
            <th className="px-6 py-4">Review note</th>
            {onView && <th className="px-6 py-4 text-right">Details</th>}
          </tr></thead>
          <tbody className="divide-y divide-gray-3">
            {visibleApplications.map((item) => <tr key={item.applicationId}>
              <td className="px-6 py-5 font-medium text-dark">#{item.applicationId}</td>
              <td className="px-6 py-5">{item.shopName}</td>
              <td className="px-6 py-5">{new Date(item.createdAt).toLocaleDateString("en-US")}</td>
              <td className="px-6 py-5"><span className={`rounded-full px-3 py-1 text-xs ${embedded ? "inline-flex whitespace-nowrap" : ""} ${statusColors[item.status]}`}>{statusLabels[item.status]}</span></td>
              <td className="max-w-[250px] break-words px-6 py-5">{item.adminNote || (["REJECTED", "NEED_MORE_DOC"].includes(item.status) ? getReviewNote(item) : "—")}</td>
              {onView && <td className="px-6 py-4 text-right"><Button type="button" disabled={viewDisabled} onClick={() => onView(item)} aria-label={getViewLabel ? `${getViewLabel(item)} ${item.applicationId}` : `View application ${item.applicationId}`} className="h-9 gap-2 rounded-lg bg-blue px-4 text-sm font-medium text-white shadow-sm hover:bg-blue-dark focus-visible:ring-blue/30"><Eye className="size-4" /> {getViewLabel ? getViewLabel(item) : "View application"}</Button></td>}
            </tr>)}
            {applications.length === 0 && emptyMessage && <tr><td colSpan={onView ? 6 : 5} className="px-6 py-10 text-center text-dark-4">{emptyMessage}</td></tr>}
          </tbody>
        </table>
      </div>
      {applications.length > 0 && <div className="flex flex-col gap-3 border-t border-gray-3 px-6 py-4 text-sm text-dark-4 sm:flex-row sm:items-center sm:justify-between">
        <p>Showing {start + 1}–{Math.min(start + PAGE_SIZE, applications.length)} of {applications.length}</p>
        {totalPages > 1 && <nav aria-label="Application history pages" className="flex flex-wrap items-center gap-1.5">
          <Button type="button" variant="outline" disabled={currentPage === 1} onClick={() => setPage(currentPage - 1)} aria-label="Previous page" className="h-9 rounded-lg border-gray-3 px-3 text-dark hover:bg-gray-1"><ChevronLeft className="size-4" /> Previous</Button>
          {visiblePages.map((value, index) => <span key={value} className="flex items-center gap-1.5">
            {index > 0 && value - visiblePages[index - 1] > 1 && <span aria-hidden="true" className="px-1 text-dark-4">…</span>}
            <Button type="button" variant={value === currentPage ? "default" : "outline"} aria-label={`Page ${value}`} aria-current={value === currentPage ? "page" : undefined} onClick={() => setPage(value)} className={`size-9 rounded-lg p-0 ${value === currentPage ? "bg-blue text-white hover:bg-blue-dark" : "border-gray-3 text-dark hover:bg-gray-1"}`}>{value}</Button>
          </span>)}
          <Button type="button" variant="outline" disabled={currentPage === totalPages} onClick={() => setPage(currentPage + 1)} aria-label="Next page" className="h-9 rounded-lg border-gray-3 px-3 text-dark hover:bg-gray-1">Next <ChevronRight className="size-4" /></Button>
        </nav>}
      </div>}
  </>;

  if (embedded) return content;

  return <Card>
    <CardHeader><CardTitle>Application history</CardTitle></CardHeader>
    <CardContent className="p-0">{content}</CardContent>
  </Card>;
}
