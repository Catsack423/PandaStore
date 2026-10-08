import Breadcrumb from "@/components/Common/Breadcrumb";
import { Skeleton } from "@/components/ui/skeleton";

export default function Loading() {
  return (
    <main aria-busy="true" aria-label="Loading shop">
      <Breadcrumb title="Shop" pages={["Shop"]} />
      <section className="bg-gray-2 py-12 sm:py-16">
        <div className="mx-auto w-full max-w-[1170px] px-4 sm:px-8 xl:px-0">
          <div aria-hidden="true" className="mb-7 rounded-xl bg-white p-6 shadow-1 sm:p-9">
            <div className="flex items-start gap-5">
              <Skeleton className="h-16 w-16 shrink-0 rounded-xl bg-gray-3 motion-reduce:animate-none" />
              <div className="min-w-0 flex-1 space-y-3">
                <Skeleton className="h-4 w-24 bg-gray-3 motion-reduce:animate-none" />
                <Skeleton className="h-9 w-56 max-w-full bg-gray-3 motion-reduce:animate-none" />
                <Skeleton className="h-5 w-80 max-w-full bg-gray-3 motion-reduce:animate-none" />
              </div>
            </div>
            <div className="mt-7 grid gap-3 border-t border-gray-3 pt-6 sm:grid-cols-3">
              {[0, 1, 2].map(index => (
                <Skeleton key={index} className="h-5 w-48 max-w-full bg-gray-3 motion-reduce:animate-none" />
              ))}
            </div>
          </div>
          <div aria-hidden="true" className="grid grid-cols-1 gap-x-7.5 gap-y-9 rounded-xl bg-white p-6 shadow-1 sm:grid-cols-2 sm:p-8 lg:grid-cols-4">
            {[0, 1, 2, 3].map(index => (
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
      </section>
    </main>
  );
}
