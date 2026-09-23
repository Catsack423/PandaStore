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

type Profile = { name: string; email: string; phone: string };
type Address = { recipient: string; phone: string; street: string; city: string; region: string; postalCode: string };
const emptyAddress: Address = { recipient: "", phone: "", street: "", city: "", region: "", postalCode: "" };

export default function MyAccount() {
  const { user, isLoading, logout } = useAuth();
  const [tab, setTab] = useState<"profile" | "address">("profile");
  const [profile, setProfile] = useState<Profile>({ name: "", email: "", phone: "" });
  const [address, setAddress] = useState<Address>(emptyAddress);
  const [savedProfile, setSavedProfile] = useState<Profile | null>(null);
  const [savedAddress, setSavedAddress] = useState<Address | null>(null);
  const [message, setMessage] = useState("");

  useEffect(() => {
    if (user) setProfile((current) => ({ ...current, name: current.name || user.name, email: current.email || user.email }));
  }, [user]);

  function saveProfile(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSavedProfile({ ...profile });
    setMessage("Profile saved for this visit. Connect the customer API to keep it permanently.");
  }

  function saveAddress(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSavedAddress({ ...address });
    setMessage("Address saved for this visit. Connect the address API to keep it permanently.");
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
              <Button type="button" variant={tab === "address" ? "default" : "ghost"} className={`h-11 w-full justify-start gap-3 ${tab === "address" ? "bg-blue text-white hover:bg-blue-dark" : ""}`} onClick={() => { setTab("address"); setMessage(""); }}><MapPin className="size-4" />Addresses</Button>
              <Link href="/order-history" className="flex h-11 items-center gap-3 rounded-lg px-2.5 text-sm font-medium text-dark hover:bg-gray-1"><Package className="size-4" />Order history</Link>
              {user && <button type="button" onClick={() => void logout()} className="flex h-11 w-full items-center gap-3 rounded-lg px-2.5 text-left text-sm font-medium text-dark hover:bg-gray-1"><LogOut className="size-4" />Sign out</button>}
            </CardContent></Card>
          </aside>
          <div>
            {tab === "profile" ? <Card><CardHeader className="border-b border-gray-3"><CardTitle>Profile details</CardTitle><p className="text-sm text-dark-4">Your contact details for future orders.</p></CardHeader><CardContent className="pt-6">
              <form onSubmit={saveProfile} className="space-y-5">
                <div className="grid gap-5 sm:grid-cols-2"><div className="space-y-2"><Label htmlFor="profile-name">Full name</Label><Input id="profile-name" required value={profile.name} onChange={(e) => setProfile({ ...profile, name: e.target.value })} placeholder="Your full name" /></div><div className="space-y-2"><Label htmlFor="profile-email">Email address</Label><Input id="profile-email" type="email" required value={profile.email} onChange={(e) => setProfile({ ...profile, email: e.target.value })} placeholder="you@example.com" /></div></div>
                <div className="max-w-sm space-y-2"><Label htmlFor="profile-phone">Phone number</Label><Input id="profile-phone" type="tel" value={profile.phone} onChange={(e) => setProfile({ ...profile, phone: e.target.value })} placeholder="Your phone number" /></div>
                <Button type="submit" className="h-10 bg-blue px-5 text-white hover:bg-blue-dark">Save profile</Button>
              </form>
              {savedProfile && <p className="mt-5 text-sm text-dark-4">Current preview: {savedProfile.name} · {savedProfile.email}</p>}
            </CardContent></Card> : <Card><CardHeader className="border-b border-gray-3"><CardTitle>Shipping address</CardTitle><p className="text-sm text-dark-4">Add a delivery address for checkout.</p></CardHeader><CardContent className="pt-6">
              <form onSubmit={saveAddress} className="space-y-5">
                <div className="grid gap-5 sm:grid-cols-2"><div className="space-y-2"><Label htmlFor="address-recipient">Recipient</Label><Input id="address-recipient" required value={address.recipient} onChange={(e) => setAddress({ ...address, recipient: e.target.value })} placeholder="Full name" /></div><div className="space-y-2"><Label htmlFor="address-phone">Phone number</Label><Input id="address-phone" type="tel" required value={address.phone} onChange={(e) => setAddress({ ...address, phone: e.target.value })} placeholder="Phone number" /></div></div>
                <div className="space-y-2"><Label htmlFor="address-street">Street address</Label><Input id="address-street" required value={address.street} onChange={(e) => setAddress({ ...address, street: e.target.value })} placeholder="House number and street" /></div>
                <div className="grid gap-5 sm:grid-cols-3"><div className="space-y-2"><Label htmlFor="address-city">City</Label><Input id="address-city" required value={address.city} onChange={(e) => setAddress({ ...address, city: e.target.value })} /></div><div className="space-y-2"><Label htmlFor="address-region">State / Region</Label><Input id="address-region" required value={address.region} onChange={(e) => setAddress({ ...address, region: e.target.value })} /></div><div className="space-y-2"><Label htmlFor="address-postal">Postal code</Label><Input id="address-postal" required value={address.postalCode} onChange={(e) => setAddress({ ...address, postalCode: e.target.value })} /></div></div>
                <Button type="submit" className="h-10 bg-blue px-5 text-white hover:bg-blue-dark">Save address</Button>
              </form>
              {savedAddress && <p className="mt-5 text-sm text-dark-4">Current preview: {savedAddress.street}, {savedAddress.city}, {savedAddress.region} {savedAddress.postalCode}</p>}
            </CardContent></Card>}
            {message && <p role="status" className="mt-4 rounded-lg border border-blue/20 bg-blue/5 px-4 py-3 text-sm text-dark">{message}</p>}
          </div>
        </div>
      </div>
    </section>
  </main>;
}
