// src/middleware.ts
import { NextResponse } from "next/server";
import type { NextRequest } from "next/request";

// กำหนดสิทธิ์ของแต่ละ Path
const ROLE_PERMISSIONS: Record<string, string[]> = {
  "/admin": ["ADMIN"],
  "/seller": ["SELLER", "ADMIN"],
  "/account": ["CUSTOMER", "SELLER", "ADMIN"],
};

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  
  // 1. ดึง Token และ Role จาก Cookie
  const token = request.cookies.get("auth_token")?.value;
  const userRole = request.cookies.get("user_role")?.value;

  // 2. ตรวจสอบว่าหน้านี้ต้องการสิทธิ์หรือไม่
  const matchedPath = Object.keys(ROLE_PERMISSIONS).find((route) =>
    pathname.startsWith(route)
  );

  if (matchedPath) {
    // 2.1 ถ้ายังไม่ได้ล็อกอิน ให้เตะกลับไปหน้า Login ทันที
    if (!token) {
      const loginUrl = new URL("/signin", request.url);
      loginUrl.searchParams.set("callbackUrl", pathname);
      return NextResponse.redirect(loginUrl);
    }

    // 2.2 ถ้าล็อกอินแล้ว แต่ Role ไม่ตรงกับสิทธิ์ที่อนุญาต
    const allowedRoles = ROLE_PERMISSIONS[matchedPath];
    if (userRole && !allowedRoles.includes(userRole)) {
      // ไม่มีสิทธิ์ -> ส่งไปหน้า 403 Forbidden หรือหน้าแรก
      return NextResponse.redirect(new URL("/", request.url));
    }
  }

  return NextResponse.next();
}

// กำหนด Path ที่ต้องการให้ Middleware ทำงาน
export const config = {
  matcher: ["/admin/:path*", "/seller/:path*", "/account/:path*"],
};