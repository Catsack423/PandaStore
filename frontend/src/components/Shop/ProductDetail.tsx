"use client";

import { useState } from "react";
import ProductImage from "@/components/Common/ProductImage";
import { MessageSquareText, Minus, Plus, ShoppingCart, Star, Store } from "lucide-react";
import { useCart } from "@/app/context/CartContext";
import { useAuth } from "@/app/context/AuthContext";
import { useProductAvailability } from "@/components/Common/ProductStock";
import Breadcrumb from "@/components/Common/Breadcrumb";
import { Button } from "@/components/ui/button";
import type { Product } from "@/types/product";
import type { ProductReview } from "@/app/(site)/shop/[shopId]/prouduct/page";

const money = (amount: number) => `$${amount.toLocaleString("en-US", { maximumFractionDigits: 2 })}`;

function Rating({ value, label }: { value: number; label: string }) {
  return <span className="inline-flex items-center gap-1" aria-label={label}>
    {Array.from({ length: 5 }, (_, index) =>
      <Star key={index} aria-hidden="true" className={`size-4 ${index < Math.round(value) ? "fill-[#F5A623] text-[#F5A623]" : "fill-gray-3 text-gray-3"}`} />,
    )}
  </span>;
}

export default function ProductDetail({ product, categoryNames, reviews, sample }: {
  product: Product;
  categoryNames: string[];
  reviews: ProductReview[] | null;
  sample: boolean;
}) {
  const images = product.imgs?.previews?.length ? product.imgs.previews : ["/images/products/product-placeholder.svg"];
  const [activeImage, setActiveImage] = useState(0);
  const [quantity, setQuantity] = useState(1);
  const { addItemToCart } = useCart();
  const { user, isLoading } = useAuth();
  const readOnly = isLoading || user?.role === "SELLER";
  const { stock, remaining, canAdd } = useProductAvailability(product, quantity);
  const price = product.discountedPrice ?? product.price;
  const rating = product.averageRating ?? (reviews?.length ? reviews.reduce((total, review) => total + review.rating, 0) / reviews.length : 0);
  const reviewCount = reviews?.length ?? (sample ? 0 : product.reviews);

  return <main>
    <Breadcrumb title="Product Details" pages={["Product Details"]} />
    <div className="bg-gray-2 pb-16 pt-6 sm:pb-20 sm:pt-10">
      <div className="mx-auto w-full max-w-[1170px] px-4 sm:px-8 xl:px-0">
        <section className="grid gap-5 rounded-xl bg-white p-4 shadow-1 sm:gap-8 sm:p-8 lg:grid-cols-2 lg:gap-12" aria-labelledby="product-title">
          <div className="mx-auto w-full min-w-0 max-w-[530px]">
            <div className="relative">
              <ProductImage src={images[activeImage]} alt={product.title} size="fill" surface="detail" sizes="(max-width: 1024px) 100vw, 530px" className="max-h-[530px] rounded-xl" imageClassName="p-6 sm:p-10" />
              {images.length > 1 && <span className="absolute bottom-4 right-4 rounded-full bg-white px-3 py-1 text-xs font-medium text-dark shadow-1" aria-live="polite">{activeImage + 1} / {images.length}</span>}
            </div>
            {images.length > 1 && <div className="mt-4 flex gap-3 overflow-x-auto pb-2" role="group" aria-label="Choose product image">
              {images.map((src, index) => <Button key={`${src}-${index}`} type="button" variant="outline" onClick={() => setActiveImage(index)} aria-label={`View image ${index + 1} of ${images.length}`} aria-pressed={activeImage === index} className={`relative h-20 w-20 shrink-0 overflow-hidden rounded-lg border-2 bg-[#F6F7FB] p-0 sm:h-24 sm:w-24 ${activeImage === index ? "border-blue" : "border-gray-3 hover:border-blue"}`}>
                <ProductImage src={src} alt="" size="fill" surface="detail" sizes="(min-width: 640px) 96px, 80px" imageClassName="p-2" />
              </Button>)}
            </div>}
          </div>

          <div className="flex min-w-0 flex-col justify-center py-2">
            {product.sellerShopName && <div className="mb-4 inline-flex items-center gap-2 text-sm font-medium text-blue"><Store className="size-4" aria-hidden="true" />{product.sellerShopName}</div>}
            <h1 id="product-title" className="text-2xl font-semibold leading-tight text-dark sm:text-3xl lg:text-[34px]">{product.title}</h1>
            <div className="mt-4 flex flex-wrap items-center gap-2.5 text-sm text-dark-4">
              {rating > 0 && <><Rating value={rating} label={`${rating.toFixed(1)} out of 5 stars`} /><span className="font-medium text-dark">{rating.toFixed(1)}</span><span aria-hidden="true">·</span></>}
              <a href="#reviews" className="hover:text-blue focus-visible:rounded focus-visible:outline focus-visible:outline-2 focus-visible:outline-blue">{reviewCount} {reviewCount === 1 ? "review" : "reviews"}</a>
            </div>
            <div className="mt-5 flex flex-wrap items-baseline gap-3 border-b border-gray-3 pb-5 sm:mt-7 sm:pb-7">
              <span className="text-3xl font-semibold text-blue sm:text-4xl">{money(price)}</span>
              {product.price > price && <span className="text-lg text-dark-4 line-through">{money(product.price)}</span>}
            </div>
            <p className={`mt-4 text-sm font-medium sm:mt-6 ${stock === 0 || remaining === 0 ? "text-red" : "text-dark-4"}`}>
              {stock == null ? "Stock information is unavailable" : stock === 0 ? "Out of stock" : remaining === 0 ? "All available items are in your cart" : `${remaining} available`}
            </p>
            {readOnly ? user?.role === "SELLER" && <p className="mt-5 rounded-lg border border-blue/20 bg-blue/5 px-4 py-3 text-sm font-medium text-blue">Storefront preview · Read only</p> : <div className="mt-4 flex min-w-0 items-center gap-2 sm:mt-5 sm:gap-3">
              <div className="inline-flex h-11 w-28 shrink-0 items-center justify-between rounded-lg border border-gray-3 sm:h-12 sm:w-36" aria-label="Quantity">
                <Button type="button" variant="ghost" size="icon" onClick={() => setQuantity((value) => Math.max(1, value - 1))} disabled={quantity <= 1} aria-label="Decrease quantity" className="h-10 w-9 text-dark-4 hover:text-blue sm:h-11 sm:w-11"><Minus className="size-4" /></Button>
                <span className="min-w-4 text-center font-medium text-dark" aria-live="polite">{quantity}</span>
                <Button type="button" variant="ghost" size="icon" onClick={() => setQuantity((value) => Math.min(stock ?? value, value + 1))} disabled={stock == null || quantity >= remaining} aria-label="Increase quantity" className="h-10 w-9 text-dark-4 hover:text-blue sm:h-11 sm:w-11"><Plus className="size-4" /></Button>
              </div>
              <Button type="button" disabled={!canAdd} onClick={() => addItemToCart({ ...product, quantity })} className="h-11 min-w-0 flex-1 gap-1.5 bg-blue px-2.5 text-white hover:bg-blue-dark sm:h-12 sm:gap-2 sm:px-6"><ShoppingCart className="size-4 sm:size-5" aria-hidden="true" />Add to cart</Button>
            </div>}
            {sample && <p className="mt-4 text-xs text-dark-4">Sample product. Purchase details are available when the product API is connected.</p>}
          </div>
        </section>

        <section className="mt-7 rounded-xl bg-white p-5 shadow-1 sm:p-8" aria-labelledby="description-title">
          <h2 id="description-title" className="text-xl font-semibold text-dark sm:text-2xl">Product information</h2>
          <div className="mt-6 grid gap-6 border-t border-gray-3 pt-6 md:grid-cols-[minmax(0,2fr)_minmax(200px,1fr)] md:gap-10">
            <div><h3 className="font-medium text-dark">Description</h3><p className="mt-3 whitespace-pre-line leading-7 text-dark-4">{product.description?.trim() || "The seller has not added a description for this product yet."}</p></div>
            <div><h3 className="font-medium text-dark">Categories</h3><div className="mt-3 flex flex-wrap gap-2">{categoryNames.length ? categoryNames.map((name, index) => <span key={`${name}-${index}`} className="rounded-full bg-blue/10 px-3 py-1.5 text-sm font-medium text-blue">{name}</span>) : <p className="text-sm text-dark-4">No category assigned</p>}</div></div>
          </div>
        </section>

        <section id="reviews" className="mt-7 rounded-xl bg-white p-6 shadow-1 sm:p-8" aria-labelledby="reviews-title">
          <div className="flex flex-wrap items-center justify-between gap-4"><div><h2 id="reviews-title" className="text-xl font-semibold text-dark sm:text-2xl">Customer reviews</h2><p className="mt-1 text-sm text-dark-4">Comments from customers who bought this product</p></div><span className="rounded-full bg-gray-2 px-3 py-1 text-sm font-medium text-dark">{reviewCount} {reviewCount === 1 ? "review" : "reviews"}</span></div>
          {reviews?.length ? <div className="mt-6 divide-y divide-gray-3 border-t border-gray-3">{reviews.map((review) => <article key={review.reviewId} className="py-6 first:pt-7 last:pb-0"><div className="flex flex-wrap items-center justify-between gap-3"><div className="flex items-center gap-3"><span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-blue/10 font-semibold text-blue" aria-hidden="true">{review.customerName?.trim()?.[0]?.toUpperCase() || "C"}</span><div><h3 className="font-medium text-dark">{review.customerName?.trim() || "Customer"}</h3>{review.createdAt && <p className="text-xs text-dark-4">{new Date(review.createdAt).toLocaleDateString("en-US", { year: "numeric", month: "short", day: "numeric", timeZone: "UTC" })}</p>}</div></div><Rating value={review.rating} label={`${review.rating} out of 5 stars`} /></div>{review.comment && <p className="mt-4 whitespace-pre-line leading-6 text-dark-4">{review.comment}</p>}{review.replyMessage && <div className="mt-4 rounded-lg bg-gray-2 p-4"><p className="text-sm font-medium text-dark">Seller response</p><p className="mt-1 whitespace-pre-line text-sm leading-6 text-dark-4">{review.replyMessage}</p></div>}</article>)}</div> : <div className="mt-6 flex flex-col items-center justify-center border-t border-gray-3 py-12 text-center"><MessageSquareText className="size-9 text-blue" aria-hidden="true" /><h3 className="mt-3 font-medium text-dark">{reviews === null ? "Reviews are unavailable right now" : "No reviews yet"}</h3><p className="mt-1 max-w-sm text-sm text-dark-4">{reviews === null ? "Please check again later to read customer comments." : "Customer comments will appear here after this product is reviewed."}</p></div>}
        </section>
      </div>
    </div>
  </main>;
}
