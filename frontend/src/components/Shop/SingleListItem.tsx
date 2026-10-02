"use client";
import React, { useState } from "react";

import { Product } from "@/types/product";
import { useModalContext } from "@/app/context/QuickViewModalContext";
import { updateQuickView } from "@/redux/features/quickView-slice";
import ProductStock, { useProductAvailability } from "@/components/Common/ProductStock";
import { useCart } from "@/app/context/CartContext";
import { useAuth } from "@/app/context/AuthContext";
import { updateSellerProduct } from "@/ServerAction/products";
import { useDispatch } from "react-redux";
import { AppDispatch } from "@/redux/store";
import Link from "next/link";
import Image from "next/image";
import ProductImage from "@/components/Common/ProductImage";
import ProductStore from "./ProductStore";
import { productUrl } from "@/lib/productUrl";
import { Pencil } from "lucide-react";

const SingleListItem = ({ item }: { item: Product }) => {
  const { addItemToCart } = useCart();
  const { canAdd } = useProductAvailability(item);
  const { openModal } = useModalContext();
  const { user } = useAuth();

  const [currentProduct, setCurrentProduct] = useState(item);
  const [isEditing, setIsEditing] = useState(false);
  const [editForm, setEditForm] = useState({
    name: item.title || "",
    price: String(item.price || ""),
    stock: item.stock ?? 0,
    status: (item.status === "INACTIVE" ? "INACTIVE" : "ACTIVE") as "ACTIVE" | "INACTIVE",
    description: item.description || "",
  });
  const [isSaving, setIsSaving] = useState(false);
  const [editError, setEditError] = useState("");

  const dispatch = useDispatch<AppDispatch>();

  // update the QuickView state
  const handleQuickViewUpdate = () => {
    dispatch(updateQuickView({ ...currentProduct }));
  };

  // add to cart
  const handleAddToCart = () => {
    addItemToCart({
      ...currentProduct,
      quantity: 1,
    });
  };

  const handleSaveEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);
    setEditError("");
    try {
      const res = await updateSellerProduct({
        productId: currentProduct.id,
        name: editForm.name,
        price: editForm.price,
        stock: editForm.stock,
        status: editForm.status,
      });
      if (!res.success) {
        setEditError(res.message || "Failed to update product");
      } else {
        setCurrentProduct((prev) => ({
          ...prev,
          title: editForm.name,
          price: Number(editForm.price),
          discountedPrice: Number(editForm.price),
          stock: editForm.stock,
          status: editForm.status,
        }));
        setIsEditing(false);
      }
    } catch {
      setEditError("Failed to update product");
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="group rounded-lg bg-white shadow-1">
      <div className="flex flex-col sm:flex-row">
        <div className="shadow-list relative w-full shrink-0 overflow-hidden sm:w-56">
          <Link href={productUrl(currentProduct)} aria-label={`View ${currentProduct.title}`} className="block w-full">
            <ProductImage src={currentProduct.imgs?.previews?.[0]} alt={currentProduct.title} size="fill" imageClassName="p-4" />
          </Link>

          <div className="absolute left-0 bottom-0 translate-y-full w-full flex items-center justify-center gap-2.5 pb-5 ease-linear duration-200 group-hover:translate-y-0">
            <button
              onClick={() => {
                openModal();
                handleQuickViewUpdate();
              }}
              aria-label="button for quick view"
              className="flex items-center justify-center w-9 h-9 rounded-[5px] shadow-1 ease-out duration-200 text-dark bg-white hover:text-blue"
            >
              <svg
                className="fill-current"
                width="16"
                height="16"
                viewBox="0 0 16 16"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
              >
                <path
                  fillRule="evenodd"
                  clipRule="evenodd"
                  d="M8.00016 5.5C6.61945 5.5 5.50016 6.61929 5.50016 8C5.50016 9.38071 6.61945 10.5 8.00016 10.5C9.38087 10.5 10.5002 9.38071 10.5002 8C10.5002 6.61929 9.38087 5.5 8.00016 5.5ZM6.50016 8C6.50016 7.17157 7.17174 6.5 8.00016 6.5C8.82859 6.5 9.50016 7.17157 9.50016 8C9.50016 8.82842 8.82859 9.5 8.00016 9.5C7.17174 9.5 6.50016 8.82842 6.50016 8Z"
                  fill=""
                />
                <path
                  fillRule="evenodd"
                  clipRule="evenodd"
                  d="M8.00016 2.16666C4.99074 2.16666 2.96369 3.96946 1.78721 5.49791L1.76599 5.52546C1.49992 5.87102 1.25487 6.18928 1.08862 6.5656C0.910592 6.96858 0.833496 7.40779 0.833496 8C0.833496 8.5922 0.910592 9.03142 1.08862 9.4344C1.25487 9.81072 1.49992 10.129 1.76599 10.4745L1.78721 10.5021C2.96369 12.0305 4.99074 13.8333 8.00016 13.8333C11.0096 13.8333 13.0366 12.0305 14.2131 10.5021L14.2343 10.4745C14.5004 10.129 14.7455 9.81072 14.9117 9.4344C15.0897 9.03142 15.1668 8.5922 15.1668 8C15.1668 7.40779 15.0897 6.96858 14.9117 6.5656C14.7455 6.18927 14.5004 5.87101 14.2343 5.52545L14.2131 5.49791C13.0366 3.96946 11.0096 2.16666 8.00016 2.16666ZM2.57964 6.10786C3.66592 4.69661 5.43374 3.16666 8.00016 3.16666C10.5666 3.16666 12.3344 4.69661 13.4207 6.10786C13.7131 6.48772 13.8843 6.7147 13.997 6.9697C14.1023 7.20801 14.1668 7.49929 14.1668 8C14.1668 8.50071 14.1023 8.79199 13.997 9.0303C13.8843 9.28529 13.7131 9.51227 13.4207 9.89213C12.3344 11.3034 10.5666 12.8333 8.00016 12.8333C5.43374 12.8333 3.66592 11.3034 2.57964 9.89213C2.28725 9.51227 2.11599 9.28529 2.00334 9.0303C1.89805 8.79199 1.8335 8.50071 1.8335 8C1.8335 7.49929 1.89805 7.20801 2.00334 6.9697C2.11599 6.7147 2.28725 6.48772 2.57964 6.10786Z"
                  fill=""
                />
              </svg>
            </button>

            <button
              disabled={!canAdd}
              onClick={() => handleAddToCart()}
              className="inline-flex font-medium text-custom-sm py-[7px] px-5 rounded-[5px] bg-blue text-white ease-out duration-200 hover:bg-blue-dark disabled:cursor-not-allowed disabled:opacity-50"
            >
              {currentProduct.stock === 0 ? "Out of stock" : "Add to cart"}
            </button>
          </div>
        </div>

        <div className="flex min-w-0 flex-1 flex-col justify-center gap-5 px-4 py-5 sm:flex-row sm:items-center sm:justify-between sm:px-7.5 lg:pl-11 lg:pr-12">
          <div className="min-w-0">
            <h3 className="font-medium text-dark ease-out duration-200 hover:text-blue mb-1.5">
              <Link href={productUrl(currentProduct)}> {currentProduct.title} </Link>
            </h3>

            <span className="flex items-center gap-2 font-medium text-lg">
              <span className="text-dark">${currentProduct.discountedPrice}</span>
              {currentProduct.price > currentProduct.discountedPrice && (
                <span className="text-dark-4 line-through">${currentProduct.price}</span>
              )}
            </span>
            <ProductStock product={currentProduct} />
            <ProductStore product={currentProduct} />
          </div>

          <div className="flex flex-col items-start sm:items-end gap-3">
            <div className="flex items-center gap-2.5 mb-2">
              <div className="flex items-center gap-1">
                <Image src="/images/icons/icon-star.svg" alt="star icon" width={15} height={15} />
                <Image src="/images/icons/icon-star.svg" alt="star icon" width={15} height={15} />
                <Image src="/images/icons/icon-star.svg" alt="star icon" width={15} height={15} />
                <Image src="/images/icons/icon-star.svg" alt="star icon" width={15} height={15} />
                <Image src="/images/icons/icon-star.svg" alt="star icon" width={15} height={15} />
              </div>

              <p className="text-custom-sm">({currentProduct.reviews ?? 0})</p>
            </div>

            {user?.role === "SELLER" && (
              <button
                type="button"
                onClick={() => {
                  setEditForm({
                    name: currentProduct.title || "",
                    price: String(currentProduct.price || ""),
                    stock: currentProduct.stock ?? 0,
                    status: (currentProduct.status === "INACTIVE" ? "INACTIVE" : "ACTIVE") as "ACTIVE" | "INACTIVE",
                    description: currentProduct.description || "",
                  });
                  setIsEditing(true);
                }}
                className="inline-flex items-center gap-1 px-3 py-1.5 text-xs font-medium text-blue border border-blue/30 rounded-md hover:bg-blue hover:text-white transition-colors"
              >
                <Pencil className="w-3.5 h-3.5" />
                Edit
              </button>
            )}
          </div>
        </div>
      </div>

      {isEditing && (
        <div className="fixed inset-0 z-99999 flex items-center justify-center bg-black/60 p-4">
          <div className="w-full max-w-md rounded-xl bg-white p-6 shadow-2xl">
            <h3 className="text-lg font-semibold text-dark mb-4">Edit Product</h3>
            <form onSubmit={handleSaveEdit} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-dark mb-1">Product Name</label>
                <input
                  type="text"
                  required
                  value={editForm.name}
                  onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
                  className="w-full rounded-lg border border-gray-3 px-3 py-2 text-sm focus:border-blue focus:outline-none"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-dark mb-1">Price ($)</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    required
                    value={editForm.price}
                    onChange={(e) => setEditForm({ ...editForm, price: e.target.value })}
                    className="w-full rounded-lg border border-gray-3 px-3 py-2 text-sm focus:border-blue focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-dark mb-1">Stock</label>
                  <input
                    type="number"
                    min="0"
                    required
                    value={editForm.stock}
                    onChange={(e) => setEditForm({ ...editForm, stock: Number(e.target.value) })}
                    className="w-full rounded-lg border border-gray-3 px-3 py-2 text-sm focus:border-blue focus:outline-none"
                  />
                </div>
              </div>
              <div>
                <label className="block text-xs font-medium text-dark mb-1">Status</label>
                <select
                  value={editForm.status}
                  onChange={(e) => setEditForm({ ...editForm, status: e.target.value as "ACTIVE" | "INACTIVE" })}
                  className="w-full rounded-lg border border-gray-3 px-3 py-2 text-sm focus:border-blue focus:outline-none"
                >
                  <option value="ACTIVE">ACTIVE</option>
                  <option value="INACTIVE">INACTIVE</option>
                </select>
              </div>
              {editError && <p className="text-xs text-red">{editError}</p>}
              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setIsEditing(false)}
                  className="px-4 py-2 text-sm font-medium text-dark-4 hover:bg-gray-2 rounded-lg"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSaving}
                  className="px-4 py-2 text-sm font-medium text-white bg-blue hover:bg-blue-dark rounded-lg disabled:opacity-50"
                >
                  {isSaving ? "Saving..." : "Save changes"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default React.memo(SingleListItem);
