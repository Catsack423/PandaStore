import "server-only";
import { createHmac, timingSafeEqual } from "node:crypto";

// Issued only by the verified UploadThing callback, bound to its uploader and file.
export function productImageRemovalToken(key: string, userId: string): string {
  const secret = process.env.UPLOADTHING_TOKEN;
  if (!secret) throw new Error("Image storage is unavailable.");
  return createHmac("sha256", secret)
    .update(JSON.stringify(["product-image-removal", userId, key]))
    .digest("hex");
}

export function verifyProductImageRemoval(key: string, userId: string, receipt: string): boolean {
  if (!/^[a-f0-9]{64}$/.test(receipt)) return false;
  return timingSafeEqual(Buffer.from(receipt, "hex"), Buffer.from(productImageRemovalToken(key, userId), "hex"));
}
