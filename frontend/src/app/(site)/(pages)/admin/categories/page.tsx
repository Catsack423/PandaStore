import type { Metadata } from "next";
import AdminCategories from "@/components/Admin/AdminCategories";

export const metadata: Metadata = { title: "Categories | PandaStore" };
export default function AdminCategoriesPage() { return <AdminCategories />; }
