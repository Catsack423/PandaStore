"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";

type ProductImageProps = {
  src?: string | null;
  alt: string;
  size: "sm" | "md" | "lg" | "fill";
  fit?: "contain" | "cover";
  surface?: "white" | "muted" | "soft" | "gray3" | "detail" | "transparent";
  className?: string;
  imageClassName?: string;
  sizes?: string;
  showSkeleton?: boolean;
};

const fallback = "/images/products/product-placeholder.svg";
const dimensions = {
  sm: "h-20 w-20",
  md: "h-20 w-20 sm:h-24 sm:w-24",
  lg: "w-56",
  fill: "w-full",
};
const surfaces = {
  white: "bg-white",
  muted: "bg-gray-1",
  soft: "bg-gray-2",
  gray3: "bg-gray-3",
  detail: "bg-[#F6F7FB]",
  transparent: "",
};
const imageSizes = {
  sm: "80px",
  md: "(min-width: 640px) 96px, 80px",
  lg: "224px",
  fill: "(min-width: 1280px) 280px, (min-width: 640px) 50vw, 100vw",
};

export default function ProductImage({ src, alt, size, fit = "contain", surface = "white",
  className = "", imageClassName = "", sizes, showSkeleton = false }: ProductImageProps) {
  const [failedSource, setFailedSource] = useState<string | null>(null);
  const [loadedSource, setLoadedSource] = useState<string | null>(null);
  useEffect(() => setFailedSource(null), [src]);
  const imageSrc = !src || failedSource === src ? fallback : src;
  const loading = showSkeleton && loadedSource !== imageSrc;

  return <div aria-busy={showSkeleton ? loading : undefined} className={`relative aspect-square shrink-0 overflow-hidden rounded-lg ${dimensions[size]} ${surfaces[surface]} ${className}`}>
    <Image key={imageSrc} src={imageSrc} alt={alt} fill sizes={sizes || imageSizes[size]}
      unoptimized className={`${fit === "cover" ? "object-cover" : "object-contain"} ${imageClassName} ${showSkeleton ? `transition-opacity duration-200 motion-reduce:transition-none ${loading ? "opacity-0" : "opacity-100"}` : ""}`}
      onLoad={() => setLoadedSource(imageSrc)}
      onError={() => { setFailedSource(src || null); if (imageSrc === fallback) setLoadedSource(fallback); }} />
    {loading && <Skeleton aria-hidden="true" className="absolute inset-0 rounded-[inherit] bg-gray-3 motion-reduce:animate-none" />}
  </div>;
}

export function ProductImageGallery({ images, activeImageId, onActiveImageChange, alt, sizes = "(max-width: 1024px) 100vw, 530px", compact = false }: {
  images: { id: string; src: string }[];
  activeImageId: string;
  onActiveImageChange: (id: string) => void;
  alt: string;
  sizes?: string;
  compact?: boolean;
}) {
  if (!images.length) return null;
  const activeIndex = Math.max(0, images.findIndex(image => image.id === activeImageId));

  return <div className={`mx-auto w-full min-w-0 ${compact ? "" : "max-w-[530px]"}`}>
    <div className="relative">
      <ProductImage src={images[activeIndex].src} alt={alt} size="fill" fit="contain" surface="detail" sizes={sizes} className={compact ? "!aspect-video rounded-xl" : "max-h-[530px] rounded-xl"} imageClassName={compact ? "p-0" : "p-6 sm:p-10"} showSkeleton />
      {compact && activeIndex === 0 && <span className="absolute left-3 top-3 rounded-md bg-blue px-2.5 py-1 text-xs font-medium text-white">Main</span>}
      {images.length > 1 && <span className={`absolute rounded-full bg-white py-1 text-xs font-medium text-dark shadow-1 ${compact ? "bottom-2 right-2 px-2" : "bottom-4 right-4 px-3"}`} aria-live="polite">{activeIndex + 1} / {images.length}</span>}
    </div>
    {images.length > 1 && <div className={`flex overflow-x-auto ${compact ? "mt-3 gap-3 p-1" : "mt-4 gap-3 pb-2"}`} role="group" aria-label="Choose product image">
      {images.map((image, index) => <Button key={image.id} type="button" variant="outline" onClick={() => onActiveImageChange(image.id)} aria-label={`View image ${index + 1} of ${images.length}${compact && index === 0 ? ", Main image" : ""}`} aria-pressed={activeIndex === index} className={`relative shrink-0 overflow-hidden rounded-lg border-2 bg-[#F6F7FB] p-0 ${compact ? "h-[72px] w-[72px] sm:h-20 sm:w-20" : "h-20 w-20 sm:h-24 sm:w-24"} ${activeIndex === index ? `border-blue ${compact ? "ring-2 ring-blue ring-offset-2" : ""}` : "border-gray-3 hover:border-blue"}`}>
        <ProductImage src={image.src} alt="" size="fill" surface="detail" sizes={compact ? "(min-width: 640px) 80px, 72px" : "(min-width: 640px) 96px, 80px"} imageClassName={compact ? "p-0" : "p-2"} showSkeleton />
        {compact && index === 0 && <span className="absolute bottom-1 left-1 rounded bg-blue px-1.5 py-0.5 text-[10px] font-medium leading-3 text-white">Main</span>}
      </Button>)}
    </div>}
  </div>;
}
