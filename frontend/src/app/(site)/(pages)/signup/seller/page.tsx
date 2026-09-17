import SellerSignup from "@/components/Auth/Signup/seller";
import React from "react";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Seller Signup | PandaStore",
  description: "สมัครเปิดร้านค้าออนไลน์และลงขายสินค้าบน PandaStore",
};

const SellerSignupPage = () => {
  return (
    <main>
      <SellerSignup />
    </main>
  );
};

export default SellerSignupPage;

