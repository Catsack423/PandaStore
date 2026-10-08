"use client";

import { usePathname } from "next/navigation";
import { useAuth } from "@/app/context/AuthContext";
import AdminLayout from "./AdminLayout";
import MainLayout from "./MainLayout";

export default function Header() {
  const pathname = usePathname();
  const { user } = useAuth();
  return pathname.startsWith("/admin") || user?.role === "ADMIN"
    ? <AdminLayout />
    : <MainLayout />;
}
