import { Skeleton } from "../ui/skeleton";

export default function Loading() {
  return (
    <main className="max-w-[1170px] mx-auto px-4 sm:px-8 xl:px-0">
      {/* Header skeleton */}
      <div className="flex items-center justify-between py-6">
        <Skeleton className="h-10 w-40" />
        <Skeleton className="h-10 w-64 hidden md:block" />
        <div className="flex items-center gap-4">
          <Skeleton className="h-8 w-8 rounded-full" />
          <Skeleton className="h-8 w-8 rounded-full" />
          <Skeleton className="h-8 w-8 rounded-full" />
        </div>
      </div>

      {/* Nav skeleton */}
      <div className="flex items-center gap-6 py-4 border-b">
        {Array.from({ length: 5 }).map((_, i) => (
          <Skeleton key={i} className="h-4 w-16" />
        ))}
      </div>

      {/* Product grid skeleton */}
      <div className="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-4 gap-6 py-10">
        {Array.from({ length: 8 }).map((_, i) => (
          <div key={i} className="flex flex-col gap-3">
            <Skeleton className="aspect-square w-full rounded-md" />
            <Skeleton className="h-4 w-3/4" />
            <Skeleton className="h-4 w-1/2" />
          </div>
        ))}
      </div>
    </main>
  )
}