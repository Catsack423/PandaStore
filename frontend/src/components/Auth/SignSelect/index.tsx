import Breadcrumb from "@/components/Common/Breadcrumb";
import Link from "next/link";
import React from "react";
import SignInCard from "./SignInCard";

const iconArrow = (
  <svg
    className="w-6 h-6"
    fill="none"
    viewBox="0 0 24 24"
    stroke="currentColor"
  >
    <path
      strokeLinecap="round"
      strokeLinejoin="round"
      strokeWidth={2}
      d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"
    />
  </svg>
);

const customerFeature = [
  "Browse and shop a wide range of products",
  "Save multiple shipping addresses",
  "Track order status in real time",
  "Receive special offers and discounts",
];

const sellerFeature = [
  "Open your online shop",
  "Manage products and inventory",
  "Track orders and shipments",
  "Manage finances and sales",
];

function SignSelection() {
  return (
    <>
      <Breadcrumb title={"Signup"} pages={["Signup"]} />
      <section className="overflow-hidden py-16 lg:py-24 bg-gray-2">
        <div className="max-w-[1170px] w-full mx-auto px-4 sm:px-8 xl:px-0">
          <div className="text-center max-w-[600px] mx-auto mb-12">
            <h2 className="font-bold text-2xl sm:text-3xl xl:text-heading-4 text-dark mb-3">
              Choose your account type
            </h2>
            <p className="text-body text-base">
              Get started with PandaStore by choosing the account that suits you
            </p>
          </div>

          <div className="flex flex-col justify-center md:flex-row gap-8  items-center max-w-[860px] mx-auto">
            {/* Customer Role Card */}
            <SignInCard
              title="Customer"
              roleSubtitle="Customer"
              description="For shoppers looking for products from trusted shops"
              href="/signup"
              buttonText="Sign up as a customer ➔"
              isPopular={true}
              icon={iconArrow}
              features={customerFeature}
            />

            {/* Seller Role Card */}
            <SignInCard
              title="Seller / Shop"
              roleSubtitle="Seller"
              description="For businesses and shop owners who want to sell online"
              href="/seller-application"
              buttonText="Register your shop ➔"
              isPopular={false}
              icon={iconArrow}
              features={sellerFeature}
              hoverbg={"hover:bg-blue hover:text-white"}
            />
          </div>

          <p className="text-center mt-10 text-body ">
            Already have an account?
            <Link
              href="/signin"
              className="text-dark font-medium ease-out duration-200 hover:text-blue pl-2 underline underline-offset-4"
            >
              Log in here
            </Link>
          </p>
        </div>
      </section>
    </>
  );
}

export default SignSelection;
