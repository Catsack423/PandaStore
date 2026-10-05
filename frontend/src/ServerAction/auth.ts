// app/actions/auth.ts (Server Action)
"use server";

import { cookies } from "next/headers";

export async function loginAction(formData: FormData) {
  const email = formData.get("email");
  const password = formData.get("password");

  // 1. เรียก API Backend ของคุณ
  const res = await fetch("https://api.yourbackend.com/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });

  if (!res.ok) {
    return { error: "เข้าสู่ระบบไม่สำเร็จ" };
  }

  const { token, user } = await res.json(); // user: { id, name, role: 'ADMIN' | 'SELLER' | 'CUSTOMER' }

  // 2. เซ็ต Token ลงใน HTTP-Only Cookie
  const cookieStore = await cookies();
  cookieStore.set("auth_token", token, {
    httpOnly: true, // ปลอดภัย ป้องกัน JS ฝั่ง Client แอบอ่าน
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: 60 * 60 * 24 * 7, // อายุ 7 วัน
  });

  // (ทางเลือก) เก็บ Role แยกไว้ใน Cookie ทั่วไปเพื่อให้ Middleware ดึงไปเช็คง่ายๆ
  cookieStore.set("user_role", user.role, {
    path: "/",
    maxAge: 60 * 60 * 24 * 7,
  });

  return { success: true, role: user.role };
}