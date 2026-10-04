import React from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { CustomerFormData, FormErrors } from "./types";
import { Spinner } from "@/components/ui/spinner";

interface CustomerFormProps {
  formData: CustomerFormData;
  errors: FormErrors;
  loading: boolean;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  onSubmit: (e: React.FormEvent) => void;
}

export const CustomerForm: React.FC<CustomerFormProps> = ({
  formData,
  errors,
  loading,
  onChange,
  onSubmit,
}) => {
  return (
    <form method="post" onSubmit={onSubmit} className="space-y-4.5 w-full">
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

      {/* Username */}
      <div className="space-y-1.5">
        <Label htmlFor="username" className="text-dark font-medium">
          ชื่อผู้ใช้ (Username) <span className="text-red">*</span>
        </Label>
        <Input
          id="username"
          name="username"
          autoComplete="username"
          maxLength={50}
          placeholder="เช่น panda_shopper (3-50 ตัวอักษร)"
          value={formData.username}
          onChange={onChange}
          className={errors.username ? "border-red focus:ring-red/20" : ""}
        />
        {errors.username && (
          <p className="text-xs text-red mt-1">{errors.username}</p>
        )}
      </div>

      {/* Full Name */}
      <div className="space-y-1.5">
        <Label htmlFor="fullName" className="text-dark font-medium">
          ชื่อ-นามสกุลจริง (Full Name) <span className="text-red">*</span>
        </Label>
        <Input
          id="fullName"
          name="fullName"
          autoComplete="name"
          maxLength={100}
          placeholder="เช่น สมชาย ใจดี"
          value={formData.fullName}
          onChange={onChange}
          className={errors.fullName ? "border-red focus:ring-red/20" : ""}
        />
        {errors.fullName && (
          <p className="text-xs text-red mt-1">{errors.fullName}</p>
        )}
      </div>

      {/* Email */}
      <div className="space-y-1.5">
        <Label htmlFor="email" className="text-dark font-medium">
          อีเมล (Email) <span className="text-red">*</span>
        </Label>
        <Input
          id="email"
          name="email"
          type="email"
          autoComplete="email"
          maxLength={100}
          placeholder="example@mail.com"
          value={formData.email}
          onChange={onChange}
          className={errors.email ? "border-red focus:ring-red/20" : ""}
        />
        {errors.email && (
          <p className="text-xs text-red mt-1">{errors.email}</p>
        )}
      </div>

      {/* Phone Number */}
      <div className="space-y-1.5">
        <Label htmlFor="phoneNumber" className="text-dark font-medium">
          เบอร์โทรศัพท์ (Phone Number) <span className="text-red">*</span>
        </Label>
        <Input
          id="phoneNumber"
          name="phoneNumber"
          type="tel"
          autoComplete="tel"
          inputMode="numeric"
          maxLength={10}
          placeholder="เช่น 0812345678 (10 หลัก)"
          value={formData.phoneNumber}
          onChange={(e) => {
            const digits = e.target.value.replace(/\D/g, "").slice(0, 10);
            onChange({ target: { name: "phoneNumber", value: digits } } as React.ChangeEvent<HTMLInputElement>);
          }}
          className={errors.phoneNumber ? "border-red focus:ring-red/20" : ""}
        />
        {errors.phoneNumber && (
          <p className="text-xs text-red mt-1">{errors.phoneNumber}</p>
        )}
      </div>

      {/* Password & Confirm Password */}
      <div className="grid grid-cols-1 sm:grid-rows-2 gap-4">
        <div className="space-y-1.5">
          <Label htmlFor="password" className="text-dark font-medium">
            รหัสผ่าน <span className="text-red">*</span>
          </Label>
          <Input
            id="password"
            name="password"
            type="password"
            autoComplete="new-password"
            maxLength={72}
            placeholder="อย่างน้อย 6 ตัวอักษร"
            value={formData.password}
            onChange={onChange}
            className={errors.password ? "border-red focus:ring-red/20" : ""}
          />
          {errors.password && (
            <p className="text-xs text-red mt-1">{errors.password}</p>
          )}
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="confirmPassword" className="text-dark font-medium">
            ยืนยันรหัสผ่าน <span className="text-red">*</span>
          </Label>
          <Input
            id="confirmPassword"
            name="confirmPassword"
            type="password"
            autoComplete="new-password"
            maxLength={72}
            placeholder="กรอกรหัสผ่านอีกครั้ง"
            value={formData.confirmPassword}
            onChange={onChange}
            className={
              errors.confirmPassword ? "border-red focus:ring-red/20" : ""
            }
          />
          {errors.confirmPassword && (
            <p className="text-xs text-red mt-1">{errors.confirmPassword}</p>
          )}
        </div>
      </div>

      <Button
        type="submit"
        disabled={loading}
        className="w-full h-11 text-base font-medium rounded-lg bg-dark text-white hover:bg-blue mt-6 transition-all duration-200"
      >
        {loading ? (
          <>
            
            <Spinner data-icon="inline-start" className="size-3" /> กำลังนำสมัคสามาชิก
          </>
        ) : (
          "สมัครสมาชิก"
        )}
      </Button>
    </form>
  );
};
