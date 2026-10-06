"use client";

import { useEffect, useRef, useState, type ChangeEvent, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, ArrowRight, FileCheck2, Store, Upload } from "lucide-react";
import Breadcrumb from "@/components/Common/Breadcrumb";
import { useAuth } from "@/app/context/AuthContext";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import type { SellerApplicationRecord } from "@/types/sellerApplication";
import { MAX_IMAGE_SIZE, uploadSingleImage } from "@/lib/uploadImage";
import { getReviewNote } from "./ApplicationHistoryTable";

type ApplicationInput = Pick<SellerApplicationRecord,
  "shopName" | "shopDescription" | "shopPhone" | "shopEmail" | "shopAddress" |
  "sellerFirstName" | "sellerLastName" | "idCardNumber" | "idCardImageUrl" |
  "bankName" | "bankAccountName" | "bankAccountNumber" | "bankBookImageUrl">;
type DocumentField = "idCardImageUrl" | "bankBookImageUrl";

const empty: ApplicationInput = {
  shopName: "", shopDescription: "", shopPhone: "", shopEmail: "", shopAddress: "",
  sellerFirstName: "", sellerLastName: "", idCardNumber: "", idCardImageUrl: "",
  bankName: "", bankAccountName: "", bankAccountNumber: "", bankBookImageUrl: "",
};

