"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import Breadcrumb from "@/components/Common/Breadcrumb";
import { useAuth } from "@/app/context/AuthContext";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export default function Signin() {
  const { login } = useAuth();
  const router = useRouter();
  const [usernameOrEmail, setUsernameOrEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [pending, setPending] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPending(true);
    setError("");
    const result = await login(usernameOrEmail.trim(), password);
    setPending(false);
    if (result.success === false) { setError(result.error); return; }
    router.replace("/");
    router.refresh();
  }

  return <>
    <Breadcrumb title="Sign in" pages={["Sign in"]} />
    <section className="bg-gray-2 px-4 py-16 sm:px-8"><Card className="mx-auto max-w-[520px] border-gray-3 bg-white shadow-1">
      <CardHeader className="text-center"><CardTitle className="text-2xl text-dark">Welcome back</CardTitle><p className="text-sm text-dark-4">Sign in to your PandaStore account.</p></CardHeader>
      <CardContent><form onSubmit={submit} className="space-y-5">
        <div className="space-y-2"><Label htmlFor="usernameOrEmail">Username or email</Label><Input id="usernameOrEmail" autoComplete="username" required value={usernameOrEmail} onChange={(event) => setUsernameOrEmail(event.target.value)} /></div>
        <div className="space-y-2"><Label htmlFor="signin-password">Password</Label><Input id="signin-password" type="password" autoComplete="current-password" required value={password} onChange={(event) => setPassword(event.target.value)} /></div>
        {error && <p role="alert" className="rounded-lg border border-red/20 bg-red-light-6 p-3 text-sm text-red">{error}</p>}
        <Button type="submit" disabled={pending} className="h-11 w-full bg-blue text-white hover:bg-blue-dark">{pending ? "Signing in..." : "Sign in"}</Button>
        <p className="text-center text-sm">New to PandaStore? <Link href="/signup" className="font-medium text-blue hover:underline">Create a customer account</Link></p>
      </form></CardContent>
    </Card></section>
  </>;
}
