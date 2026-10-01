import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

const ROLE_PERMISSIONS: Record<string, string[]> = {
  "/admin": ["ADMIN"],
  "/seller/products": ["SELLER"],
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

async function getIdentity(token: string): Promise<{ role: string; userId: number } | null> {
  try {
    const response = await fetch(`${backend}/api/auth/me`, {
      headers: { Authorization: `Bearer ${token}` },
      cache: "no-store",
      signal: AbortSignal.timeout(5000),
    });
    if (!response.ok) return null;
    const body = await response.json();
    if (!body?.success || !["CUSTOMER", "SELLER", "ADMIN"].includes(body.data?.role)) return null;
    return body.data;
  } catch {
    return null;
  }
}

function signInRedirect(request: NextRequest, pathname: string) {
  const loginUrl = new URL("/signin", request.url);
  loginUrl.searchParams.set("callbackUrl", pathname);
  return NextResponse.redirect(loginUrl);
}

export async function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  // Only the order detail preview is public. It uses local demo data and never calls seller APIs.
  if (/^\/seller\/[^/]+$/.test(pathname) && request.nextUrl.searchParams.get("demo") === "1") {
    return NextResponse.next();
  }

  const token = request.cookies.get("auth_token")?.value;
  const accountPath = Object.keys(ACCOUNT_PAGES).find((route) =>
    pathname === route || pathname.startsWith(route + "/")
  );
  if (accountPath) {
    if (!token) return signInRedirect(request, pathname);
    const identity = await getIdentity(token);
    if (!identity) return signInRedirect(request, pathname);
    if (!ACCOUNT_PAGES[accountPath].includes(identity.role)) {
      const destination = identity.role === "SELLER" ? "/seller-dashboard" : identity.role === "ADMIN" ? "/admin" : "/";
      return NextResponse.redirect(new URL(destination, request.url));
    }
    return NextResponse.next();
  }
  const matchedPath = Object.keys(ROLE_PERMISSIONS).find((route) =>
    pathname === route || pathname.startsWith(route + "/")
  );
  if (matchedPath) {
    if (!token) return signInRedirect(request, pathname);
    const identity = await getIdentity(token);
    if (!identity) return signInRedirect(request, pathname);
    if (!ROLE_PERMISSIONS[matchedPath].includes(identity.role)) {
      const destination = identity.role === "ADMIN" ? "/admin" : identity.role === "SELLER" ? "/seller-dashboard" : matchedPath === "/seller-dashboard" ? "/seller-application" : "/";
      return NextResponse.redirect(new URL(destination, request.url));
    }
    return NextResponse.next();
  }

  const isCustomerPage = CUSTOMER_PAGES.some((path) => pathname === path || pathname.startsWith(path + "/"));
  const shopMatch = /^\/shop\/([^/]+)(?:\/.*)?$/.exec(pathname);
  if (!token || (!isCustomerPage && !shopMatch)) return NextResponse.next();

  const identity = await getIdentity(token);
  if (!identity) return NextResponse.next();
  if (identity.role === "ADMIN") return NextResponse.redirect(new URL("/admin", request.url));
  if (identity.role !== "SELLER") return NextResponse.next();

  try {
    if (shopMatch) {
      const shopResponse = await fetch(`${backend}/api/seller/shops/user/${identity.userId}`, {
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
    // A verified seller cannot enter a shop without confirming ownership.
    return NextResponse.redirect(new URL("/seller-dashboard", request.url));
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
