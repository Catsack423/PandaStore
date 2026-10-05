import type { Metadata } from "next";
import AdminCategories from "@/components/Admin/AdminCategories";

export const metadata: Metadata = { title: "Categories | NextMerce" };
export default function AdminCategoriesPage() { return <AdminCategories />; }
