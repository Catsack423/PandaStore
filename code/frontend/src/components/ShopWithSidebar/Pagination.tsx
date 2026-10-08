import { ChevronLeft, ChevronRight } from "lucide-react";

export default function Pagination({ page, totalPages, totalItems, onPageChange }: {
  page: number; totalPages: number; totalItems: number; onPageChange: (page: number) => void;
}) {
  if (totalItems === 0) return null;
  const visiblePages = Array.from(new Set([1, page - 2, page - 1, page, page + 1, page + 2, totalPages]))
    .filter((value) => value >= 1 && value <= totalPages).sort((a, b) => a - b);
  const buttonClass = "flex h-9 min-w-8 items-center justify-center rounded-[3px] px-2 duration-200 hover:bg-blue hover:text-white disabled:cursor-not-allowed disabled:text-gray-4 disabled:hover:bg-transparent";
  return (
    <nav aria-label="Product pages" className="mt-15 flex justify-center">
      <div className="flex items-center gap-1 rounded-md bg-white p-2 text-dark shadow-1">
        <button type="button" aria-label="Previous page" disabled={page === 1}
          onClick={() => onPageChange(page - 1)} className={buttonClass}><ChevronLeft className="size-4" /></button>
        {visiblePages.map((value, index) => (
          <span key={value} className="flex items-center gap-1">
            {index > 0 && value - visiblePages[index - 1] > 1 && <span aria-hidden="true" className="px-2">…</span>}
            <button type="button" aria-label={`Page ${value}`} aria-current={value === page ? "page" : undefined}
              onClick={() => { if (value !== page) onPageChange(value); }}
              className={`${buttonClass} ${value === page ? "bg-blue text-white" : ""}`}>{value}</button>
          </span>
        ))}
        <button type="button" aria-label="Next page" disabled={page === totalPages}
          onClick={() => onPageChange(page + 1)} className={buttonClass}><ChevronRight className="size-4" /></button>
      </div>
    </nav>
  );
}
