"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { LogOut, MapPin, Package, UserRound } from "lucide-react";
import Breadcrumb from "@/components/Common/Breadcrumb";
import { useAuth } from "@/app/context/AuthContext";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import Addresses from "./Addresses";

type Profile = { name: string; email: string; phone: string };

export default function MyAccount() {
  const { user, isLoading, logout } = useAuth();
  const [tab, setTab] = useState<"profile" | "address">("profile");
  const [profile, setProfile] = useState<Profile>({ name: "", email: "", phone: "" });
  const [savedProfile, setSavedProfile] = useState<Profile | null>(null);
  const [message, setMessage] = useState("");

  useEffect(() => {
    if (user) setProfile((current) => ({ ...current, name: current.name || user.name, email: current.email || user.email }));
  }, [user]);

  function saveProfile(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSavedProfile({ ...profile });
    setMessage("Profile saved for this visit. Connect the customer API to keep it permanently.");
  }

  return <main>
    <Breadcrumb title="My Account" pages={["My Account"]} />
    <section className="bg-gray-2 py-12 sm:py-16">
      <div className="mx-auto w-full max-w-[1170px] px-4 sm:px-8 xl:px-0">
        {!isLoading && !user && <div className="mb-7 flex flex-col gap-4 rounded-xl border border-blue/20 bg-blue/5 p-5 sm:flex-row sm:items-center sm:justify-between">
          <div><h2 className="font-semibold text-dark">Sign in to your account</h2><p className="mt-1 text-sm">You can explore and edit this account preview now. Changes last only while this page is open.</p></div>
          <Link href="/signin" className="inline-flex h-10 shrink-0 items-center justify-center rounded-lg bg-blue px-5 text-sm font-medium text-white hover:bg-blue-dark">Sign in</Link>
        </div>}
        <div className="grid gap-7 lg:grid-cols-[260px_1fr]">
          <aside className="space-y-4">
            <Card><CardContent className="flex items-center gap-3 p-5"><div className="flex size-11 items-center justify-center rounded-full bg-blue/10 text-blue"><UserRound className="size-5" /></div><div className="min-w-0"><p className="truncate font-medium text-dark">{user?.name || savedProfile?.name || "Guest customer"}</p><p className="truncate text-xs text-dark-4">{user?.email || savedProfile?.email || "Account preview"}</p></div></CardContent></Card>
            <Card><CardContent className="space-y-2 p-3">
              <Button type="button" variant={tab === "profile" ? "default" : "ghost"} className={`h-11 w-full justify-start gap-3 ${tab === "profile" ? "bg-blue text-white hover:bg-blue-dark" : ""}`} onClick={() => { setTab("profile"); setMessage(""); }}><UserRound className="size-4" />Profile</Button>
              {user?.role === "CUSTOMER" && <Button type="button" variant={tab === "address" ? "default" : "ghost"} className={`h-11 w-full justify-start gap-3 ${tab === "address" ? "bg-blue text-white hover:bg-blue-dark" : ""}`} onClick={() => { setTab("address"); setMessage(""); }}><MapPin className="size-4" />Addresses</Button>}
              {user?.role === "CUSTOMER" && <Link href="/order-history?from=my-account" className="flex h-11 items-center gap-3 rounded-lg px-2.5 text-sm font-medium text-dark hover:bg-gray-1"><Package className="size-4" />Order history</Link>}
              {user && <button type="button" onClick={() => void logout()} className="flex h-11 w-full items-center gap-3 rounded-lg px-2.5 text-left text-sm font-medium text-dark hover:bg-gray-1"><LogOut className="size-4" />Sign out</button>}
            </CardContent></Card>
          </aside>
          <div>
            {tab === "profile" || user?.role !== "CUSTOMER" ? <Card><CardHeader className="border-b border-gray-3"><CardTitle>Profile details</CardTitle><p className="text-sm text-dark-4">Your account contact details.</p></CardHeader><CardContent className="pt-6">
              <form onSubmit={saveProfile} className="space-y-5">
                <div className="grid gap-5 sm:grid-cols-2"><div className="space-y-2"><Label htmlFor="profile-name">Full name</Label><Input id="profile-name" required value={profile.name} onChange={(e) => setProfile({ ...profile, name: e.target.value })} placeholder="Your full name" /></div><div className="space-y-2"><Label htmlFor="profile-email">Email address</Label><Input id="profile-email" type="email" required value={profile.email} onChange={(e) => setProfile({ ...profile, email: e.target.value })} placeholder="you@example.com" /></div></div>
                <div className="max-w-sm space-y-2"><Label htmlFor="profile-phone">Phone number</Label><Input id="profile-phone" type="tel" value={profile.phone} onChange={(e) => setProfile({ ...profile, phone: e.target.value })} placeholder="Your phone number" /></div>
                <Button type="submit" className="h-10 bg-blue px-5 text-white hover:bg-blue-dark">Save profile</Button>
              </form>
              {savedProfile && <p className="mt-5 text-sm text-dark-4">Current preview: {savedProfile.name} · {savedProfile.email}</p>}
            </CardContent></Card> : <Addresses />}
            {message && <p role="status" className="mt-4 rounded-lg border border-blue/20 bg-blue/5 px-4 py-3 text-sm text-dark">{message}</p>}
          </div>
        </div>
      </div>
    </section>
  </main>;
}
