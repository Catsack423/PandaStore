import type { Metadata } from "next";
import AdminApplications from "@/components/Admin/AdminApplications";

export const metadata: Metadata = { title: "Seller applications | NextMerce" };
export default function AdminPage() { return <AdminApplications />; }
