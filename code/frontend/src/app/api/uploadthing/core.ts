import { createUploadthing, type FileRouter } from "uploadthing/next";
import { UploadThingError } from "uploadthing/server";
import { productImageRemovalToken } from "@/lib/productImageRemoval";

const f = createUploadthing();
const backend = process.env.BACKEND_API_URL || "http://localhost:8080";
const allowedTypes = new Set(["image/jpeg", "image/png", "image/webp"]);

export const uploadRouter = {
  productImage: f({
    "image/jpeg": { maxFileSize: "4MB", maxFileCount: 1 },
    "image/png": { maxFileSize: "4MB", maxFileCount: 1 },
    "image/webp": { maxFileSize: "4MB", maxFileCount: 1 },
  }, { awaitServerData: true })
    .middleware(async ({ req, files }) => {
      if (files.length !== 1 || !allowedTypes.has(files[0].type) || files[0].size > 4 * 1024 * 1024)
        throw new UploadThingError("Choose one JPG, PNG, or WebP image up to 4 MB");
      const token = req.cookies.get("auth_token")?.value;
      if (!token) throw new UploadThingError("Please sign in first");
      try {
        const options = { headers: { Authorization: `Bearer ${token}` }, cache: "no-store" as const, signal: AbortSignal.timeout(8000) };
        const response = await fetch(`${backend}/api/auth/me`, options);
        const body = await response.json();
        if (!response.ok || !body?.success || body.data?.role !== "SELLER" || body.data?.status !== "ACTIVE")
          throw new UploadThingError("Active Seller account required");
        const shopResponse = await fetch(`${backend}/api/seller/shops/user/${body.data.userId}`, options);
        const shop = await shopResponse.json();
        if (!shopResponse.ok || !shop?.success || shop.data?.status !== "ACTIVE")
          throw new UploadThingError("Active shop required");
        return { userId: String(body.data.userId) };
      } catch (error) {
        if (error instanceof UploadThingError) throw error;
        throw new UploadThingError("Authentication service is unavailable");
      }
    })
    .onUploadComplete(({ file, metadata }) => ({
      url: file.ufsUrl,
      key: file.key,
      removalToken: productImageRemovalToken(file.key, metadata.userId),
      userId: metadata.userId,
    })),
  sellerDocument: f({
    "image/jpeg": { maxFileSize: "4MB", maxFileCount: 1 },
    "image/png": { maxFileSize: "4MB", maxFileCount: 1 },
    "image/webp": { maxFileSize: "4MB", maxFileCount: 1 },
  })
    .middleware(async ({ req, files }) => {
      if (
        files.length !== 1 ||
        !allowedTypes.has(files[0].type) ||
        files[0].size > 4 * 1024 * 1024
      ) {
        throw new UploadThingError("Choose one JPG, PNG, or WebP image up to 4 MB");
      }

      const token = req.cookies.get("auth_token")?.value;
      if (!token) throw new UploadThingError("Please sign in first");

      let response: Response;
      try {
        response = await fetch(`${backend}/api/auth/me`, {
          headers: { Authorization: `Bearer ${token}` },
          cache: "no-store",
          signal: AbortSignal.timeout(8000),
        });
      } catch {
        throw new UploadThingError("Authentication service is unavailable");
      }

      const body = await response.json().catch(() => null);
      if (!response.ok || !body?.success || body.data?.role !== "CUSTOMER") {
        throw new UploadThingError("Customer account required");
      }
      return { userId: String(body.data.userId) };
    })
    .onUploadComplete(({ file }) => ({ url: file.ufsUrl, key: file.key })),
} satisfies FileRouter;

export type UploadRouter = typeof uploadRouter;
