import type { NextRequest } from "next/server";

export function hasAllowedRequestOrigin(request: NextRequest): boolean {
  const origin = request.headers.get("origin");
  if (!origin || origin === "null") return false;

  // Explicit public origins support proxies without trusting forwarded headers.
  // Keep the existing same-origin behavior when no allowlist is configured.
  const configured = process.env.ALLOWED_REQUEST_ORIGINS;
  const allowedOrigins = configured === undefined
    ? [request.nextUrl.origin]
    : configured.split(",").map(value => value.trim()).filter(Boolean);

  return allowedOrigins.includes(origin);
}
