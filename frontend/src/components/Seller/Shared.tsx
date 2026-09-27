"use client";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/app/context/AuthContext";
import { Card, CardContent } from "@/components/ui/card";
import { FlaskConical, ClipboardList, FilePlus2, Store } from "lucide-react";
import type { ApplicationStatus, SellerOrderStatus } from "@/app/context/SellerPreviewContext";

const orderLabels: Record<SellerOrderStatus, string> = {
  PENDING_PAYMENT: "Pending payment",
  WAITING_SELLER_CONFIRM: "Awaiting acceptance",
  PREPARING: "Preparing",
  SHIPPED: "Shipped",
  COMPLETED: "Completed",
  CANCELLED: "Cancelled",
};
const applicationLabels: Record<ApplicationStatus, string> = {
  PENDING: "Under review",
  APPROVED: "Approved",
  REJECTED: "Rejected",
  NEED_MORE_DOC: "More documents needed",
};

export function DemoNotice({ children }: { children?: React.ReactNode }) {
  return <div role="note" className="mb-7 flex gap-3 rounded-xl border border-blue/20 bg-blue/5 p-4 text-sm text-dark"><FlaskConical className="mt-0.5 size-5 shrink-0 text-blue" aria-hidden="true" /><p><strong>Demo preview.</strong> {children || "These records are examples kept only while this page stays open. No seller action is sent to the backend."}</p></div>;
}

export function StatusBadge({ status }: { status: SellerOrderStatus | ApplicationStatus }) {
  const label = status in orderLabels ? orderLabels[status as SellerOrderStatus] : applicationLabels[status as ApplicationStatus];
  const color = status === "APPROVED" || status === "COMPLETED" ? "bg-green-light-6 text-green" : status === "REJECTED" || status === "CANCELLED" ? "bg-red-light-6 text-red" : status === "PENDING" || status === "WAITING_SELLER_CONFIRM" || status === "NEED_MORE_DOC" ? "bg-yellow-light-4 text-yellow-dark" : "bg-blue/10 text-blue";
  return <span className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-medium ${color}`}>{label}</span>;
}

export function SellerNav() {
  const { user } = useAuth();
  const pathname = usePathname();
  const links = [
    { href: "/seller-application", label: "Application dashboard", icon: ClipboardList },
    ...(user?.role === "CUSTOMER" ? [{ href: "/seller-application/apply", label: "Submit application", icon: FilePlus2 }] : []),
    ...(user?.role === "SELLER" && user.status === "ACTIVE" ? [{ href: "/seller-dashboard", label: "Shop dashboard", icon: Store }] : []),
  ];
  return <aside><Card><CardContent className="p-3"><nav aria-label="Seller pages" className="space-y-2">{links.map(({href, label, icon: Icon}) => <Link key={href} href={href} aria-current={pathname === href ? "page" : undefined} className={`flex min-h-[44px] items-center gap-3 rounded-lg px-3 py-3 text-sm font-medium ${pathname === href ? "bg-blue text-white" : "text-dark hover:bg-gray-1"}`}><Icon className="size-4 shrink-0" />{label}</Link>)}</nav></CardContent></Card></aside>;
}
