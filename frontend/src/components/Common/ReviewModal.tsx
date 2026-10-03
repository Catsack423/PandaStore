"use client";

import React, { useState } from "react";
import { Star, X, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import toast from "react-hot-toast";

export interface ReviewModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  orderItemId: number;
  productName: string;
}

const ratingLabels: Record<number, string> = {
  1: "Poor",
  2: "Fair",
  3: "Good",
  4: "Very Good",
  5: "Excellent",
};

export default function ReviewModal({
  isOpen,
  onClose,
  onSuccess,
  orderItemId,
  productName,
}: ReviewModalProps) {
  const [rating, setRating] = useState(5);
  const [hoverRating, setHoverRating] = useState<number | null>(null);
  const [comment, setComment] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  if (!isOpen) return null;

  const currentRating = hoverRating ?? rating;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (rating < 1 || rating > 5) {
      setError("Please select a rating between 1 and 5 stars");
      return;
    }

    setSubmitting(true);
    setError("");

    try {
      const response = await fetch("/api/reviews", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          orderItemId,
          rating,
          comment: comment.trim(),
        }),
      });

      const data = await response.json().catch(() => null);
      if (!response.ok || !data?.success) {
        throw new Error(data?.message || "Failed to submit review");
      }

      toast.success("Review submitted successfully! Thank you for your feedback.");
      onSuccess();
      onClose();
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Failed to submit review";
      setError(msg);
      toast.error(msg);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="fixed inset-0 z-[99999] flex items-center justify-center bg-black/60 p-4 animate-fade-in backdrop-blur-sm">
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="review-dialog-title"
        className="relative w-full max-w-lg rounded-2xl bg-white p-6 shadow-2xl transition-all sm:p-7"
      >
        <button
          type="button"
          onClick={onClose}
          disabled={submitting}
          aria-label="Close dialog"
          className="absolute right-4 top-4 rounded-lg p-1.5 text-dark-4 hover:bg-gray-1 hover:text-dark transition-colors"
        >
          <X className="size-5" />
        </button>

        <div className="mb-5">
          <h3 id="review-dialog-title" className="text-xl font-semibold text-dark">
            Review Product
          </h3>
          <p className="mt-1 text-sm text-dark-4 line-clamp-2">
            Share your experience with <span className="font-medium text-dark">{productName}</span>
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5">
          {/* Star Rating Section */}
          <div className="rounded-xl bg-gray-1 p-4 text-center">
            <span className="block text-xs font-medium uppercase tracking-wider text-dark-4 mb-2">
              Your Rating
            </span>
            <div className="flex items-center justify-center gap-2">
              {[1, 2, 3, 4, 5].map((star) => (
                <button
                  key={star}
                  type="button"
                  disabled={submitting}
                  onClick={() => setRating(star)}
                  onMouseEnter={() => setHoverRating(star)}
                  onMouseLeave={() => setHoverRating(null)}
                  className="p-1 transition-transform hover:scale-110 focus:outline-none"
                  aria-label={`${star} star`}
                >
                  <Star
                    size={32}
                    className={`transition-colors ${
                      star <= currentRating
                        ? "fill-[#FFA645] text-[#FFA645]"
                        : "fill-gray-3 text-gray-3 hover:text-gray-4"
                    }`}
                  />
                </button>
              ))}
            </div>
            <p className="mt-2 text-sm font-semibold text-dark">
              {ratingLabels[currentRating] || `${currentRating} Stars`}
            </p>
          </div>

          {/* Comment Section */}
          <div>
            <label
              htmlFor="review-comment"
              className="block text-sm font-medium text-dark mb-1.5"
            >
              Your Feedback <span className="text-xs text-dark-4 font-normal">(Optional)</span>
            </label>
            <textarea
              id="review-comment"
              rows={4}
              maxLength={500}
              disabled={submitting}
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              placeholder="What did you like or dislike about this product? How was the quality?"
              className="w-full rounded-lg border border-gray-3 p-3 text-sm text-dark placeholder:text-dark-5 focus:border-blue focus:outline-none focus:ring-1 focus:ring-blue transition-colors resize-none"
            />
            <div className="mt-1 flex justify-end">
              <span className="text-xs text-dark-4">{comment.length} / 500</span>
            </div>
          </div>

          {error && (
            <p role="alert" className="text-sm text-red">
              {error}
            </p>
          )}

          {/* Actions */}
          <div className="flex items-center justify-end gap-3 pt-2">
            <Button
              type="button"
              variant="outline"
              disabled={submitting}
              onClick={onClose}
              className="h-10 px-4"
            >
              Cancel
            </Button>
            <Button
              type="submit"
              disabled={submitting}
              className="h-10 px-5 bg-blue hover:bg-blue-dark text-white font-medium shadow-sm flex items-center gap-2"
            >
              {submitting ? (
                <>
                  <Loader2 className="size-4 animate-spin" />
                  Submitting…
                </>
              ) : (
                "Submit Review"
              )}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
