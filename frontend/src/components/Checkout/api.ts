export type CheckoutItem = {
  productId: number; productName: string; sellerId: number; shopName: string;
  unitPrice: number; quantity: number; imageUrl: string | null;
};
export type Address = {
  addressId: number; receiverName: string; phoneNumber: string; addressLine: string;
  district: string; province: string; postalCode: string; isDefault: boolean;
  hasOrders?: boolean;
};
export type CheckoutData = {
  customerId: number; items: CheckoutItem[]; addresses: Address[];
  shippingMethods: string[]; paymentMethods: string[];
};
export type PlacedOrder = {
  orderGroupId: number; groupNumber: string; grandTotal: number; paymentStatus: string;
  totalProductsAmount: number; totalShippingFee: number;
  subOrders: { orderId: number; shippingMethod: string; shippingFee: number; totalAmount: number; orderStatus: string }[];
};
export const money = (value: number) => new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(value);
export const methodLabel = (method: string) => ({
  STANDARD: "Standard delivery", EMS: "EMS", FLASH: "Flash Express", KERRY: "Kerry Express",
  "J&T": "J&T Express", THAILANDPOST: "Thailand Post", CREDIT_CARD: "Credit card",
  PROMPTPAY: "PromptPay", BANK_TRANSFER: "Bank transfer", WALLET: "Wallet",
}[method] || method);

export async function checkoutApi<T>(path = "", body?: object, signal?: AbortSignal, method?: "GET" | "POST" | "PUT"): Promise<T> {
  const httpMethod = method || (body ? "POST" : "GET");
  const response = await fetch(`/api/checkout${path ? `/${path}` : ""}`, {
    method: httpMethod, cache: "no-store", signal,
    ...(body ? { headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) } : {}),
  });
  const result = await response.json().catch(() => null);
  if (!response.ok || !result?.success) {
    const fields = result?.error && typeof result.error === "object" ? Object.values(result.error).filter(v => typeof v === "string").join(". ") : "";
    throw new Error(fields || result?.message || "Could not reach checkout service");
  }
  return result.data as T;
}
