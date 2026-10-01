import type { ReactNode } from "react";
import Link from "next/link";

export default function Home({ children }: { children: ReactNode }) {
  return (
    <main className="pb-16 pt-[244px] sm:pt-[196px] xl:pt-[232px]">
      <section aria-labelledby="new-arrivals-title" className="mx-auto w-full max-w-[1170px] px-4 sm:px-8 xl:px-0">
        <div className="mb-7 flex items-center justify-between gap-4">
          <h1 id="new-arrivals-title" className="text-xl font-semibold text-dark xl:text-heading-5">
            New Arrivals
          </h1>
          <Link href="/shop-with-sidebar" className="inline-flex shrink-0 rounded-md border border-gray-3 bg-gray-1 px-5 py-2.5 text-custom-sm font-medium text-dark transition-colors hover:border-dark hover:bg-dark hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue focus-visible:ring-offset-2 sm:px-7">
            View All
          </Link>
        </div>
        {children}
      </section>
    </main>
  );
}
