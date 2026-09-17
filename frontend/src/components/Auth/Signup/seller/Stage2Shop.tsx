import React from "react";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { SellerFormData, FormErrors } from "./types";

interface Stage2ShopProps {
  formData: SellerFormData;
  errors: FormErrors;
  onChange: (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>,
  ) => void;
}

export const Stage2Shop: React.FC<Stage2ShopProps> = ({
  formData,
  errors,
  onChange,
}) => {
  return (
    <div className="space-y-4">
      <div className="border-b border-gray-3 pb-2.5 mb-4">
        <h4 className="font-semibold text-base text-dark">
          Stage 2: ข้อมูลร้านค้า (Shop Information)
        </h4>
        <p className="text-xs text-body">
          ข้อมูลร้านค้าที่จะแสดงให้ลูกค้าเห็นบนแพลตฟอร์ม
        </p>
      </div>

      {/* Shop Name */}
      <div className="space-y-1.5">
        <Label htmlFor="shopName" className="text-dark font-medium">
          ชื่อร้านค้า (Shop Name) <span className="text-red">*</span>
        </Label>
        <Input
          id="shopName"
          name="shopName"
          placeholder="เช่น Panda Official Store"
          value={formData.shopName}
          onChange={onChange}
          className={errors.shopName ? "border-red" : ""}
        />
        {errors.shopName && (
          <p className="text-xs text-red mt-1">{errors.shopName}</p>
        )}
      </div>

      {/* Shop Description */}
      <div className="space-y-1.5">
        <Label htmlFor="shopDescription" className="text-dark font-medium">
          คำอธิบายร้านค้า (Shop Description)
        </Label>
        <Textarea
          id="shopDescription"
          name="shopDescription"
          placeholder="อธิบายเกี่ยวกับร้านค้าและประเภทสินค้าที่จำหน่าย"
          value={formData.shopDescription}
          onChange={onChange}
          className={errors.shopDescription ? "border-red" : ""}
        />
        {errors.shopDescription && (
          <p className="text-xs text-red mt-1">{errors.shopDescription}</p>
        )}
      </div>

      {/* Shop Phone & Shop Email */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div className="space-y-1.5">
          <Label htmlFor="shopPhone" className="text-dark font-medium">
            เบอร์โทรร้านค้า <span className="text-red">*</span>
          </Label>
          <Input
            id="shopPhone"
            name="shopPhone"
            type="tel"
            placeholder="เช่น 021234567"
            value={formData.shopPhone}
            onChange={onChange}
            className={errors.shopPhone ? "border-red" : ""}
          />
          {errors.shopPhone && (
            <p className="text-xs text-red mt-1">{errors.shopPhone}</p>
          )}
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="shopEmail" className="text-dark font-medium">
            อีเมลติดต่อร้านค้า <span className="text-red">*</span>
          </Label>
          <Input
            id="shopEmail"
            name="shopEmail"
            type="email"
            placeholder="contact@shoppanda.com"
            value={formData.shopEmail}
            onChange={onChange}
            className={errors.shopEmail ? "border-red" : ""}
          />
          {errors.shopEmail && (
            <p className="text-xs text-red mt-1">{errors.shopEmail}</p>
          )}
        </div>
      </div>

      {/* Shop Address */}
      <div className="space-y-1.5">
        <Label htmlFor="shopAddress" className="text-dark font-medium">
          ที่อยู่ร้านค้า / สถานที่ส่งสินค้า <span className="text-red">*</span>
        </Label>
        <Textarea
          id="shopAddress"
          name="shopAddress"
          placeholder="เลขที่ อาคาร ถนน แขวง/ตำบล เขต/อำเภอ จังหวัด รหัสไปรษณีย์"
          value={formData.shopAddress}
          onChange={onChange}
          className={errors.shopAddress ? "border-red" : ""}
        />
        {errors.shopAddress && (
          <p className="text-xs text-red mt-1">{errors.shopAddress}</p>
        )}
      </div>
    </div>
  );
};
