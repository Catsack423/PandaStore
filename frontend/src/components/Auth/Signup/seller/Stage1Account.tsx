import React from "react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { SellerFormData, FormErrors } from "./types";

interface Stage1AccountProps {
  formData: SellerFormData;
  errors: FormErrors;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
}

export const Stage1Account: React.FC<Stage1AccountProps> = ({
  formData,
  errors,
  onChange,
}) => {
  return (
    <div className="space-y-4">
      <div className="border-b border-gray-3 pb-2.5 mb-4">
        <h4 className="font-semibold text-base text-dark">
          Stage 1: ข้อมูลบัญชีผู้ใช้ (Account Information)
        </h4>
        <p className="text-xs text-body">สำหรับเข้าสู่ระบบจัดการร้านค้า</p>
      </div>

      {/* Username */}
      <div className="space-y-1.5">
        <Label htmlFor="username" className="text-dark font-medium">
          ชื่อผู้ใช้ (Username) <span className="text-red">*</span>
        </Label>
        <Input
          id="username"
          name="username"
          placeholder="เช่น seller_panda (3-50 ตัวอักษร)"
          value={formData.username}
          onChange={onChange}
          className={errors.username ? "border-red" : ""}
        />
        {errors.username && (
          <p className="text-xs text-red mt-1">{errors.username}</p>
        )}
      </div>

      {/* Email */}
      <div className="space-y-1.5">
        <Label htmlFor="email" className="text-dark font-medium">
          อีเมลผู้ขาย (Email) <span className="text-red">*</span>
        </Label>
        <Input
          id="email"
          name="email"
          type="email"
          placeholder="seller@mail.com"
          value={formData.email}
          onChange={onChange}
          className={errors.email ? "border-red" : ""}
        />
        {errors.email && (
          <p className="text-xs text-red mt-1">{errors.email}</p>
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
            placeholder="อย่างน้อย 6 ตัวอักษร"
            value={formData.password}
            onChange={onChange}
            className={errors.password ? "border-red" : ""}
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
            placeholder="กรอกรหัสผ่านอีกครั้ง"
            value={formData.confirmPassword}
            onChange={onChange}
            className={errors.confirmPassword ? "border-red" : ""}
          />
          {errors.confirmPassword && (
            <p className="text-xs text-red mt-1">{errors.confirmPassword}</p>
          )}
        </div>
      </div>
    </div>
  );
};
