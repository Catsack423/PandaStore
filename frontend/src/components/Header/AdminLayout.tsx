"use client";

import MainLayout from "./MainLayout";

const adminNavItems = [
  { id: 8, title: "Seller applications", path: "/admin" },
  { id: 9, title: "Categories", path: "/admin/categories" },
];

export default function AdminLayout() {
  return <MainLayout mode="admin" adminNavItems={adminNavItems} />;
}
