import { formatBaht } from "@/lib/currency";
import React from "react";
import ProductImage from "@/components/Common/ProductImage";
import Link from "next/link";
import { productUrl } from "@/lib/productUrl";

const LatestProducts = ({ products }) => {
  return (
    <div className="shadow-1 bg-white rounded-xl mt-7.5">
      <div className="px-4 sm:px-6 py-4.5 border-b border-gray-3">
        <h2 className="font-medium text-lg text-dark">Latest Products</h2>
      </div>

      <div className="p-4 sm:p-6">
        <div className="flex flex-col gap-6">
          {/* <!-- product item --> */}
          {products.slice(0, 3).map((product, key) => (
            <div className="flex items-center gap-6" key={key}>
              <ProductImage src={product.imgs?.thumbnails?.[0]} alt={product.title} size="sm" surface="gray3" />

              <div>
                <h3 className="font-medium text-dark mb-1 ease-out duration-200 hover:text-blue">
                  <Link href={productUrl(product)}> {product.title} </Link>
                </h3>
                <p className="text-custom-sm">Price: {formatBaht(product.price)}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default LatestProducts;
