"use client";

import { useState, type ChangeEvent, type FormEvent } from "react";
import Link from "next/link";
import { ArrowRight, ClipboardList, FileText, FileUp, Store } from "lucide-react";
import Breadcrumb from "@/components/Common/Breadcrumb";
import { useSellerPreview, type ApplicationStatus, type SellerApplicationPreview, type ShopFields } from "@/app/context/SellerPreviewContext";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { DemoNotice, SellerNav, StatusBadge } from "./Shared";

const statusCopy: Record<ApplicationStatus, string> = {
  PENDING: "Your application is in review. We will show the result here when a decision is available.",
  APPROVED: "Your shop is approved. Open the dashboard to manage orders.",
  REJECTED: "Review the reason, update your details below, and submit a new application from this page.",
  NEED_MORE_DOC: "Add the requested documents and submit an updated application from this page.",
};

const formatDate = (date: string) => new Date(date).toLocaleDateString("en-US", {
  year: "numeric", month: "short", day: "numeric",
});
const scrollToRevision = () => document.getElementById("resubmit-application")?.scrollIntoView({ behavior: "smooth", block: "start" });

function ResubmissionForm({ application, onSubmitted }: { application: SellerApplicationPreview; onSubmitted: () => void }) {
  const { resubmitApplication } = useSellerPreview();
  const [shop, setShop] = useState<ShopFields>({
    shopName: application.shopName,
    shopDescription: application.shopDescription,
    shopPhone: application.shopPhone,
    shopEmail: application.shopEmail,
    shopAddress: application.shopAddress,
  });
  const [sellerNote, setSellerNote] = useState("");
  const [files, setFiles] = useState<File[]>([]);
  const [error, setError] = useState("");
  const needsDocuments = application.status === "NEED_MORE_DOC";
  const updateShop = (field: keyof ShopFields, value: string) => setShop((current) => ({ ...current, [field]: value }));

  function selectFiles(event: ChangeEvent<HTMLInputElement>) {
    const selected = Array.from(event.target.files ?? []);
    if (selected.length > 3 || selected.some((file) => file.size > 5 * 1024 * 1024)) {
      setError("Choose up to 3 files, each no larger than 5 MB.");
      setFiles([]);
      event.target.value = "";
      return;
    }
    setError("");
    setFiles(selected);
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (needsDocuments && files.length === 0) {
      setError("Choose at least one document requested by the review team.");
      return;
    }
    const ok = resubmitApplication({
      shopName: shop.shopName.trim(),
      shopDescription: shop.shopDescription.trim(),
      shopPhone: shop.shopPhone.trim(),
      shopEmail: shop.shopEmail.trim(),
      shopAddress: shop.shopAddress.trim(),
    }, sellerNote, files.map((file) => file.name));
    if (!ok) {
      setError("Add a note about your changes and try again.");
      return;
    }
    onSubmitted();
  }

  return <Card id="resubmit-application" className="scroll-mt-6 overflow-hidden">
    <CardHeader className="border-b border-gray-3 bg-white">
      <div className="flex items-center gap-3">
        <div className="flex size-10 items-center justify-center rounded-lg bg-blue/10 text-blue"><FileUp className="size-5" /></div>
        <div><CardTitle>{needsDocuments ? "Provide additional documents" : "Revise and resubmit"}</CardTitle><p className="mt-1 text-sm text-dark-4">Update application #{application.applicationId} here. No new account registration is needed.</p></div>
      </div>
    </CardHeader>
    <CardContent className="pt-6">
      <form onSubmit={submit} className="space-y-6">
        <div className="grid gap-5 sm:grid-cols-2">
          <div className="space-y-2"><Label htmlFor="revision-shop-name">Shop name</Label><Input id="revision-shop-name" required maxLength={100} value={shop.shopName} onChange={(event) => updateShop("shopName", event.target.value)} /></div>
          <div className="space-y-2"><Label htmlFor="revision-shop-phone">Shop phone</Label><Input id="revision-shop-phone" required type="tel" value={shop.shopPhone} onChange={(event) => updateShop("shopPhone", event.target.value)} /></div>
          <div className="space-y-2"><Label htmlFor="revision-shop-email">Shop email</Label><Input id="revision-shop-email" required type="email" value={shop.shopEmail} onChange={(event) => updateShop("shopEmail", event.target.value)} /></div>
          <div className="space-y-2"><Label htmlFor="revision-shop-address">Shop address</Label><Input id="revision-shop-address" required value={shop.shopAddress} onChange={(event) => updateShop("shopAddress", event.target.value)} /></div>
        </div>
        <div className="space-y-2"><Label htmlFor="revision-shop-description">Shop description</Label><Textarea id="revision-shop-description" value={shop.shopDescription} onChange={(event) => updateShop("shopDescription", event.target.value)} /></div>
        <div className="space-y-2"><Label htmlFor="revision-note">What did you update? <span className="text-red">*</span></Label><Textarea id="revision-note" required maxLength={500} value={sellerNote} onChange={(event) => setSellerNote(event.target.value)} placeholder="Explain how you addressed the review note" /></div>
        <div className="rounded-xl border border-dashed border-blue/30 bg-blue/5 p-5">
          <Label htmlFor="revision-documents" className="flex items-center gap-2"><FileText className="size-4 text-blue" />Supporting documents {needsDocuments && <span className="text-red">*</span>}</Label>
          <p className="mt-1 text-xs text-dark-4">PDF, JPG or PNG. Up to 3 files, 5 MB each. Choose only demo files; their contents are not uploaded or saved.</p>
          <Input id="revision-documents" type="file" accept=".pdf,.jpg,.jpeg,.png" multiple onChange={selectFiles} className="mt-3 bg-white" />
          {files.length > 0 && <ul className="mt-3 space-y-1 text-sm text-dark">{files.map((file) => <li key={`${file.name}-${file.lastModified}`}>• {file.name}</li>)}</ul>}
        </div>
        {error && <p role="alert" className="rounded-lg border border-red/20 bg-red-light-6 p-3 text-sm text-red">{error}</p>}
        <div className="flex flex-wrap items-center justify-between gap-3 border-t border-gray-3 pt-5"><p className="text-xs text-dark-4">A new Pending row will appear in the application list.</p><Button type="submit" className="h-11 gap-2 bg-blue px-6 text-white hover:bg-blue-dark">Submit for review <ArrowRight className="size-4" /></Button></div>
      </form>
    </CardContent>
  </Card>;
}

