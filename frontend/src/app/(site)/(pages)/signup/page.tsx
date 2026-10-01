import React from "react";

import { Metadata } from "next";
import CustomerSignup from "@/components/Auth/Signup/customer";
export const metadata: Metadata = {
  title: "Create customer account | PandaStore",
  description: "Create a PandaStore customer account before applying to sell.",
};

const SignupPage = () => {
  return (
    <main>
      <CustomerSignup />
    </main>
  );
};

export default SignupPage;
