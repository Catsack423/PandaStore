import CustomerSignup from "@/components/Auth/Signup/customer";
import React from "react";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Customer Signup | PandaStore",
  description: "สมัครสมาชิกบัญชีลูกค้าทั่วไปสำหรับช้อปปิ้งใน PandaStore",
};

const CustomerSignupPage = () => {
  return (
    <main>
      <CustomerSignup />
    </main>
  );
};

export default CustomerSignupPage;
