import { cookies } from "next/headers";
import { redirect } from "next/navigation";

export default async function AdminLayout({ children }: { children: React.ReactNode }) {
  const token = (await cookies()).get("auth_token")?.value;
  if (!token) redirect("/signin?callbackUrl=%2Fadmin");

  try {
    const response = await fetch(`${process.env.BACKEND_API_URL || "http://localhost:8080"}/api/auth/me`, {
      headers: { Authorization: `Bearer ${token}` }, cache: "no-store", signal: AbortSignal.timeout(8000),
    });
    const identity = response.ok ? await response.json() : null;
    if (identity?.success && identity.data?.role === "ADMIN") return children;
  } catch { /* Keep admin pages closed when identity cannot be verified. */ }
  redirect("/signin?callbackUrl=%2Fadmin");
}
