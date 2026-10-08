import type { Metadata } from "next";
import SellerAddProduct from "@/components/Seller/SellerAddProduct";
import { getSellerProductAccess } from "@/ServerAction/products";
import Breadcrumb from "@/components/Common/Breadcrumb";
import Link from "next/link";

export const metadata: Metadata = { title: "Add Product | PandaStore" };

export default async function AddProductPage() {
  if (!(await getSellerProductAccess())) return <main><Breadcrumb title="Add Product" pages={["Add Product"]} /><section className="bg-gray-2 px-4 py-14"><div className="mx-auto max-w-[1170px] rounded-xl bg-white p-8 text-dark"><h2 className="text-xl font-semibold">An active Seller shop is required</h2><p className="mt-2 text-dark-4">Please check your account and shop status, then try again.</p><Link className="mt-5 inline-block text-blue focus-visible:ring-2 focus-visible:ring-blue" href="/seller-dashboard">Back to dashboard</Link></div></section></main>;
  return <SellerAddProduct />;
}
