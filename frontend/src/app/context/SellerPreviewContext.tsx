"use client";

import { createContext, useContext, useState, type ReactNode } from "react";

export type ApplicationStatus = "PENDING" | "APPROVED" | "REJECTED" | "NEED_MORE_DOC";
export type SellerOrderStatus = "PENDING_PAYMENT" | "WAITING_SELLER_CONFIRM" | "PREPARING" | "SHIPPED" | "COMPLETED" | "CANCELLED";

export type SellerApplicationPreview = {
  applicationId: number;
  sellerId: number;
  shopName: string;
  shopDescription: string;
  shopPhone: string;
  shopEmail: string;
  shopAddress: string;
  status: ApplicationStatus;
  submittedAt: string;
  adminNote?: string;
  sellerNote?: string;
  documentNames: string[];
  revisionOf?: number;
};

export type SellerOrderPreview = {
  orderId: number;
  orderNumber: string;
  createdAt: string;
  customer: string;
  status: SellerOrderStatus;
  totalAmount: number;
  items: { name: string; quantity: number; unitPrice: number }[];
  rejectionReason?: string;
  courierName?: string;
  trackingNumber?: string;
};

const demoOrders: SellerOrderPreview[] = [
  { orderId: 4101, orderNumber: "DEMO-4101", createdAt: "2026-09-21", customer: "Demo Customer A", status: "WAITING_SELLER_CONFIRM", totalAmount: 129, items: [{ name: "Wireless headphones", quantity: 1, unitPrice: 129 }] },
  { orderId: 4102, orderNumber: "DEMO-4102", createdAt: "2026-09-20", customer: "Demo Customer B", status: "PREPARING", totalAmount: 89, items: [{ name: "Desk lamp", quantity: 1, unitPrice: 89 }] },
  { orderId: 4103, orderNumber: "DEMO-4103", createdAt: "2026-09-18", customer: "Demo Customer C", status: "SHIPPED", totalAmount: 219, items: [{ name: "Portable monitor", quantity: 1, unitPrice: 219 }] },
  { orderId: 4104, orderNumber: "DEMO-4104", createdAt: "2026-09-15", customer: "Demo Customer D", status: "COMPLETED", totalAmount: 59, items: [{ name: "USB hub", quantity: 1, unitPrice: 59 }] },
  { orderId: 4105, orderNumber: "DEMO-4105", createdAt: "2026-09-14", customer: "Demo Customer E", status: "CANCELLED", totalAmount: 42, items: [{ name: "Phone stand", quantity: 1, unitPrice: 42 }], rejectionReason: "Item unavailable" },
  { orderId: 4106, orderNumber: "DEMO-4106", createdAt: "2026-09-22", customer: "Demo Customer F", status: "PENDING_PAYMENT", totalAmount: 75, items: [{ name: "Keyboard", quantity: 1, unitPrice: 75 }] },
];

export type ShopFields = Pick<SellerApplicationPreview, "shopName" | "shopDescription" | "shopPhone" | "shopEmail" | "shopAddress">;
type SellerPreviewContextValue = {
  application: SellerApplicationPreview | null;
  applications: SellerApplicationPreview[];
  orders: SellerOrderPreview[];
  createApplication: (shop: ShopFields) => void;
  resubmitApplication: (shop: ShopFields, sellerNote: string, documentNames: string[]) => boolean;
  startSampleApplication: () => void;
  changeDemoStatus: (status: ApplicationStatus) => void;
  acceptOrder: (id: number) => boolean;
  rejectOrder: (id: number, reason: string) => boolean;
  shipOrder: (id: number, courierName: string, trackingNumber: string) => boolean;
};

const SellerPreviewContext = createContext<SellerPreviewContextValue | null>(null);

export function SellerPreviewProvider({ children }: { children: ReactNode }) {
  const [applications, setApplications] = useState<SellerApplicationPreview[]>([]);
  const application = applications[0] ?? null;
  const [orders, setOrders] = useState<SellerOrderPreview[]>(demoOrders);

  const createApplication = (shop: ShopFields) => {
    setApplications((current) => [{
      applicationId: (current[0]?.applicationId ?? 0) + 1,
      sellerId: 0,
      ...shop,
      status: "PENDING",
      submittedAt: new Date().toISOString(),
      documentNames: [],
    }, ...current]);
    setOrders(demoOrders);
  };
  const resubmitApplication = (shop: ShopFields, sellerNote: string, documentNames: string[]) => {
    if (!application || !["REJECTED", "NEED_MORE_DOC"].includes(application.status) || !sellerNote.trim() || (application.status === "NEED_MORE_DOC" && documentNames.length === 0)) return false;
    setApplications((current) => [{
      applicationId: (current[0]?.applicationId ?? 0) + 1,
      sellerId: current[0].sellerId,
      ...shop,
      status: "PENDING",
      submittedAt: new Date().toISOString(),
      sellerNote: sellerNote.trim(),
      documentNames,
      revisionOf: current[0].applicationId,
    }, ...current]);
    setOrders(demoOrders);
    return true;
  };
  const startSampleApplication = () => createApplication({
    shopName: "Northline Goods",
    shopDescription: "Thoughtful tools and accessories for everyday work.",
    shopPhone: "0000000000",
    shopEmail: "demo@example.com",
    shopAddress: "Demo storefront",
  });
  const changeDemoStatus = (status: ApplicationStatus) => setApplications((current) => current.map((item, index) => index === 0 ? {
    ...item,
    status,
    adminNote: status === "NEED_MORE_DOC" ? "Please provide a clearer identity document." : status === "REJECTED" ? "The application did not meet the review requirements." : undefined,
  } : item));
  const canManage = application?.status === "APPROVED";
  const acceptOrder = (id: number) => {
    if (!canManage || !orders.some((order) => order.orderId === id && order.status === "WAITING_SELLER_CONFIRM")) return false;
    setOrders((current) => current.map((order) => order.orderId === id ? { ...order, status: "PREPARING" } : order));
    return true;
  };
  const rejectOrder = (id: number, reason: string) => {
    if (!canManage || !reason.trim() || reason.trim().length > 255 || !orders.some((order) => order.orderId === id && order.status === "WAITING_SELLER_CONFIRM")) return false;
    setOrders((current) => current.map((order) => order.orderId === id ? { ...order, status: "CANCELLED", rejectionReason: reason.trim() } : order));
    return true;
  };
  const shipOrder = (id: number, courierName: string, trackingNumber: string) => {
    if (!canManage || !courierName.trim() || !trackingNumber.trim() || !orders.some((order) => order.orderId === id && order.status === "PREPARING")) return false;
    setOrders((current) => current.map((order) => order.orderId === id ? { ...order, status: "SHIPPED", courierName: courierName.trim(), trackingNumber: trackingNumber.trim() } : order));
    return true;
  };

  return <SellerPreviewContext.Provider value={{ application, applications, orders, createApplication, resubmitApplication, startSampleApplication, changeDemoStatus, acceptOrder, rejectOrder, shipOrder }}>{children}</SellerPreviewContext.Provider>;
}

export function useSellerPreview() {
  const context = useContext(SellerPreviewContext);
  if (!context) throw new Error("useSellerPreview must be used within SellerPreviewProvider");
  return context;
}
