import { genUploader } from "uploadthing/client";
import type { UploadRouter } from "@/app/api/uploadthing/core";

export const MAX_IMAGE_SIZE = 4 * 1024 * 1024;
export const ALLOWED_IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"] as const;

const { uploadFiles } = genUploader<UploadRouter>();

export async function uploadSingleImage(
  file: File,
  endpoint: keyof UploadRouter,
): Promise<{ url: string; key: string }> {
  if (!ALLOWED_IMAGE_TYPES.includes(file.type as (typeof ALLOWED_IMAGE_TYPES)[number])) {
    throw new Error("Choose a JPG, PNG, or WebP image");
  }
  if (file.size > MAX_IMAGE_SIZE) {
    throw new Error("Image must be 4 MB or smaller");
  }

  const uploaded = await uploadFiles(endpoint, { files: [file] });
  const result = uploaded[0];
  if (!result?.ufsUrl || !result.key) {
    throw new Error("Image upload did not return a file URL. Please try again.");
  }
  return { url: result.ufsUrl, key: result.key };
}
