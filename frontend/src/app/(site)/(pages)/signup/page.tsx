import Signup from "@/components/Auth/Signup";
import React from "react";

import { Metadata } from "next";
import SignSelection from "@/components/Auth/SignSelect";
export const metadata: Metadata = {
  title: "Signup Page | NextCommerce Nextjs E-commerce template",
  description: "This is Signup Page for NextCommerce Template",
  // other metadata
};

const SignupPage = () => {
  return (
    <main>
      <SignSelection />
    </main>
  );
};

export default SignupPage;
