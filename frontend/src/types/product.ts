
import { z } from "zod"
export const productSchema = z.object({
  title: z.string(),
  reviews: z.number(),
  price: z.number(),
  discountedPrice: z.number(),
  id: z.number(),
  imgs: z
    .object({
      thumbnails: z.array(z.string()),
      previews: z.array(z.string())
    })
    .optional()
})

export type Product = z.infer<typeof productSchema>;

