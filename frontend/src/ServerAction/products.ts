"use server";

import { ApiResponse } from "@/types/apiresponse";
import { Product, productSchema } from "@/types/product";
import z from "zod";

export async function getProducts(): Promise<ApiResponse<Product[]>> {
  try {
    const res = await fetch("http://localhost:5000/products");
    const result: ApiResponse<{ products: unknown[] }> = await res.json();

    if (!result.success) {
      console.error("API Error:", result.error);
      return {
        success: false,
        message: result.message,
        data: null,
        error: result.error,
      };
    }

    // ดึง products ออกมาจาก object ก่อน parse
    const parsedData = z.array(productSchema).safeParse(result.data.products);

    if (!parsedData.success) {
      console.error("Schema mismatch:", parsedData.error.format());
      return {
        success: false,
        message: "โครงสร้างข้อมูลสินค้าจาก API ไม่ถูกต้อง",
        data: null,
        error: parsedData.error.flatten(),
      };
    }

    // ต้อง return กรณีสำเร็จด้วย!
    return {
      success: true,
      message: "ดึงข้อมูลสินค้าสำเร็จ",
      data: parsedData.data,
      error: null,
    };
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : "เกิดข้อผิดพลาด";
    return {
      data: null,
      error: message,
      message: message,
      success: false,
    };
  }
}
