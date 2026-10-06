"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ClipboardList, KeyRound, LogOut, Mail, MapPin, Package, Phone, UserRound } from "lucide-react";
import toast from "react-hot-toast";
import Breadcrumb from "@/components/Common/Breadcrumb";
import { useAuth } from "@/app/context/AuthContext";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import Addresses from "./Addresses";

type Profile = { name: string; email: string; phone: string };

export default function MyAccount() {
  const { user, isLoading, logout, updateProfile, changePassword } = useAuth();
  const [tab, setTab] = useState<"profile" | "address" | "password">("profile");
  const [profile, setProfile] = useState<Profile>({ name: "", email: "", phone: "" });
  const [isSaving, setIsSaving] = useState(false);

  // Password change state
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [isChangingPassword, setIsChangingPassword] = useState(false);

  useEffect(() => {
    if (user) {
      setProfile({
        name: user.name || "",
        email: user.email || "",
        phone: user.phone || "",
      });
    }
  }, [user]);

  async function saveProfile(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSaving(true);
    try {
      const res = await updateProfile({
        name: profile.name,
        email: profile.email,
        phone: profile.phone,
      });
      if (res.success) {
        toast.success("Profile updated successfully!");
      } else {
        toast.error(res.error || "Failed to update profile");
      }
    } catch {
      toast.error("Failed to update profile");
    } finally {
      setIsSaving(false);
    }
  }

  async function handlePasswordChange(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!currentPassword) {
      toast.error("Please enter your current password");
      return;
    }
    if (newPassword.length < 6) {
      toast.error("New password must be at least 6 characters");
      return;
    }
    if (newPassword !== confirmPassword) {
      toast.error("New passwords do not match");
      return;
    }

    setIsChangingPassword(true);
    try {
      const res = await changePassword(currentPassword, newPassword, confirmPassword);
      if (res.success) {
        toast.success("Password changed successfully!");
        setCurrentPassword("");
        setNewPassword("");
        setConfirmPassword("");
      } else {
        toast.error(res.error || "Failed to change password");
      }
    } catch {
      toast.error("Could not reach the server");
    } finally {
      setIsChangingPassword(false);
    }
  }

  return (
    <main>
      <Breadcrumb title="My Account" pages={["My Account"]} />
      <section className="bg-gray-2 py-12 sm:py-16">
        <div className="mx-auto w-full max-w-[1170px] px-4 sm:px-8 xl:px-0">
          {!isLoading && !user && (
            <div className="mb-7 flex flex-col gap-4 rounded-xl border border-blue/20 bg-blue/5 p-5 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <h2 className="font-semibold text-dark">Sign in to your account</h2>
                <p className="mt-1 text-sm text-dark-4">
                  Please sign in to manage your profile, addresses, and order history.
                </p>
              </div>
              <Link
                href="/signin"
                className="inline-flex h-10 shrink-0 items-center justify-center rounded-lg bg-blue px-5 text-sm font-medium text-white hover:bg-blue-dark"
              >
                Sign in
              </Link>
            </div>
          )}

          <div className="grid grid-cols-1 gap-7 lg:grid-cols-[260px_minmax(0,1fr)]">
            <aside className="min-w-0 space-y-4">
              <Card>
                <CardContent className="flex items-start gap-3 p-5">
                  <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-blue/10 text-blue">
                    <UserRound size={22} aria-hidden="true" />
                  </div>
                  <div className="min-w-0 flex-1 space-y-1.5">
                    <p className="truncate font-semibold text-dark" title={user?.name}>{user?.name || "Guest customer"}</p>
                    <p className="flex min-w-0 items-center gap-1.5 text-sm text-dark-3">
                      <Mail size={14} className="shrink-0 text-blue" aria-hidden="true" />
                      <span className="min-w-0 break-all" title={user?.email}>{user?.email || "Account preview"}</span>
                    </p>
                    {user?.phone ? (
                      <p className="flex min-w-0 items-center gap-1.5 text-sm text-dark-3">
                        <Phone size={14} className="shrink-0 text-blue" aria-hidden="true" />
                        <span className="min-w-0 break-all">{user.phone}</span>
                      </p>
                    ) : null}
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardContent className="space-y-2 p-3">
                  <Button
                    type="button"
                    variant={tab === "profile" ? "default" : "ghost"}
                    className={`h-11 w-full justify-start gap-3 ${
                      tab === "profile" ? "bg-blue text-white hover:bg-blue-dark" : ""
                    }`}
                    onClick={() => setTab("profile")}
                  >
                    <UserRound className="size-4" />
                    Profile
                  </Button>

                  {user?.role === "CUSTOMER" && (
                    <Button
                      type="button"
                      variant={tab === "address" ? "default" : "ghost"}
                      className={`h-11 w-full justify-start gap-3 ${
                        tab === "address" ? "bg-blue text-white hover:bg-blue-dark" : ""
                      }`}
                      onClick={() => setTab("address")}
                    >
                      <MapPin className="size-4" />
                      Addresses
                    </Button>
                  )}

                  {user && (
                    <Button
                      type="button"
                      variant={tab === "password" ? "default" : "ghost"}
                      className={`h-11 w-full justify-start gap-3 ${
                        tab === "password" ? "bg-blue text-white hover:bg-blue-dark" : ""
                      }`}
                      onClick={() => setTab("password")}
                    >
                      <KeyRound className="size-4" />
                      Change password
                    </Button>
                  )}

                  {user?.role === "CUSTOMER" && (
                    <Link
                      href="/order-history?from=my-account"
                      className="flex h-11 items-center gap-3 rounded-lg px-2.5 text-sm font-medium text-dark hover:bg-gray-1"
                    >
                      <Package className="size-4" />
                      Order history
                    </Link>
                  )}

                  {user?.role === "SELLER" && (
                    <Link
                      href="/seller-application"
                      className="flex h-11 items-center gap-3 rounded-lg px-2.5 text-sm font-medium text-dark hover:bg-gray-1 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue focus-visible:ring-offset-2"
                    >
                      <ClipboardList className="size-4" aria-hidden="true" />
                      Application dashboard
                    </Link>
                  )}

                  {user && (
                    <button
                      type="button"
                      onClick={() => void logout()}
                      className="flex h-11 w-full items-center gap-3 rounded-lg px-2.5 text-left text-sm font-medium text-dark hover:bg-gray-1"
                    >
                      <LogOut className="size-4" />
                      Sign out
                    </button>
                  )}
                </CardContent>
              </Card>
            </aside>

            <div>
              {tab === "profile" && (
                <Card>
                  <CardHeader className="border-b border-gray-3">
                    <CardTitle>Profile details</CardTitle>
                    <p className="text-sm text-dark-4">Your account contact details.</p>
                  </CardHeader>
                  <CardContent className="pt-6">
                    <form onSubmit={saveProfile} className="space-y-5">
                      <div className="grid gap-5 sm:grid-cols-2">
                        <div className="space-y-2">
                          <Label htmlFor="profile-name">Full name</Label>
                          <Input
                            id="profile-name"
                            required
                            value={profile.name}
                            onChange={(e) => setProfile({ ...profile, name: e.target.value })}
                            placeholder="Your full name"
                          />
                        </div>
                        <div className="space-y-2">
                          <Label htmlFor="profile-email">Email address</Label>
                          <Input
                            id="profile-email"
                            type="email"
                            required
                            value={profile.email}
                            onChange={(e) => setProfile({ ...profile, email: e.target.value })}
                            placeholder="you@example.com"
                          />
                        </div>
                      </div>
                      <div className="max-w-sm space-y-2">
                        <Label htmlFor="profile-phone">Phone number</Label>
                        <Input
                          id="profile-phone"
                          type="tel"
                          inputMode="numeric"
                          maxLength={10}
                          value={profile.phone}
                          onChange={(e) => {
                            const digits = e.target.value.replace(/\D/g, "").slice(0, 10);
                            setProfile({ ...profile, phone: digits });
                          }}
                          placeholder="08XXXXXXXX (10 digits)"
                        />
                      </div>
                      <Button
                        type="submit"
                        disabled={isSaving}
                        className="h-10 bg-blue px-5 text-white hover:bg-blue-dark"
                      >
                        {isSaving ? "Saving..." : "Save profile"}
                      </Button>
                    </form>
                  </CardContent>
                </Card>
              )}

              {tab === "address" && user?.role === "CUSTOMER" && <Addresses />}

              {tab === "password" && (
                <Card>
                  <CardHeader className="border-b border-gray-3">
                    <CardTitle>Change password</CardTitle>
                    <p className="text-sm text-dark-4">Ensure your account uses a strong and unique password.</p>
                  </CardHeader>
                  <CardContent className="pt-6">
                    <form onSubmit={handlePasswordChange} className="max-w-md space-y-5">
                      <div className="space-y-2">
                        <Label htmlFor="current-password">Current password</Label>
                        <Input
                          id="current-password"
                          type="password"
                          required
                          value={currentPassword}
                          onChange={(e) => setCurrentPassword(e.target.value)}
                          placeholder="Enter current password"
                        />
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="new-password">New password</Label>
                        <Input
                          id="new-password"
                          type="password"
                          required
                          minLength={6}
                          value={newPassword}
                          onChange={(e) => setNewPassword(e.target.value)}
                          placeholder="At least 6 characters"
                        />
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="confirm-password">Confirm new password</Label>
                        <Input
                          id="confirm-password"
                          type="password"
                          required
                          minLength={6}
                          value={confirmPassword}
                          onChange={(e) => setConfirmPassword(e.target.value)}
                          placeholder="Re-enter new password"
                        />
                      </div>
                      <Button
                        type="submit"
                        disabled={isChangingPassword}
                        className="h-10 bg-blue px-5 text-white hover:bg-blue-dark"
                      >
                        {isChangingPassword ? "Updating..." : "Update password"}
                      </Button>
                    </form>
                  </CardContent>
                </Card>
              )}
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}
