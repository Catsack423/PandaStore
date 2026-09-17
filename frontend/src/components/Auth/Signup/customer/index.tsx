"use client";

import Breadcrumb from "@/components/Common/Breadcrumb";
import Link from "next/link";
import React, { useState } from "react";
import { useRouter } from "next/navigation";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { CustomerFormData, FormErrors } from "./types";
import { CustomerForm } from "./CustomerForm";

const initialFormData: CustomerFormData = {
  username: "",
  fullName: "",
  email: "",
  phoneNumber: "",
  password: "",
  confirmPassword: "",
};

const CustomerSignup = () => {
  const router = useRouter();

  const [formData, setFormData] = useState<CustomerFormData>(initialFormData);
  const [errors, setErrors] = useState<FormErrors>({});
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  const validate = (): boolean => {
    const newErrors: FormErrors = {};

    // 1. Username
    if (!formData.username.trim()) {
      newErrors.username = "กรุณากรอกชื่อผู้ใช้";
    } else if (formData.username.length < 3 || formData.username.length > 50) {
      newErrors.username = "ชื่อผู้ใช้ต้องมีความยาวระหว่าง 3 ถึง 50 ตัวอักษร";
    }

    // 2. Full Name
    if (!formData.fullName.trim()) {
      newErrors.fullName = "กรุณากรอกชื่อ-นามสกุล";
    } else if (formData.fullName.length > 100) {
      newErrors.fullName = "ชื่อ-นามสกุลต้องมีความยาวไม่เกิน 100 ตัวอักษร";
    }

    // 3. Email
    if (!formData.email.trim()) {
      newErrors.email = "กรุณากรอกอีเมล";
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      newErrors.email = "รูปแบบอีเมลไม่ถูกต้อง";
    } else if (formData.email.length > 100) {
      newErrors.email = "อีเมลต้องมีความยาวไม่เกิน 100 ตัวอักษร";
    }

    // 4. Phone Number (9-15 digits as per backend CreateCustomerRequest)
    if (!formData.phoneNumber.trim()) {
      newErrors.phoneNumber = "กรุณากรอกเบอร์โทรศัพท์";
    } else if (
      !/^[0-9]{9,15}$/.test(formData.phoneNumber.replace(/[-\s]/g, ""))
    ) {
      newErrors.phoneNumber = "เบอร์โทรศัพท์ต้องเป็นตัวเลขความยาว 9-15 หลัก";
    }

    // 5. Password
    if (!formData.password) {
      newErrors.password = "กรุณากรอกรหัสผ่าน";
    } else if (formData.password.length < 6) {
      newErrors.password = "รหัสผ่านต้องมีความยาวอย่างน้อย 6 ตัวอักษร";
    }

    // 6. Confirm Password
    if (!formData.confirmPassword) {
      newErrors.confirmPassword = "กรุณายืนยันรหัสผ่าน";
    } else if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = "รหัสผ่านไม่ตรงกัน";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name as keyof FormErrors]) {
      setErrors((prev) => ({ ...prev, [name]: undefined }));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    setLoading(true);
    setErrors({});

    try {
      // const payload = {
      //   username: formData.username.trim(),
      //   fullName: formData.fullName.trim(),
      //   email: formData.email.trim(),
      //   phoneNumber: formData.phoneNumber.replace(/[-\s]/g, ""),
      //   password: formData.password,
      // };

      // const res = await fetch("/api/customers", {
      //   method: "POST",
      //   headers: { "Content-Type": "application/json" },
      //   body: JSON.stringify(payload),
      // });
      //  setSuccess(true);
      //   setTimeout(() => {
      //     router.push("/signin");
      //   }, 2000);
      // const data = await res.json().catch(() => null);

      // if (res.ok) {
      //   setSuccess(true);
      //   setTimeout(() => {
      //     router.push("/signin");
      //   }, 2000);
      // } else {
      //   setErrors({
      //     general:
      //       data?.message ||
      //       "เกิดข้อผิดพลาดในการสมัครสมาชิก กรุณาลองใหม่อีกครั้ง",
      //   });
      // }
    } catch (err: any) {
      console.error("Signup error:", err);
      setErrors({
        general: "ไม่สามารถเชื่อมต่อกับเซิร์ฟเวอร์ได้ กรุณาตรวจสอบการเชื่อมต่อ",
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <Breadcrumb title={"Customer Signup"} pages={["Signup", "Customer"]} />
      <section className="overflow-hidden py-16 lg:py-20 bg-gray-2">
        <div className="max-w-[1170px] w-full mx-auto px-4 sm:px-8 xl:px-0">
          <Card className="max-w-[680px] lg:max-w-[780px] w-full mx-auto bg-white shadow-1 border-gray-3">
            <CardHeader className="text-center pb-6">
              <div className="inline-flex items-center justify-center w-12 h-12 rounded-full bg-blue/10 text-blue mx-auto mb-3">
                <svg
                  className="w-6 h-6"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
                  />
                </svg>
              </div>
              <CardTitle className="text-2xl font-bold text-dark">
                สมัครสมาชิกสำหรับลูกค้า
              </CardTitle>
              <CardDescription className="text-body text-sm mt-1">
                กรอกข้อมูลด้านล่างเพื่อเริ่มช้อปปิ้งกับ PandaStore
              </CardDescription>
            </CardHeader>

            <CardContent className="pt-4 lg:max-w-[500px] lg:justify-center lg:flex lg:mx-auto w-full">
              {success ? (
                <div className="text-center py-8">
                  <div className="w-16 h-16 bg-green/10 text-green rounded-full flex items-center justify-center mx-auto mb-4">
                    <svg
                      className="w-8 h-8"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M5 13l4 4L19 7"
                      />
                    </svg>
                  </div>
                  <h3 className="text-xl font-bold text-dark mb-2">
                    สร้างบัญชีลูกค้าสำเร็จ!
                  </h3>
                  <p className="text-body text-sm">
                    กำลังนำท่านไปยังหน้าเข้าสู่ระบบ...
                  </p>
                </div>
              ) : (
                <CustomerForm
                  formData={formData}
                  errors={errors}
                  loading={loading}
                  onChange={handleChange}
                  onSubmit={handleSubmit}
                />
              )}
            </CardContent>

            <CardFooter className="flex flex-col items-center gap-2 pt-2 border-t border-gray-3 mt-4">
              <p className="text-sm text-body text-center">
                มีบัญชีอยู่แล้ว?
                <Link
                  href="/signin"
                  className="text-dark font-medium ease-out duration-200 hover:text-blue pl-2 underline underline-offset-4"
                >
                  เข้าสู่ระบบ
                </Link>
              </p>
              <Link
                href="/signup"
                className="text-xs text-dark-4 hover:text-dark mt-1 flex items-center gap-1"
              >
                ⬅ เปลี่ยนบทบาท (เลือกประเภทบัญชีอื่น)
              </Link>
            </CardFooter>
          </Card>
        </div>
      </section>
    </>
  );
};

export default CustomerSignup;
