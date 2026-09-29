"use client";

import { useEffect, useState } from "react";
import Image from "next/image";

type ProductImageProps = {
  src?: string | null;
  alt: string;
  size: "sm" | "md" | "lg" | "fill";
  fit?: "contain" | "cover";
  surface?: "white" | "muted" | "soft" | "gray3" | "detail" | "transparent";
  className?: string;
  imageClassName?: string;
  sizes?: string;
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
  className = "", imageClassName = "", sizes }: ProductImageProps) {
  const [failed, setFailed] = useState(false);
  useEffect(() => setFailed(false), [src]);
  const imageSrc = !src || failed ? fallback : src;

  return <div className={`relative aspect-square shrink-0 overflow-hidden rounded-lg ${dimensions[size]} ${surfaces[surface]} ${className}`}>
    <Image key={imageSrc} src={imageSrc} alt={alt} fill sizes={sizes || imageSizes[size]}
      unoptimized className={`${fit === "cover" ? "object-cover" : "object-contain"} ${imageClassName}`}
      onError={() => setFailed(true)} />
  </div>;
}
