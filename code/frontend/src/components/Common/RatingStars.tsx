"use client";

import { Star } from "lucide-react";

interface RatingStarsProps {
  rating?: number | null;
  reviews?: number | null;
  size?: number;
  showCount?: boolean;
  className?: string;
}

export default function RatingStars({
  rating = 0,
  reviews = 0,
  size = 15,
  showCount = true,
  className = "",
}: RatingStarsProps) {
  const reviewCount = reviews ?? 0;
  // If 0 reviews, star rating is 0. Otherwise clamp rating between 0 and 5.
  const numericRating = reviewCount > 0 && rating != null ? Math.min(5, Math.max(0, Math.round(rating))) : 0;

  return (
    <div className={`flex items-center gap-2 ${className}`}>
      <div className="flex items-center gap-0.5" aria-label={`${numericRating} out of 5 stars`}>
        {Array.from({ length: 5 }, (_, index) => (
          <Star
            key={index}
            size={size}
            className={index < numericRating ? "fill-[#FFA645] text-[#FFA645]" : "fill-gray-3 text-gray-3"}
          />
        ))}
      </div>
      {showCount && <p className="text-custom-sm text-dark-4">({reviewCount})</p>}
    </div>
  );
}
