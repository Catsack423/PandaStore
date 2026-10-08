import SellerApplicationForm from "@/components/Seller/SellerApplicationForm";
import React from "react";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Apply to sell | PandaStore",
  description: "Apply to open a PandaStore shop with your customer account.",
};

const SellerSignupPage = () => {
  return (
    <main>
      <SellerApplicationForm />
    </main>
  );
};

export default SellerSignupPage;

