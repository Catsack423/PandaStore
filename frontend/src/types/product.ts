import { z } from "zod";

export const productSchema = z.object({
  title: z.string(),
  description: z.string().nullish(),
  averageRating: z.number().nullish(),
  reviews: z.number(),
  price: z.number(),
  discountedPrice: z.number(),
  id: z.number(),
  categoryIds: z.array(z.number().int()).optional(),
  createdAt: z.string().nullish(),
  stock: z.number().int().nonnegative().nullish(),
  status: z.string().nullish(),
  sellerId: z.number().int().nullish(),
  sellerShopName: z.string().nullish(),
  imgs: z
    .object({
      thumbnails: z.array(z.string()),
      previews: z.array(z.string()),
    })
    .optional(),
});

export type Product = z.infer<typeof productSchema>;
