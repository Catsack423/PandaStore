"use client";

import Link from "next/link";
import React from "react";
import { useAuth } from "@/app/context/AuthContext";

const Breadcrumb = ({ title, pages, rootLabel = "Home", rootHref }: {
  title: string;
  pages: string[];
  rootLabel?: string;
  rootHref?: string;
}) => {
  const { user } = useAuth();
  const homeHref = rootHref ?? (user?.role === "SELLER" && user.status === "ACTIVE" ? "/seller-dashboard" : "/");
  return (
    <div className="overflow-hidden shadow-breadcrumb pt-[209px] sm:pt-[155px] xl:pt-[165px]">
      <div className="border-t border-gray-3">
        <div className="max-w-[1170px] w-full mx-auto px-4 sm:px-8 xl:px-0 py-5 xl:py-10">
          <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
            <h1 className="font-semibold text-dark text-xl sm:text-2xl xl:text-custom-2">
              {title}
            </h1>

            <ul aria-label="Breadcrumb" className="flex items-center gap-2">
              <li className="text-custom-sm hover:text-blue">
                <Link href={homeHref}>{rootLabel} /</Link>
              </li>

              {pages.length > 0 &&
                pages.map((page, key) => (
                  <li className="text-custom-sm last:text-blue capitalize" key={key}>
                    {page} 
                  </li>
                ))}
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Breadcrumb;
