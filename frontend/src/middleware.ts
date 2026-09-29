import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

const ROLE_PERMISSIONS: Record<string, string[]> = {
  "/admin": ["ADMIN"],
  "/seller": ["SELLER", "ADMIN"],
  "/seller-dashboard": ["SELLER"],
  "/account": ["CUSTOMER", "SELLER", "ADMIN"],
};

const ACCOUNT_PAGES: Record<string, string[]> = {
  "/seller-application/apply": ["CUSTOMER"],
  "/seller-application": ["CUSTOMER", "SELLER"],
  "/order-history": ["CUSTOMER"],
  "/my-account": ["CUSTOMER", "SELLER", "ADMIN"],
};

const CUSTOMER_PAGES = [
  "/", "/cart", "/checkout", "/shop-with-sidebar", "/shop-without-sidebar",
  "/shop-details", "/order-history", "/wishlist",
];
const backend = process.env.BACKEND_API_URL || "http://localhost:8080";

export async function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  // Only the order detail preview is public. It uses local demo data and never calls seller APIs.
  if (/^\/seller\/[^/]+$/.test(pathname) && request.nextUrl.searchParams.get("demo") === "1") {
    return NextResponse.next();
  }

  const token = request.cookies.get("auth_token")?.value;
  const userRole = request.cookies.get("user_role")?.value;
  const accountPath = Object.keys(ACCOUNT_PAGES).find((route) =>
    pathname === route || pathname.startsWith(route + "/")
  );
  if (accountPath) {
    if (!token) {
      const loginUrl = new URL("/signin", request.url);
      loginUrl.searchParams.set("callbackUrl", pathname);
      return NextResponse.redirect(loginUrl);
    }
    try {
      const response = await fetch(`${backend}/api/auth/me`, {
        headers: { Authorization: `Bearer ${token}` },
        cache: "no-store",
        signal: AbortSignal.timeout(5000),
      });
      const body = response.ok ? await response.json() : null;
      const role = body?.success ? body.data?.role : null;
      if (!role) {
        const loginUrl = new URL("/signin", request.url);
        loginUrl.searchParams.set("callbackUrl", pathname);
        return NextResponse.redirect(loginUrl);
      }
      if (!ACCOUNT_PAGES[accountPath].includes(role)) {
        return NextResponse.redirect(new URL(role === "SELLER" ? "/seller-dashboard" : "/", request.url));
      }
      return NextResponse.next();
    } catch {
      const loginUrl = new URL("/signin", request.url);
      loginUrl.searchParams.set("callbackUrl", pathname);
      return NextResponse.redirect(loginUrl);
    }
  }
  const matchedPath = Object.keys(ROLE_PERMISSIONS).find((route) =>
    pathname === route || pathname.startsWith(route + "/")
  );
  if (matchedPath) {
    if (!token) {
      const loginUrl = new URL("/signin", request.url);
      loginUrl.searchParams.set("callbackUrl", pathname);
      return NextResponse.redirect(loginUrl);
    }
    if (!userRole || !ROLE_PERMISSIONS[matchedPath].includes(userRole)) {
      return NextResponse.redirect(new URL(matchedPath === "/seller-dashboard" ? "/seller-application" : "/", request.url));
    }
  }

  const isCustomerPage = CUSTOMER_PAGES.some((path) => pathname === path || pathname.startsWith(path + "/"));
  const shopMatch = /^\/shop\/([^/]+)(?:\/.*)?$/.exec(pathname);
  if (!token || (!isCustomerPage && !shopMatch)) return NextResponse.next();

  try {
    const authResponse = await fetch(`${backend}/api/auth/me`, {
      headers: { Authorization: `Bearer ${token}` },
      cache: "no-store",
      signal: AbortSignal.timeout(5000),
    });
    if (!authResponse.ok) return userRole === "SELLER"
      ? NextResponse.redirect(new URL("/seller-dashboard", request.url))
      : NextResponse.next();
    const auth = await authResponse.json();
    if (auth?.data?.role !== "SELLER") return NextResponse.next();

    if (shopMatch) {
      const shopResponse = await fetch(`${backend}/api/seller/shops/user/${auth.data.userId}`, {
        cache: "no-store",
        signal: AbortSignal.timeout(5000),
      });
      if (shopResponse.ok) {
        const shop = await shopResponse.json();
        if (shop?.success && String(shop.data?.sellerId) === shopMatch[1]) return NextResponse.next();
      }
    }
    return NextResponse.redirect(new URL("/seller-dashboard", request.url));
  } catch {
    // Deny known seller shopping routes while the backend cannot confirm shop ownership.
    if (userRole === "SELLER") return NextResponse.redirect(new URL("/seller-dashboard", request.url));
    return NextResponse.next();
  }
}

export const config = {
  matcher: [
    "/", "/admin/:path*", "/seller/:path*", "/seller-dashboard/:path*", "/account/:path*",
    "/cart/:path*", "/checkout/:path*", "/shop-with-sidebar/:path*",
    "/shop-without-sidebar/:path*", "/shop-details/:path*", "/shop/:path*",
    "/order-history/:path*", "/my-account/:path*", "/seller-application/:path*", "/wishlist/:path*",
  ],
};
