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
      

      {/* Username */}
      <div className="space-y-1.5">
        <Label htmlFor="username" className="text-dark font-medium">
          Username <span className="text-red">*</span>
        </Label>
        <Input
          id="username"
          name="username"
          placeholder="e.g. seller_panda (3-50 characters)"
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
          Seller email <span className="text-red">*</span>
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
            Password <span className="text-red">*</span>
          </Label>
          <Input
            id="password"
            name="password"
            type="password"
            placeholder="At least 6 characters"
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
            Confirm password <span className="text-red">*</span>
          </Label>
          <Input
            id="confirmPassword"
            name="confirmPassword"
            type="password"
            placeholder="Enter your password again"
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
