import React from "react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { SellerFormData, FormErrors } from "./types";

interface Stage3IdentityBankProps {
  formData: SellerFormData;
  errors: FormErrors;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
}

export const Stage3IdentityBank: React.FC<Stage3IdentityBankProps> = ({
  formData,
  errors,
  onChange,
}) => {
  return (
    <div className="space-y-4">
      {/* First Name & Last Name */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div className="space-y-1.5">
          <Label htmlFor="sellerFirstName" className="text-dark font-medium">
            Applicant&apos;s first name <span className="text-red">*</span>
          </Label>
          <Input
            id="sellerFirstName"
            name="sellerFirstName"
            placeholder="First name as shown on your national ID card"
            value={formData.sellerFirstName}
            onChange={onChange}
            className={errors.sellerFirstName ? "border-red" : ""}
          />
          {errors.sellerFirstName && (
            <p className="text-xs text-red mt-1">{errors.sellerFirstName}</p>
          )}
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="sellerLastName" className="text-dark font-medium">
            Last name <span className="text-red">*</span>
          </Label>
          <Input
            id="sellerLastName"
            name="sellerLastName"
            placeholder="Last name as shown on your national ID card"
            value={formData.sellerLastName}
            onChange={onChange}
            className={errors.sellerLastName ? "border-red" : ""}
          />
          {errors.sellerLastName && (
            <p className="text-xs text-red mt-1">{errors.sellerLastName}</p>
          )}
        </div>
      </div>

      {/* ID Card Number */}
      <div className="space-y-1.5">
        <Label htmlFor="idCardNumber" className="text-dark font-medium">
          National ID number (13 digits) <span className="text-red">*</span>
        </Label>
        <Input
          id="idCardNumber"
          name="idCardNumber"
          maxLength={13}
          placeholder="e.g. 1100123456789 (13 digits)"
          value={formData.idCardNumber}
          onChange={onChange}
          className={errors.idCardNumber ? "border-red" : ""}
        />
        {errors.idCardNumber && (
          <p className="text-xs text-red mt-1">{errors.idCardNumber}</p>
        )}
      </div>

      <div className="pt-2 border-t border-gray-3 mt-4">
        <h5 className="font-semibold text-sm text-dark mb-3">
          Bank account for receiving sales payments
        </h5>

        {/* Bank Name */}
        <div className="space-y-1.5 mb-3.5">
          <Label htmlFor="bankName" className="text-dark font-medium">
            Bank name <span className="text-red">*</span>
          </Label>
          <Input
            id="bankName"
            name="bankName"
            placeholder="e.g. Kasikornbank, Siam Commercial Bank"
            value={formData.bankName}
            onChange={onChange}
            className={errors.bankName ? "border-red" : ""}
          />
          {errors.bankName && (
            <p className="text-xs text-red mt-1">{errors.bankName}</p>
          )}
        </div>

        {/* Bank Account Name & Account Number */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="space-y-1.5">
            <Label htmlFor="bankAccountName" className="text-dark font-medium">
              Bank account name <span className="text-red">*</span>
            </Label>
            <Input
              id="bankAccountName"
              name="bankAccountName"
              placeholder="Must match the applicant's name"
              value={formData.bankAccountName}
              onChange={onChange}
              className={errors.bankAccountName ? "border-red" : ""}
            />
            {errors.bankAccountName && (
              <p className="text-xs text-red mt-1">{errors.bankAccountName}</p>
            )}
          </div>

          <div className="space-y-1.5">
            <Label
              htmlFor="bankAccountNumber"
              className="text-dark font-medium"
            >
              Bank account number <span className="text-red">*</span>
            </Label>
            <Input
              id="bankAccountNumber"
              name="bankAccountNumber"
              placeholder="e.g. 1234567890"
              value={formData.bankAccountNumber}
              onChange={onChange}
              className={errors.bankAccountNumber ? "border-red" : ""}
            />
            {errors.bankAccountNumber && (
              <p className="text-xs text-red mt-1">
                {errors.bankAccountNumber}
              </p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
