import { ChevronLeft, ChevronRight } from "lucide-react";

export default function Pagination({ page, totalPages, onPageChange }: {
  page: number; totalPages: number; onPageChange: (page: number) => void;
}) {
  if (totalPages <= 1) return null;
  const visiblePages = Array.from(new Set([1, page - 1, page, page + 1, totalPages]))
    .filter((value) => value >= 1 && value <= totalPages).sort((a, b) => a - b);
  const buttonClass = "flex h-9 min-w-9 items-center justify-center rounded px-2 hover:bg-blue hover:text-white disabled:opacity-40 disabled:hover:bg-transparent disabled:hover:text-dark";
  return (
    <nav aria-label="Product pages" className="mt-12 flex justify-center">
      <div className="flex items-center gap-1 rounded-lg bg-white p-2 text-dark shadow-1">
        <button type="button" aria-label="Previous page" disabled={page === 1}
          onClick={() => onPageChange(page - 1)} className={buttonClass}><ChevronLeft className="size-4" /></button>
        {visiblePages.map((value, index) => (
          <span key={value} className="flex items-center gap-1">
            {index > 0 && value - visiblePages[index - 1] > 1 && <span aria-hidden="true" className="px-2">…</span>}
            <button type="button" aria-label={`Page ${value}`} aria-current={value === page ? "page" : undefined}
              onClick={() => onPageChange(value)} className={`${buttonClass} ${value === page ? "bg-blue text-white" : ""}`}>{value}</button>
          </span>
        ))}
        <button type="button" aria-label="Next page" disabled={page === totalPages}
          onClick={() => onPageChange(page + 1)} className={buttonClass}><ChevronRight className="size-4" /></button>
      </div>
    </nav>
  );
}
