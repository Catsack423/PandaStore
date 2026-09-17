import Breadcrumb from "@/components/Common/Breadcrumb";
import Link from "next/link";
import React from "react";
import SignInCard from "./SignInCard";

function SignSelection() {
  return (
    <>
      <Breadcrumb title={"Signup"} pages={["Signup"]} />
      <section className="overflow-hidden py-16 lg:py-24 bg-gray-2">
        <div className="max-w-[1170px] w-full mx-auto px-4 sm:px-8 xl:px-0">
          <div className="text-center max-w-[600px] mx-auto mb-12">
            <h2 className="font-bold text-2xl sm:text-3xl xl:text-heading-4 text-dark mb-3">
              เลือกประเภทบัญชีที่คุณต้องการสมัคร
            </h2>
            <p className="text-body text-base">
              เริ่มต้นใช้งาน PandaStore โดยเลือกประเภทบัญชีที่เหมาะกับคุณ
            </p>
          </div>

          <div className="flex flex-col justify-center md:flex-row gap-8  items-center max-w-[860px] mx-auto">
            {/* Customer Role Card */}
            <SignInCard
              title="ลูกค้าทั่วไป"
              roleSubtitle="Customer"
              description="เหมาะสำหรับผู้ที่ต้องการเลือกซื้อสินค้าจากร้านค้าคุณภาพมากมาย"
              href="/signup/customer"
              buttonText="สมัครเป็นลูกค้า ➔"
              isPopular={true}
              icon={
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
                    d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z"
                  />
                </svg>
              }
              features={[
                "ค้นหาและเลือกซื้อสินค้าหลากหลาย",
                "บันทึกที่อยู่จัดส่งได้หลายแห่ง",
                "ติดตามสถานะคำสั่งซื้อแบบเรียลไทม์",
                "รับโปรโมชั่นและส่วนลดพิเศษ",
              ]}
            />

            {/* Seller Role Card */}
            <SignInCard
              title="ผู้ขาย / ร้านค้า"
              roleSubtitle="Seller"
              description="เหมาะสำหรับเจ้าของธุรกิจหรือร้านค้าที่ต้องการขายสินค้าออนไลน์"
              href="/signup/seller"
              buttonText="สมัครเปิดร้านค้า ➔"
              isPopular={false}
              icon={
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
              }
              features={[
                "เปิดร้านค้าออนไลน์ได้ทันที",
                "ระบบจัดการสินค้าและสต็อก",
                "ติดตามออเดอร์และการจัดส่งพัสดุ",
                "ระบบจัดการการเงินและยอดขาย",
              ]}
            />
          </div>

          <p className="text-center mt-10 text-body">
            มีบัญชีอยู่แล้ว?
            <Link
              href="/signin"
              className="text-dark font-medium ease-out duration-200 hover:text-blue pl-2 underline underline-offset-4"
            >
              เข้าสู่ระบบที่นี่
            </Link>
          </p>
        </div>
      </section>
    </>
  );
}

export default SignSelection;
