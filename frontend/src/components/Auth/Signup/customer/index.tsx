"use client";

import Breadcrumb from "@/components/Common/Breadcrumb";
import Link from "next/link";
import React, { useState, useTransition } from "react";
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
import { useAuth } from "@/app/context/AuthContext";

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
  const { registerCustomer, login } = useAuth();

  const [formData, setFormData] = useState<CustomerFormData>(initialFormData);
  const [errors, setErrors] = useState<FormErrors>({});
  const [loading, startTransition] = useTransition();
  const [success, setSuccess] = useState(false);

  const validate = (): boolean => {
    const newErrors: FormErrors = {};

    // 1. Username
    if (!formData.username.trim()) {
      newErrors.username = "Please enter your username";
    } else if (formData.username.trim().length < 3 || formData.username.trim().length > 50) {
      newErrors.username = "Username must be between 3 and 50 characters";
    }

    // 2. Full Name
    if (!formData.fullName.trim()) {
      newErrors.fullName = "Please enter your full name";
    } else if (formData.fullName.trim().length > 100) {
      newErrors.fullName = "Full name must not exceed 100 characters";
    }

    // 3. Email
    if (!formData.email.trim()) {
      newErrors.email = "Please enter your email";
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email.trim())) {
      newErrors.email = "Invalid email format";
    } else if (formData.email.trim().length > 100) {
      newErrors.email = "Email must not exceed 100 characters";
    }

    // 4. Phone Number (9-15 digits as per backend CreateCustomerRequest)
    if (!formData.phoneNumber.trim()) {
      newErrors.phoneNumber = "Please enter your phone number";
    } else if (
      !/^[0-9]{9,15}$/.test(formData.phoneNumber.replace(/[-\s]/g, ""))
    ) {
      newErrors.phoneNumber = "Phone number must contain 9-15 digits";
    }

    // 5. Password
    if (!formData.password.trim()) {
      newErrors.password = "Please enter your password";
    } else if (formData.password.length < 6) {
      newErrors.password = "Password must contain at least 6 characters";
    } else if (new TextEncoder().encode(formData.password).length > 72) {
      newErrors.password = "Password must not exceed 72 bytes (Thai characters use multiple bytes)";
    }

    // 6. Confirm Password
    if (!formData.confirmPassword) {
      newErrors.confirmPassword = "Please confirm your password";
    } else if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = "Passwords do not match";
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

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (loading || !validate()) return;

    setErrors({});

    startTransition(async () => {
      const registered = await registerCustomer({
        username: formData.username.trim(),
        fullName: formData.fullName.trim(),
        email: formData.email.trim(),
        phoneNumber: formData.phoneNumber.replace(/[-\s]/g, ""),
        password: formData.password,
        confirmPassword: formData.confirmPassword,
      });
      if (registered.success === false) {
        startTransition(() => {
          setErrors({ general: registered.error });
        });
        return;
      }
      const signedIn = await login(formData.email.trim(), formData.password);
      startTransition(() => {
        if (!signedIn.success) {
          setErrors({
            general:
              "Account created. Automatic sign in failed; please sign in to continue.",
          });
          router.push("/signin");
          return;
        }
        setSuccess(true);
        router.replace("/");
        router.refresh();
      });
    });
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
                Create a customer account
              </CardTitle>
              <CardDescription className="text-body text-sm mt-1">
                Enter your details below to start shopping with PandaStore
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
                    Customer account created successfully!
                  </h3>
                  <p className="text-body text-sm">
                    Redirecting you to the home page...
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
                Already have an account?
                <Link
                  href="/signin"
                  className="text-dark font-medium ease-out duration-200 hover:text-blue pl-2 underline underline-offset-4"
                >
                  Log in
                </Link>
              </p>
              <Link
                href="/seller-application"
                className="text-xs text-dark-4 hover:text-dark mt-1 flex items-center gap-1"
              >
                Become a seller after creating your account
              </Link>
            </CardFooter>
          </Card>
        </div>
      </section>
    </>
  );
};

export default CustomerSignup;
