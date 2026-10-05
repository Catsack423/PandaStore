// src/middleware.ts
import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

// à¸à¸³à¸«à¸™à¸”à¸ªà¸´à¸—à¸˜à¸´à¹Œà¸‚à¸­à¸‡à¹à¸•à¹ˆà¸¥à¸° Path
const ROLE_PERMISSIONS: Record<string, string[]> = {
  "/admin": ["ADMIN"],
  "/seller": ["SELLER", "ADMIN"],
  "/account": ["CUSTOMER", "SELLER", "ADMIN"],
};

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  
  // 1. à¸”à¸¶à¸‡ Token à¹à¸¥à¸° Role à¸ˆà¸²à¸ Cookie
  const token = request.cookies.get("auth_token")?.value;
  const userRole = request.cookies.get("user_role")?.value;

  // 2. à¸•à¸£à¸§à¸ˆà¸ªà¸­à¸šà¸§à¹ˆà¸²à¸«à¸™à¹‰à¸²à¸™à¸µà¹‰à¸•à¹‰à¸­à¸‡à¸à¸²à¸£à¸ªà¸´à¸—à¸˜à¸´à¹Œà¸«à¸£à¸·à¸­à¹„à¸¡à¹ˆ
  const matchedPath = Object.keys(ROLE_PERMISSIONS).find((route) =>
    pathname.startsWith(route)
  );

  if (matchedPath) {
    // 2.1 à¸–à¹‰à¸²à¸¢à¸±à¸‡à¹„à¸¡à¹ˆà¹„à¸”à¹‰à¸¥à¹‡à¸­à¸à¸­à¸´à¸™ à¹ƒà¸«à¹‰à¹€à¸•à¸°à¸à¸¥à¸±à¸šà¹„à¸›à¸«à¸™à¹‰à¸² Login à¸—à¸±à¸™à¸—à¸µ
    if (!token) {
      const loginUrl = new URL("/signin", request.url);
      loginUrl.searchParams.set("callbackUrl", pathname);
      return NextResponse.redirect(loginUrl);
    }

    // 2.2 à¸–à¹‰à¸²à¸¥à¹‡à¸­à¸à¸­à¸´à¸™à¹à¸¥à¹‰à¸§ à¹à¸•à¹ˆ Role à¹„à¸¡à¹ˆà¸•à¸£à¸‡à¸à¸±à¸šà¸ªà¸´à¸—à¸˜à¸´à¹Œà¸—à¸µà¹ˆà¸­à¸™à¸¸à¸à¸²à¸•
    const allowedRoles = ROLE_PERMISSIONS[matchedPath];
    if (userRole && !allowedRoles.includes(userRole)) {
      // à¹„à¸¡à¹ˆà¸¡à¸µà¸ªà¸´à¸—à¸˜à¸´à¹Œ -> à¸ªà¹ˆà¸‡à¹„à¸›à¸«à¸™à¹‰à¸² 403 Forbidden à¸«à¸£à¸·à¸­à¸«à¸™à¹‰à¸²à¹à¸£à¸
      return NextResponse.redirect(new URL("/", request.url));
    }
  }

  return NextResponse.next();
}

// à¸à¸³à¸«à¸™à¸” Path à¸—à¸µà¹ˆà¸•à¹‰à¸­à¸‡à¸à¸²à¸£à¹ƒà¸«à¹‰ Middleware à¸—à¸³à¸‡à¸²à¸™
export const config = {
  matcher: ["/admin/:path*", "/seller/:path*", "/account/:path*"],
};