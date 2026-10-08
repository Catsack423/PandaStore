import { Skeleton } from "@/components/ui/skeleton";
import { HOME_PRODUCT_LIMIT } from "../constants";

export default function NewArrivalsSkeleton() {
  return (
    <div role="status" aria-label="Loading new arrivals" aria-busy="true">
      <div aria-hidden="true" className="grid grid-cols-1 gap-x-7.5 gap-y-9 sm:grid-cols-2 xl:grid-cols-4">
        {Array.from({ length: HOME_PRODUCT_LIMIT }, (_, index) => (
          <div key={index}>
            <Skeleton className="mb-4 aspect-square w-full rounded-lg bg-gray-3 motion-reduce:animate-none" />
            <Skeleton className="mb-2 h-5 w-28 bg-gray-3 motion-reduce:animate-none" />
            <Skeleton className="mb-1.5 h-6 w-3/4 bg-gray-3 motion-reduce:animate-none" />
            <Skeleton className="h-7 w-20 bg-gray-3 motion-reduce:animate-none" />
            <Skeleton className="mt-2 h-4 w-24 bg-gray-3 motion-reduce:animate-none" />
            <Skeleton className="mt-2 h-5 w-32 bg-gray-3 motion-reduce:animate-none" />
          </div>
        ))}
      </div>
    </div>
  );
}
