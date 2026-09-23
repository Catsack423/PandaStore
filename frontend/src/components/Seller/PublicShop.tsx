"use client";

import Link from "next/link";
import { Mail, MapPin, PackageOpen, Phone, Store } from "lucide-react";
import Breadcrumb from "@/components/Common/Breadcrumb";
import { useSellerPreview } from "@/app/context/SellerPreviewContext";
import { Card, CardContent } from "@/components/ui/card";
import { DemoNotice } from "./Shared";

export type PublicShopData = {
  sellerId: number;
  shopName: string;
  shopDescription: string;
  shopPhone: string;
  shopEmail: string;
  shopAddress: string;
  status: "ACTIVE" | "PENDING" | "REJECTED" | "SUSPENDED";
};

export default function PublicShop({ shopId, shop }: { shopId: number; shop: PublicShopData | null }) {
  const { application } = useSellerPreview();
  const demo = shopId === 0;
  const previewShop: PublicShopData | null = demo && application?.status === "APPROVED" ? {
    sellerId: 0, shopName: application.shopName, shopDescription: application.shopDescription,
    shopPhone: application.shopPhone, shopEmail: application.shopEmail, shopAddress: application.shopAddress,
    status: "ACTIVE",
  } : null;
  const activeShop = demo ? previewShop : shop?.status === "ACTIVE" ? shop : null;

  return <main>
    <Breadcrumb title="Shop" pages={["Shop"]} />
    <section className="bg-gray-2 py-12 sm:py-16"><div className="mx-auto w-full max-w-[1170px] px-4 sm:px-8 xl:px-0">
      {demo && <DemoNotice>This storefront uses the approved application in your current demo session. It is not a published shop.</DemoNotice>}
      {!activeShop ? <Card><CardContent className="flex min-h-[300px] flex-col items-center justify-center px-6 py-14 text-center"><div className="mb-5 flex size-16 items-center justify-center rounded-full bg-blue/10 text-blue"><Store className="size-8" /></div><h2 className="text-xl font-semibold text-dark">Shop unavailable</h2><p className="mt-2 max-w-md text-sm">{demo ? "Approve the demo application to preview this storefront." : "This shop is not available to browse right now."}</p><Link href={demo ? "/seller-application" : "/shop-with-sidebar"} className="mt-6 inline-flex h-10 items-center rounded-lg bg-blue px-5 text-sm font-medium text-white hover:bg-blue-dark">{demo ? "View application" : "Browse all products"}</Link></CardContent></Card> : <>
        <div className="mb-7 rounded-xl bg-white p-6 shadow-1 sm:p-9"><div className="flex flex-wrap items-start gap-5"><div className="flex size-16 shrink-0 items-center justify-center rounded-xl bg-blue/10 text-blue"><Store className="size-8" /></div><div className="min-w-0 flex-1"><p className="text-sm font-medium text-blue">Storefront</p><h1 className="mt-1 text-3xl font-semibold text-dark">{activeShop.shopName}</h1><p className="mt-3 max-w-2xl text-sm leading-6">{activeShop.shopDescription}</p></div></div><div className="mt-7 grid gap-3 border-t border-gray-3 pt-6 text-sm sm:grid-cols-3"><p className="flex items-start gap-2"><MapPin className="mt-0.5 size-4 shrink-0 text-blue" />{activeShop.shopAddress}</p><p className="flex items-start gap-2"><Mail className="mt-0.5 size-4 shrink-0 text-blue" />{activeShop.shopEmail}</p><p className="flex items-start gap-2"><Phone className="mt-0.5 size-4 shrink-0 text-blue" />{activeShop.shopPhone}</p></div></div>
        <Card><CardContent className="flex min-h-[280px] flex-col items-center justify-center px-6 py-12 text-center"><PackageOpen className="mb-4 size-10 text-blue" /><h2 className="text-lg font-semibold text-dark">No products from this shop yet</h2><p className="mt-2 max-w-md text-sm">Products will appear here when the shop catalog API is connected.</p><Link href="/shop-with-sidebar" className="mt-5 text-sm font-medium text-blue hover:underline">Browse all products</Link></CardContent></Card>
      </>}
    </div></section>
  </main>;
}