export default function SellerApplicationForm() {
  const { user, isLoading } = useAuth();
  const router = useRouter();
  const [latest, setLatest] = useState<SellerApplicationRecord | null>(null);
  const [form, setForm] = useState<ApplicationInput>(empty);
  const [step, setStep] = useState<1 | 2>(1);
  const [loading, setLoading] = useState(true);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const [uploading, setUploading] = useState<Record<DocumentField, boolean>>({ idCardImageUrl: false, bankBookImageUrl: false });
  const [uploadErrors, setUploadErrors] = useState<Record<DocumentField, string>>({ idCardImageUrl: "", bankBookImageUrl: "" });
  const uploadingRef = useRef(new Set<DocumentField>());
  const fileInputs = useRef<Record<DocumentField, HTMLInputElement | null>>({ idCardImageUrl: null, bankBookImageUrl: null });
  const shopDetailsForm = useRef<HTMLFormElement | null>(null);

  useEffect(() => {
    if (!user) { setLoading(false); return; }
    let active = true;
    setLoading(true);
    fetch("/api/seller-applications", { cache: "no-store" })
      .then(async (response) => {
        const body = await response.json();
        if (!response.ok || !body.success || !Array.isArray(body.data)) throw new Error(body.message || "Could not load your application");
        if (!active) return;
        setError("");
        const current = body.data[0] as SellerApplicationRecord | undefined;
        setLatest(current || null);
        if (current && ["REJECTED", "NEED_MORE_DOC"].includes(current.status)) {
          setForm(Object.fromEntries(Object.keys(empty).map((key) => [key, current[key as keyof ApplicationInput] || ""])) as ApplicationInput);
        }
      })
      .catch((reason) => { if (active) setError(reason instanceof Error ? reason.message : "Could not load your application"); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [user?.id]);

  function update(field: keyof ApplicationInput, value: string) { setForm((current) => ({ ...current, [field]: value })); }
  function field(name: keyof ApplicationInput, label: string, options: { type?: string; pattern?: string; maxLength?: number; hint?: string } = {}) {
    return <div className="space-y-2"><Label htmlFor={name}>{label}</Label><Input id={name} required value={form[name]} onChange={(event) => update(name, event.target.value)} type={options.type || "text"} pattern={options.pattern} maxLength={options.maxLength} placeholder={options.hint} aria-describedby={options.hint ? `${name}-hint` : undefined} className="bg-white" />{options.hint && <p id={`${name}-hint`} className="text-xs leading-5 text-dark-4">{options.hint}</p>}</div>;
  }
  async function uploadDocument(name: DocumentField, event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file || uploadingRef.current.has(name)) return;

    uploadingRef.current.add(name);
    setUploading((current) => ({ ...current, [name]: true }));
    setUploadErrors((current) => ({ ...current, [name]: "" }));
    try {
      const uploaded = await uploadSingleImage(file, "sellerDocument");
      if (uploaded.url.length > 255) throw new Error("Uploaded image URL is too long for this application");
      update(name, uploaded.url);
    } catch (reason) {
      setUploadErrors((current) => ({ ...current, [name]: reason instanceof Error ? reason.message : "Could not upload image" }));
    } finally {
      uploadingRef.current.delete(name);
      setUploading((current) => ({ ...current, [name]: false }));
    }
  }
  function documentField(name: DocumentField, label: string) {
    return <div className="min-w-0 space-y-2">
      <Label htmlFor={name}>{label}</Label>
      <p className="text-xs leading-5 text-dark-4">{name === "idCardImageUrl" ? "An ID card image with your name and ID number clearly visible" : "A bank book page with the account holder name and account number clearly visible"} · JPG, PNG or WebP, up to {MAX_IMAGE_SIZE / 1024 / 1024} MB per file</p>
      <input id={name} ref={(node) => { fileInputs.current[name] = node; }} type="file" accept="image/jpeg,image/png,image/webp" tabIndex={-1} disabled={uploading[name] || pending} onChange={(event) => void uploadDocument(name, event)} className="sr-only" />
      <div className="flex flex-wrap items-center gap-3">
        <Button type="button" disabled={uploading[name] || pending} onClick={() => fileInputs.current[name]?.click()} aria-label={`Upload ${label.toLowerCase()}`} className="h-11 gap-2 bg-blue px-4 text-white hover:bg-blue-dark"><Upload size={16} aria-hidden="true" />{form[name] ? "Replace file" : "Upload file"}</Button>
        <span aria-live="polite" className={`text-xs font-medium ${form[name] && !uploading[name] ? "text-green-dark" : "text-dark-4"}`}>{uploading[name] ? "Uploading..." : form[name] ? "File uploaded" : "No file yet"}</span>
      </div>
      {form[name] && <a href={form[name]} target="_blank" rel="noopener noreferrer" className="inline-block text-xs font-medium text-blue underline underline-offset-2">View document</a>}
      {uploadErrors[name] && <p role="alert" className="text-sm text-red">{uploadErrors[name]}</p>}
    </div>;
  }
  function next(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setStep(2);
    setError("");
    window.scrollTo({ top: 0, behavior: "smooth" });
  }
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (pending || uploadingRef.current.size > 0) return;
    if (!form.idCardImageUrl || !form.bankBookImageUrl) {
      setError("Upload both document images before submitting");
      return;
    }
    setPending(true);
    setError("");
    try {
      const response = await fetch("/api/seller-applications", {
        method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(form),
      });
      const body = await response.json();
      if (!response.ok || !body.success) throw new Error(body.message || "Could not submit application");
      router.replace("/seller-application");
      router.refresh();
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Could not submit application");
      setPending(false);
    }
  }

  const blocked = latest && !["REJECTED", "NEED_MORE_DOC"].includes(latest.status);
  return <main>
    <Breadcrumb title="Apply to sell" pages={["Seller Applications", "Apply"]} />
    <section className="bg-gray-2 py-12 sm:py-16"><div className="mx-auto w-full max-w-[760px] px-4 sm:px-8 xl:px-0">
      <Link href="/seller-application" className="mb-6 inline-flex items-center gap-2 text-sm font-medium text-blue hover:underline"><ArrowLeft className="size-4" />Applications</Link>
      {isLoading || loading ? <Card><CardContent className="py-16 text-center">Loading your account...</CardContent></Card>
        : !user ? <Card><CardContent className="py-16 text-center"><h2 className="text-xl font-semibold text-dark">Sign in as a customer first</h2><Link href="/signin?callbackUrl=%2Fseller-application%2Fapply" className="mt-5 inline-flex rounded-lg bg-blue px-5 py-3 text-sm text-white">Sign in</Link></CardContent></Card>
        : user.role !== "CUSTOMER" ? <Card><CardContent className="py-16 text-center"><h2 className="text-xl font-semibold text-dark">Customer account required</h2><p className="mt-2 text-sm">Seller applications start from a customer account.</p></CardContent></Card>
        : blocked ? <Card><CardContent className="py-16 text-center"><h2 className="text-xl font-semibold text-dark">An application is already {latest.status === "PENDING" ? "under review" : "approved"}</h2><Link href="/seller-application" className="mt-5 inline-flex rounded-lg bg-blue px-5 py-3 text-sm text-white">View application</Link></CardContent></Card>
        : error && !latest && Object.values(form).every((value) => !value) ? <Card><CardContent className="py-16 text-center"><p role="alert" className="text-red">{error}</p><Button type="button" onClick={() => window.location.reload()} className="mt-4">Try again</Button></CardContent></Card>
        : <>
          <div className="mb-7"><p className="text-sm font-medium text-blue">Customer account · {user.name}</p><h1 className="mt-1 text-3xl font-semibold text-dark">{latest ? "Revise your seller application" : "Open your shop"}</h1><p className="mt-2 text-sm text-dark-4">Your existing account stays the same. The review team will assess your shop and identity details.</p></div>
          <p className="mb-6 rounded-lg border border-blue/20 bg-blue/5 p-4 text-sm leading-6 text-dark">All fields are required. Enter your shop details in step 1, then your identity and bank details in step 2. Upload both document images before selecting Submit for review.</p>
          {latest && <div className="mb-6 rounded-xl border border-yellow/30 bg-yellow-light-4 p-5 text-sm"><strong className="text-dark">{latest.status === "REJECTED" ? "Reason for rejection" : "Requested changes"}</strong><p className="mt-2 whitespace-pre-line break-words leading-6">{getReviewNote(latest)}</p></div>}
          <nav aria-label="Application steps" className="mb-6 grid grid-cols-2 gap-3 text-sm">
            <button type="button" aria-current={step === 1 ? "step" : undefined} onClick={() => { setStep(1); setError(""); }} className={`w-full rounded-xl border p-4 text-left transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue/40 ${step === 1 ? "border-blue bg-blue/5 text-blue" : "border-gray-3 bg-white hover:border-blue/40 hover:bg-blue/5"}`}><Store className="mb-2 size-5" /><strong className="block">1. Shop details</strong></button>
            <button type="button" aria-current={step === 2 ? "step" : undefined} onClick={() => { if (step === 1) shopDetailsForm.current?.requestSubmit(); }} className={`w-full rounded-xl border p-4 text-left transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue/40 ${step === 2 ? "border-blue bg-blue/5 text-blue" : "border-gray-3 bg-white hover:border-blue/40 hover:bg-blue/5"}`}><FileCheck2 className="mb-2 size-5" /><strong className="block">2. Identity &amp; payment</strong></button>
          </nav>
          <Card><CardHeader className="border-b border-gray-3 sm:px-7"><CardTitle>{step === 1 ? "Tell us about your shop" : "Verify the person behind the shop"}</CardTitle><p className="text-sm text-dark-4">{step === 1 ? "Use the contact details customers will see." : "Provide your legal details and upload document images for review."}</p></CardHeader><CardContent className="pt-6 sm:px-7 sm:pb-7">
            {step === 1 ? <form ref={shopDetailsForm} onSubmit={next} className="space-y-5">
              {field("shopName", "Shop name", { maxLength: 100 })}
              <div className="space-y-2"><Label htmlFor="shopDescription">Shop description</Label><Textarea id="shopDescription" required value={form.shopDescription} onChange={(event) => update("shopDescription", event.target.value)} rows={4} /></div>
              <div className="grid gap-5 sm:grid-cols-2">{field("shopPhone", "Shop phone", { type: "tel", pattern: "[0-9]{9,15}", hint: "9–15 digits" })}{field("shopEmail", "Shop email", { type: "email", maxLength: 100 })}</div>
              <div className="space-y-2"><Label htmlFor="shopAddress">Shop address</Label><Textarea id="shopAddress" required maxLength={255} value={form.shopAddress} onChange={(event) => update("shopAddress", event.target.value)} rows={3} /></div>
              <div className="flex flex-wrap items-center justify-between gap-3 border-t border-gray-3 pt-5"><p className="text-xs text-dark-4">Step 1 of 2 · Your application has not been submitted yet</p><Button type="submit" className="h-11 gap-2 bg-blue px-6 text-white hover:bg-blue-dark">Continue <ArrowRight className="size-4" /></Button></div>
            </form> : <form onSubmit={submit} className="space-y-7">
              <div className="grid gap-x-5 gap-y-5 sm:grid-cols-2">
                {field("sellerFirstName", "Legal first name", { maxLength: 100 })}
                {field("sellerLastName", "Legal last name", { maxLength: 100 })}
                <div className="space-y-5">
                  {field("idCardNumber", "Identity card number", { pattern: "[0-9]{13}", maxLength: 13, hint: "13 digits" })}
                  {documentField("idCardImageUrl", "Identity card image")}
                </div>
              </div>
              <div className="border-t border-gray-3 pt-6">
                <h3 className="mb-5 font-semibold text-dark">Bank account for payouts</h3>
                <div className="grid gap-x-5 gap-y-5 sm:grid-cols-2">
                  {field("bankName", "Bank name", { maxLength: 100 })}
                  {field("bankAccountName", "Account holder name", { maxLength: 100 })}
                  <div className="space-y-5">
                    {field("bankAccountNumber", "Account number", { maxLength: 30 })}
                    {documentField("bankBookImageUrl", "Bank book image")}
                  </div>
                </div>
              </div>
              {error && <p role="alert" className="rounded-lg border border-red/20 bg-red-light-6 p-4 text-sm text-red">{error}</p>}
              <p className="text-xs leading-5 text-dark-4">Step 2 of 2 · Check your details and both document images before submitting</p>
              <div className="flex flex-wrap justify-between gap-3 border-t border-gray-3 pt-5"><Button type="button" variant="outline" onClick={() => setStep(1)} className="h-11">Back</Button><Button type="submit" disabled={pending || uploading.idCardImageUrl || uploading.bankBookImageUrl} className="h-11 bg-blue px-6 text-white hover:bg-blue-dark">{pending ? "Submitting..." : uploading.idCardImageUrl || uploading.bankBookImageUrl ? "Uploading..." : "Submit for review"}</Button></div>
            </form>}
          </CardContent></Card>
        </>}
    </div></section>
  </main>;
}
