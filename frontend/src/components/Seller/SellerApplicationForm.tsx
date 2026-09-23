"use client";

import { useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, ArrowRight, FileCheck2, Store } from "lucide-react";
import Breadcrumb from "@/components/Common/Breadcrumb";
import { useAuth } from "@/app/context/AuthContext";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import type { SellerApplicationRecord } from "@/types/sellerApplication";

type ApplicationInput = Pick<SellerApplicationRecord,
  "shopName" | "shopDescription" | "shopPhone" | "shopEmail" | "shopAddress" |
  "sellerFirstName" | "sellerLastName" | "idCardNumber" | "idCardImageUrl" |
  "bankName" | "bankAccountName" | "bankAccountNumber" | "bankBookImageUrl">;

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
    return <div className="space-y-2"><Label htmlFor={name}>{label}</Label><Input id={name} required value={form[name]} onChange={(event) => update(name, event.target.value)} type={options.type || "text"} pattern={options.pattern} maxLength={options.maxLength} placeholder={options.hint} className="bg-white" /></div>;
  }
  function next(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setStep(2);
    setError("");
    window.scrollTo({ top: 0, behavior: "smooth" });
  }
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
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
    <section className="bg-gray-2 py-12 sm:py-16"><div className="mx-auto max-w-[850px] px-4 sm:px-8">
      <Link href="/seller-application" className="mb-6 inline-flex items-center gap-2 text-sm font-medium text-blue hover:underline"><ArrowLeft className="size-4" />Applications</Link>
      {isLoading || loading ? <Card><CardContent className="py-16 text-center">Loading your account...</CardContent></Card>
        : !user ? <Card><CardContent className="py-16 text-center"><h2 className="text-xl font-semibold text-dark">Sign in as a customer first</h2><Link href="/signin?callbackUrl=%2Fseller-application%2Fapply" className="mt-5 inline-flex rounded-lg bg-blue px-5 py-3 text-sm text-white">Sign in</Link></CardContent></Card>
        : user.role !== "CUSTOMER" ? <Card><CardContent className="py-16 text-center"><h2 className="text-xl font-semibold text-dark">Customer account required</h2><p className="mt-2 text-sm">Seller applications start from a customer account.</p></CardContent></Card>
        : blocked ? <Card><CardContent className="py-16 text-center"><h2 className="text-xl font-semibold text-dark">An application is already {latest.status === "PENDING" ? "under review" : "approved"}</h2><Link href="/seller-application" className="mt-5 inline-flex rounded-lg bg-blue px-5 py-3 text-sm text-white">View application</Link></CardContent></Card>
        : error && !latest && Object.values(form).every((value) => !value) ? <Card><CardContent className="py-16 text-center"><p role="alert" className="text-red">{error}</p><Button type="button" onClick={() => window.location.reload()} className="mt-4">Try again</Button></CardContent></Card>
        : <>
          <div className="mb-7"><p className="text-sm font-medium text-blue">Customer account · {user.name}</p><h1 className="mt-1 text-3xl font-semibold text-dark">{latest ? "Revise your seller application" : "Open your shop"}</h1><p className="mt-2 text-sm text-dark-4">Your existing account stays the same. The review team will assess your shop and identity details.</p></div>
          {latest?.adminNote && <div className="mb-6 rounded-xl border border-yellow/30 bg-yellow-light-4 p-5 text-sm"><strong className="text-dark">Review note</strong><p className="mt-1">{latest.adminNote}</p></div>}
          <div className="mb-6 grid grid-cols-2 gap-3 text-sm"><div className={`rounded-xl border p-4 ${step === 1 ? "border-blue bg-blue/5 text-blue" : "border-gray-3 bg-white"}`}><Store className="mb-2 size-5" /><strong className="block">1. Shop details</strong></div><div className={`rounded-xl border p-4 ${step === 2 ? "border-blue bg-blue/5 text-blue" : "border-gray-3 bg-white"}`}><FileCheck2 className="mb-2 size-5" /><strong className="block">2. Identity & payment</strong></div></div>
          <Card><CardHeader className="border-b border-gray-3"><CardTitle>{step === 1 ? "Tell us about your shop" : "Verify the person behind the shop"}</CardTitle><p className="text-sm text-dark-4">{step === 1 ? "Use the contact details customers will see." : "Provide your legal details and document URLs for review."}</p></CardHeader><CardContent className="pt-6">
            {step === 1 ? <form onSubmit={next} className="space-y-5">
              {field("shopName", "Shop name", { maxLength: 100 })}
              <div className="space-y-2"><Label htmlFor="shopDescription">Shop description</Label><Textarea id="shopDescription" required value={form.shopDescription} onChange={(event) => update("shopDescription", event.target.value)} rows={4} /></div>
              <div className="grid gap-5 sm:grid-cols-2">{field("shopPhone", "Shop phone", { type: "tel", pattern: "[0-9]{9,15}", hint: "9–15 digits" })}{field("shopEmail", "Shop email", { type: "email", maxLength: 100 })}</div>
              <div className="space-y-2"><Label htmlFor="shopAddress">Shop address</Label><Textarea id="shopAddress" required maxLength={255} value={form.shopAddress} onChange={(event) => update("shopAddress", event.target.value)} rows={3} /></div>
              <div className="flex justify-end border-t border-gray-3 pt-5"><Button type="submit" className="h-11 gap-2 bg-blue px-6 text-white hover:bg-blue-dark">Continue <ArrowRight className="size-4" /></Button></div>
            </form> : <form onSubmit={submit} className="space-y-5">
              <div className="grid gap-5 sm:grid-cols-2">{field("sellerFirstName", "Legal first name", { maxLength: 100 })}{field("sellerLastName", "Legal last name", { maxLength: 100 })}</div>
              {field("idCardNumber", "Identity card number", { pattern: "[0-9]{13}", maxLength: 13, hint: "13 digits" })}
              {field("idCardImageUrl", "Identity card image URL", { type: "url", maxLength: 255, hint: "https://..." })}
              <div className="border-t border-gray-3 pt-5"><h3 className="mb-4 font-semibold text-dark">Bank account for payouts</h3><div className="space-y-5">{field("bankName", "Bank name", { maxLength: 100 })}{field("bankAccountName", "Account holder name", { maxLength: 100 })}<div className="grid gap-5 sm:grid-cols-2">{field("bankAccountNumber", "Account number", { maxLength: 30 })}{field("bankBookImageUrl", "Bank book image URL", { type: "url", maxLength: 255, hint: "https://..." })}</div></div></div>
              {error && <p role="alert" className="rounded-lg border border-red/20 bg-red-light-6 p-4 text-sm text-red">{error}</p>}
              <div className="flex justify-between gap-3 border-t border-gray-3 pt-5"><Button type="button" variant="outline" onClick={() => setStep(1)} className="h-11">Back</Button><Button type="submit" disabled={pending} className="h-11 bg-blue px-6 text-white hover:bg-blue-dark">{pending ? "Submitting..." : "Submit for review"}</Button></div>
            </form>}
          </CardContent></Card>
        </>}
    </div></section>
  </main>;
}
