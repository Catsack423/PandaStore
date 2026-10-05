"use client";

import Breadcrumb from "@/components/Common/Breadcrumb";
import Link from "next/link";
import React, { useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

import { SellerFormData, FormErrors } from "./types";
import { StageIndicator } from "./StageIndicator";
import { Stage1Account } from "./Stage1Account";
import { Stage2Shop } from "./Stage2Shop";
import { Stage3IdentityBank } from "./Stage3IdentityBank";
import { useSellerPreview } from "@/app/context/SellerPreviewContext";
import { DemoNotice } from "@/components/Seller/Shared";

const initialFormData: SellerFormData = {
  // Stage 1
  username: "",
  email: "",
  password: "",
  confirmPassword: "",

  // Stage 2
  shopName: "",
  shopDescription: "",
  shopPhone: "",
  shopEmail: "",
  shopAddress: "",

  // Stage 3
  sellerFirstName: "",
  sellerLastName: "",
  idCardNumber: "",
  bankName: "",
  bankAccountName: "",
  bankAccountNumber: "",
};

const SellerSignup = () => {
  const router = useRouter();
  const { application, createApplication } = useSellerPreview();
  const isReapplying = application?.status === "REJECTED";
  const [currentStage, setCurrentStage] = useState(1);
  const [formData, setFormData] = useState<SellerFormData>(() => application?.status === "REJECTED" ? {
    ...initialFormData,
    shopName: application.shopName,
    shopDescription: application.shopDescription,
    shopPhone: application.shopPhone,
    shopEmail: application.shopEmail,
    shopAddress: application.shopAddress,
  } : initialFormData);
  const [errors, setErrors] = useState<FormErrors>({});

  // Validate Stage 1
  const validateStage1 = (): boolean => {
    const errs: FormErrors = {};

    if (!formData.username.trim()) {
      errs.username = "Please enter your username";
    } else if (formData.username.length < 3 || formData.username.length > 50) {
      errs.username = "Username must be between 3 and 50 characters";
    }

    if (!formData.email.trim()) {
      errs.email = "Please enter your email";
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      errs.email = "Invalid email format";
    }

    if (!formData.password) {
      errs.password = "Please enter your password";
    } else if (formData.password.length < 6) {
      errs.password = "Password must contain at least 6 characters";
    }

    if (!formData.confirmPassword) {
      errs.confirmPassword = "Please confirm your password";
    } else if (formData.password !== formData.confirmPassword) {
      errs.confirmPassword = "Passwords do not match";
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  // Validate Stage 2
  const validateStage2 = (): boolean => {
    const errs: FormErrors = {};

    if (!formData.shopName.trim()) {
      errs.shopName = "Please enter your shop name";
    } else if (formData.shopName.length > 100) {
      errs.shopName = "Shop name must not exceed 100 characters";
    }

    if (!formData.shopPhone.trim()) {
      errs.shopPhone = "Please enter your shop phone number";
    } else if (
      !/^[0-9]{9,15}$/.test(formData.shopPhone.replace(/[-\s]/g, ""))
    ) {
      errs.shopPhone = "Phone number must contain 9-15 digits";
    }

    if (!formData.shopEmail.trim()) {
      errs.shopEmail = "Please enter your shop contact email";
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.shopEmail)) {
      errs.shopEmail = "Invalid shop email format";
    }

    if (!formData.shopAddress.trim()) {
      errs.shopAddress = "Please enter your shop address / shipping location";
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  // Validate Stage 3
  const validateStage3 = (): boolean => {
    const errs: FormErrors = {};

    if (!formData.sellerFirstName.trim()) {
      errs.sellerFirstName = "Please enter the seller's first name";
    }

    if (!formData.sellerLastName.trim()) {
      errs.sellerLastName = "Please enter the seller's last name";
    }

    if (!formData.idCardNumber.trim()) {
      errs.idCardNumber = "Please enter your 13-digit national ID number";
    } else if (
      !/^[0-9]{13}$/.test(formData.idCardNumber.replace(/[-\s]/g, ""))
    ) {
      errs.idCardNumber = "National ID number must contain 13 digits";
    }

    if (!formData.bankName.trim()) {
      errs.bankName = "Please specify your bank name";
    }

    if (!formData.bankAccountName.trim()) {
      errs.bankAccountName = "Please enter your bank account name";
    }

    if (!formData.bankAccountNumber.trim()) {
      errs.bankAccountNumber = "Please enter your bank account number";
    } else if (
      !/^[0-9]{10,15}$/.test(formData.bankAccountNumber.replace(/[-\s]/g, ""))
    ) {
      errs.bankAccountNumber = "Bank account number must contain 10-15 digits";
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>,
  ) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: undefined }));
    }
  };

  const handleNext = (e?: React.MouseEvent) => {
    e?.preventDefault();
    e?.stopPropagation();

    if (currentStage === 1) {
      if (validateStage1()) {
        setErrors({});
        if (!formData.shopEmail) {
          setFormData((prev) => ({ ...prev, shopEmail: prev.email }));
        }
        setCurrentStage(2);
        window.scrollTo({ top: 0, behavior: "smooth" });
      }
    } else if (currentStage === 2) {
      if (validateStage2()) {
        setErrors({});
        setCurrentStage(3);
        window.scrollTo({ top: 0, behavior: "smooth" });
      }
    }
  };

  const handlePrev = (e?: React.MouseEvent) => {
    e?.preventDefault();
    e?.stopPropagation();
    setErrors({});
    setCurrentStage((prev) => Math.max(prev - 1, 1));
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const handleSubmit = (e?: React.FormEvent | React.MouseEvent) => {
    e?.preventDefault();
    e?.stopPropagation();

    if (!validateStage3()) return;

    setErrors({});
    createApplication({
      shopName: formData.shopName.trim(),
      shopDescription: formData.shopDescription.trim(),
      shopPhone: formData.shopPhone.trim(),
      shopEmail: formData.shopEmail.trim(),
      shopAddress: formData.shopAddress.trim(),
    });
    router.push("/seller-application");
  };

  const handleFormKeyDown = (e: React.KeyboardEvent<HTMLFormElement>) => {
    if (e.key === "Enter") {
      e.preventDefault();
      if (currentStage < 3) {
        handleNext();
      } else {
        handleSubmit();
      }
    }
  };

  return (
    <>
      <Breadcrumb title={"Seller Signup"} pages={["Signup", "Seller"]} />
      <section className="overflow-hidden py-14 lg:py-20 bg-gray-2">
        <div className="max-w-[1170px] w-full mx-auto px-4 sm:px-8 xl:px-0">
          <div className="mx-auto max-w-[780px]"><DemoNotice>This signup creates a local application preview only. Use sample details. Your password, identity number and bank details are not submitted or saved.</DemoNotice></div>
          <Card className="max-w-[680px] lg:max-w-[780px] w-full mx-auto bg-white shadow-1 border-gray-3">
            <CardHeader className="text-center pb-4 lg:max-w-[500px] lg:justify-center lg:flex lg:mx-auto w-full">
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
                    d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"
                  />
                </svg>
              </div>
              <CardTitle className="text-2xl font-bold text-dark">
                {isReapplying ? "Revise your seller application" : "Seller registration"}
              </CardTitle>
              <CardDescription className="text-body text-sm mt-1">
                {isReapplying ? "Your shop details are prefilled. Review the rejection reason, correct the information, and complete all three steps to submit again." : "Complete all 3 steps to apply for a shop on PandaStore"}
              </CardDescription>
              {isReapplying && application?.adminNote && <div className="mt-4 w-full rounded-lg border border-yellow/30 bg-yellow-light-4 px-4 py-3 text-left text-sm text-dark"><strong>Previous review note</strong><p className="mt-1">{application.adminNote}</p></div>}

              {/* Stepper Progress Bar */}
              <StageIndicator currentStage={currentStage} />
            </CardHeader>

            <CardContent className="pt-4 lg:max-w-[500px] lg:justify-center lg:flex lg:mx-auto w-full">
              <form
                  method="post"
                  onSubmit={(e) => {
                    e.preventDefault();
                    if (currentStage === 3) {
                      handleSubmit(e);
                    }
                  }}
                  onKeyDown={handleFormKeyDown}
                  className="space-y-4.5 w-full"
                >
                  {errors.general && (
                    <div className="p-3.5 rounded-lg bg-red-light-6 border border-red-light-3 text-red text-sm flex items-center gap-2">
                      <svg
                        className="w-5 h-5 shrink-0"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                        />
                      </svg>
                      <span>{errors.general}</span>
                    </div>
                  )}

                  {/* Stage Views */}
                  {currentStage === 1 && (
                    <Stage1Account
                      formData={formData}
                      errors={errors}
                      onChange={handleChange}
                    />
                  )}

                  {currentStage === 2 && (
                    <Stage2Shop
                      formData={formData}
                      errors={errors}
                      onChange={handleChange}
                    />
                  )}

                  {currentStage === 3 && (
                    <Stage3IdentityBank
                      formData={formData}
                      errors={errors}
                      onChange={handleChange}
                    />
                  )}

                  {/* Stepper Navigation Buttons */}
                  <div className="flex items-center justify-between gap-3 pt-6 border-t border-gray-3 mt-6">
                    {currentStage > 1 ? (
                      <Button
                        key="btn-prev"
                        type="button"
                        variant="outline"
                        onClick={handlePrev}
                        className="h-11 px-5 text-sm font-medium border-gray-3 hover:bg-gray-1"
                      >
                        ⬅ Back
                      </Button>
                    ) : (
                      <div />
                    )}

                    {currentStage < 3 ? (
                      <Button
                        key="btn-next"
                        type="button"
                        onClick={handleNext}
                        className="h-11 px-7 text-sm font-medium bg-dark text-white hover:bg-blue ml-auto"
                      >
                        Next ➔
                      </Button>
                    ) : (
                      <Button
                        key="btn-submit"
                        type="button"
                        onClick={handleSubmit}
                        className="h-11 px-7 text-sm font-medium bg-dark text-white hover:bg-blue ml-auto"
                      >
                        {isReapplying ? "Resubmit demo application" : "Create demo application"}
                      </Button>
                    )}
                  </div>
                </form>
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
                href="/signup"
                className="text-xs text-dark-4 hover:text-dark mt-1 flex items-center gap-1"
              >
                ⬅ Change account type
              </Link>
            </CardFooter>
          </Card>
        </div>
      </section>
    </>
  );
};

export default SellerSignup;