export default function SellerApplication() {
  const { application, applications, startSampleApplication, changeDemoStatus } = useSellerPreview();
  const [submitted, setSubmitted] = useState(false);
  const canResubmit = application?.status === "REJECTED" || application?.status === "NEED_MORE_DOC";

  return <main>
    <Breadcrumb title="Seller Applications" pages={["Seller Applications"]} />
    <section className="bg-gray-2 py-12 sm:py-16"><div className="mx-auto w-full max-w-[1170px] px-4 sm:px-8 xl:px-0">
      <SellerNav />
      <DemoNotice>Applications, review decisions and document names are kept in this browser session only. Selected file contents are not uploaded to the backend.</DemoNotice>
      {!application ? <Card className="mx-auto max-w-2xl"><CardContent className="flex flex-col items-center px-6 py-14 text-center"><div className="mb-5 flex size-16 items-center justify-center rounded-full bg-blue/10 text-blue"><ClipboardList className="size-8" /></div><h2 className="text-xl font-semibold text-dark">No applications yet</h2><p className="mt-2 max-w-md text-sm">Complete seller signup to create the first application, or load a sample to explore the review flow.</p><div className="mt-6 flex flex-wrap justify-center gap-3"><Link href="/signup/seller" className="inline-flex h-10 items-center rounded-lg bg-blue px-5 text-sm font-medium text-white hover:bg-blue-dark">Seller signup</Link><Button type="button" variant="outline" className="h-10 px-5" onClick={startSampleApplication}>Load example</Button></div></CardContent></Card> : <div className="space-y-7">
        <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_290px]">
          <Card className="overflow-hidden"><CardContent className="p-6 sm:p-8"><div className="flex flex-wrap items-start justify-between gap-4"><div><p className="text-sm font-medium text-blue">Latest application · #{application.applicationId}</p><h1 className="mt-1 text-2xl font-semibold text-dark">{application.shopName}</h1><p className="mt-1 text-sm text-dark-4">Submitted {formatDate(application.submittedAt)}</p></div><StatusBadge status={application.status} /></div><h2 className="mt-7 text-lg font-semibold text-dark">{application.status === "NEED_MORE_DOC" ? "Action needed" : application.status === "REJECTED" ? "Application rejected" : application.status === "APPROVED" ? "Your shop is ready" : "Review in progress"}</h2><p className="mt-2 text-sm leading-6">{statusCopy[application.status]}</p>{application.adminNote && <div className="mt-5 rounded-lg border border-yellow/30 bg-yellow-light-4 p-4 text-sm"><strong className="text-dark">Review note</strong><p className="mt-1">{application.adminNote}</p></div>}{submitted && application.status === "PENDING" && <p role="status" className="mt-5 rounded-lg border border-green/20 bg-green-light-6 p-4 text-sm text-green">Your updated application was added to the list for review in this demo.</p>}<div className="mt-6 flex flex-wrap gap-3">{canResubmit && <Button type="button" onClick={scrollToRevision} className="h-10 bg-blue px-5 text-white hover:bg-blue-dark">{application.status === "NEED_MORE_DOC" ? "Add documents and resubmit" : "Update and resubmit"}</Button>}{application.status === "APPROVED" && <><Link href="/seller-dashboard" className="inline-flex h-10 items-center rounded-lg bg-blue px-5 text-sm font-medium text-white hover:bg-blue-dark">Open shop dashboard</Link><Link href={`/shop/${application.sellerId}`} className="inline-flex h-10 items-center gap-2 rounded-lg border border-gray-3 bg-white px-5 text-sm font-medium text-dark hover:border-blue"><Store className="size-4" />View storefront</Link></>}</div></CardContent></Card>
          <Card><CardHeader><CardTitle>Preview review outcome</CardTitle><p className="text-sm text-dark-4">Switch the latest status to test each Seller screen.</p></CardHeader><CardContent><Label htmlFor="demo-application-status" className="mb-2 block">Application status</Label><select id="demo-application-status" value={application.status} onChange={(event) => { setSubmitted(false); changeDemoStatus(event.target.value as ApplicationStatus); }} className="h-11 w-full rounded-lg border border-gray-3 bg-gray-1 px-4 text-sm text-dark outline-none focus:ring-2 focus:ring-blue/30"><option value="PENDING">Pending</option><option value="APPROVED">Approved</option><option value="NEED_MORE_DOC">Need more documents</option><option value="REJECTED">Rejected</option></select><p className="mt-3 text-xs text-dark-4">This control updates only the local demo.</p></CardContent></Card>
        </div>

        <Card className="overflow-hidden"><CardHeader className="border-b border-gray-3"><CardTitle>Application history</CardTitle><p className="text-sm text-dark-4">Each submission appears as a separate row. Older decisions remain visible.</p></CardHeader><CardContent className="p-0"><div className="overflow-x-auto"><table className="w-full min-w-[800px] text-left text-sm"><thead className="bg-gray-1 text-xs font-medium uppercase tracking-wide text-dark-4"><tr><th scope="col" className="px-5 py-4 sm:px-6">Application</th><th scope="col" className="px-5 py-4">Submitted</th><th scope="col" className="px-5 py-4">Status</th><th scope="col" className="px-5 py-4">Documents</th><th scope="col" className="px-5 py-4">Review note</th><th scope="col" className="px-5 py-4 sm:px-6">Action</th></tr></thead><tbody className="divide-y divide-gray-3">{applications.map((item, index) => <tr key={item.applicationId} className={index === 0 ? "bg-blue/[0.03]" : "bg-white"}><td className="px-5 py-5 sm:px-6"><span className="font-semibold text-dark">#{item.applicationId}</span><span className="block text-xs text-dark-4">{item.revisionOf ? `Revision of #${item.revisionOf}` : "Original submission"}</span></td><td className="whitespace-nowrap px-5 py-5">{formatDate(item.submittedAt)}</td><td className="px-5 py-5"><StatusBadge status={item.status} /></td><td className="px-5 py-5">{item.documentNames.length ? <span title={item.documentNames.join(", ")}>{item.documentNames.length} selected</span> : "—"}</td><td className="max-w-[240px] px-5 py-5 text-dark-4">{item.adminNote || "—"}</td><td className="px-5 py-5 sm:px-6">{index === 0 && canResubmit ? <Button type="button" variant="outline" onClick={scrollToRevision} className="h-9 whitespace-nowrap px-3 text-xs">Update & resubmit</Button> : <span className="text-xs text-dark-4">{index === 0 ? "Current" : "Previous"}</span>}</td></tr>)}</tbody></table></div></CardContent></Card>

        {canResubmit && <ResubmissionForm key={application.applicationId} application={application} onSubmitted={() => setSubmitted(true)} />}
      </div>}
    </div></section>
  </main>;
}
