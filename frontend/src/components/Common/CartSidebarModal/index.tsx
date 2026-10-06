"use client";
import { formatBaht } from "@/lib/currency";
import React, { useEffect } from "react";

import { useCartModalContext } from "@/app/context/CartSidebarModalContext";
import { useCart } from "@/app/context/CartContext";
import SingleItem from "./SingleItem";
import Link from "next/link";
import EmptyCart from "./EmptyCart";

const CartSidebarModal = () => {
  const { isCartModalOpen, closeCartModal } = useCartModalContext();
  const { items: cartItems, totalPrice, removeItemFromCart, isLoading, isPending, error, refreshCart } = useCart();

  useEffect(() => { if (isCartModalOpen) void refreshCart(); }, [isCartModalOpen, refreshCart]);

  useEffect(() => {
    if (!isCartModalOpen) return;
    const previousOverflow = document.body.style.overflow;
    const previousPadding = document.body.style.paddingRight;
    const scrollbarWidth = window.innerWidth - document.documentElement.clientWidth;
    const bodyPadding = parseFloat(window.getComputedStyle(document.body).paddingRight) || 0;
    document.body.style.overflow = "hidden";
    if (scrollbarWidth > 0) document.body.style.paddingRight = `${bodyPadding + scrollbarWidth}px`;
    return () => {
      document.body.style.overflow = previousOverflow;
      document.body.style.paddingRight = previousPadding;
    };
  }, [isCartModalOpen]);

  useEffect(() => {
    // closing modal while clicking outside
    function handleClickOutside(event) {
      if (!event.target.closest(".modal-content")) {
        closeCartModal();
      }
    }

    if (isCartModalOpen) {
      document.addEventListener("mousedown", handleClickOutside);
    }

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [isCartModalOpen, closeCartModal]);

  return (
    <div
      aria-hidden={!isCartModalOpen}
      inert={!isCartModalOpen}
      className={`fixed top-0 left-0 z-99999 overflow-hidden w-full h-[100dvh] ${
        isCartModalOpen ? "pointer-events-auto" : "pointer-events-none"
      }`}
    >
      <div aria-hidden="true" className={`absolute inset-0 bg-dark/70 transition-opacity duration-300 ease-out motion-reduce:transition-none ${isCartModalOpen ? "opacity-100" : "opacity-0"}`} />
      <div className="relative flex max-h-[100dvh] items-start justify-end">
        <div className={`relative flex h-[100dvh] w-full max-w-[440px] flex-col bg-white shadow-1 transition-transform duration-300 ease-out motion-reduce:transition-none modal-content ${isCartModalOpen ? "translate-x-0" : "translate-x-full"}`}>
          <div className="flex shrink-0 items-center justify-between border-b border-gray-3 bg-white px-4 py-4 sm:px-5">
            <h2 className="font-medium text-dark text-lg sm:text-2xl">
              Cart View
            </h2>
            <button
              onClick={() => closeCartModal()}
              aria-label="button for close modal"
              className="flex h-11 w-11 items-center justify-center rounded-lg ease-in duration-150 bg-meta text-dark-5 hover:bg-gray-2 hover:text-dark"
            >
              <svg
                className="fill-current"
                width="30"
                height="30"
                viewBox="0 0 30 30"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
              >
                <path
                  d="M12.5379 11.2121C12.1718 10.846 11.5782 10.846 11.212 11.2121C10.8459 11.5782 10.8459 12.1718 11.212 12.5379L13.6741 15L11.2121 17.4621C10.846 17.8282 10.846 18.4218 11.2121 18.7879C11.5782 19.154 12.1718 19.154 12.5379 18.7879L15 16.3258L17.462 18.7879C17.8281 19.154 18.4217 19.154 18.7878 18.7879C19.154 18.4218 19.154 17.8282 18.7878 17.462L16.3258 15L18.7879 12.5379C19.154 12.1718 19.154 11.5782 18.7879 11.2121C18.4218 10.846 17.8282 10.846 17.462 11.2121L15 13.6742L12.5379 11.2121Z"
                  fill=""
                />
                <path
                  fillRule="evenodd"
                  clipRule="evenodd"
                  d="M15 1.5625C7.57867 1.5625 1.5625 7.57867 1.5625 15C1.5625 22.4213 7.57867 28.4375 15 28.4375C22.4213 28.4375 28.4375 22.4213 28.4375 15C28.4375 7.57867 22.4213 1.5625 15 1.5625ZM3.4375 15C3.4375 8.61421 8.61421 3.4375 15 3.4375C21.3858 3.4375 26.5625 8.61421 26.5625 15C26.5625 21.3858 21.3858 26.5625 15 26.5625C8.61421 26.5625 3.4375 21.3858 3.4375 15Z"
                  fill=""
                />
              </svg>
            </button>
          </div>

          <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-4 py-4 sm:px-5">
            <div className="flex flex-col gap-4">
              {/* <!-- cart item --> */}
              {isLoading ? <p role="status">Loading your cart…</p> : error && cartItems.length === 0 ? <p role="alert">{error}</p> : cartItems.length > 0 ? (
                cartItems.map((item) => (
                  <SingleItem
                    key={item.id}
                    item={item}
                    removeItemFromCart={removeItemFromCart} disabled={isPending}
                  />
                ))
              ) : (
                <EmptyCart />
              )}
            </div>
          </div>

          <div className="shrink-0 border-t border-gray-3 bg-white px-4 py-4 sm:px-5">
            <div className="flex items-center justify-between gap-5 mb-4">
              <p className="font-medium text-xl text-dark">Subtotal:</p>

              <p className="font-medium text-xl text-dark">{formatBaht(totalPrice)}</p>
            </div>

            <div className="flex items-center gap-3">
              <Link
                onClick={() => closeCartModal()}
                href="/cart"
                className="w-full flex justify-center font-medium text-white bg-blue py-[13px] px-3 rounded-md ease-out duration-200 hover:bg-blue-dark"
              >
                View Cart
              </Link>

              <Link
                href="/checkout" aria-disabled={isPending} onClick={event => { if (isPending) event.preventDefault(); else closeCartModal(); }}
                className="w-full flex justify-center font-medium text-white bg-dark py-[13px] px-3 rounded-md ease-out duration-200 hover:bg-opacity-95"
              >
                Checkout
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CartSidebarModal;
