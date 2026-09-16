"use server";

import { ApiResponse } from "@/types/apiresponse";
import { Product } from "@/types/product";
import z from "zod";

export const productSchema = z.object({
  title: z.string(),
  reviews: z.number(),
  price: z.number(),
  discountedPrice: z.number(),
  id: z.number(),
  imgs: z
    .object({
      thumbnails: z.array(z.string()),
      previews: z.array(z.string()),
    })
    .optional(),
});

export async function getProducts(): Promise<ApiResponse<Product[]>> {
  try {
    // ตัวอย่าง: ยิง API ภายนอก หรือ ดึงข้อมูล
    const res = await fetch("http://localhost:8080/api/products");
    const result: ApiResponse<Product[]> = await res.json();

    if (!result.success) {
      console.error("API Error:", result.error);
      return result;
    }

    const parsedData = z.array(productSchema).safeParse(result.data);

    if (!parsedData.success) {
      // ดักจับกรณีโครงสร้าง Product ไม่ตรงตามที่กำหนด
      console.error("Schema mismatch:", parsedData.error.format());
      return {
        success: false,
        message: "โครงสร้างข้อมูลสินค้าจาก API ไม่ถูกต้อง",
        data: null,
        error: parsedData.error.flatten(),
      };
    }
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : "เกิดข้อผิดพลาด";
    return {
      data: null,
      error: true,
      message: message,
      success: false,
    };
  }
}
