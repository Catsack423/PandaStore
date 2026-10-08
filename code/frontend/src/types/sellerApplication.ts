export type SellerApplicationStatus = "PENDING" | "APPROVED" | "REJECTED" | "NEED_MORE_DOC";

export interface SellerApplicationRecord {
  applicationId: number;
  userId: number;
  shopName: string;
  shopDescription: string;
  shopPhone: string;
  shopEmail: string;
  shopAddress: string;
  sellerFirstName: string;
  sellerLastName: string;
  idCardNumber: string;
  idCardImageUrl: string;
  bankName: string;
  bankAccountName: string;
  bankAccountNumber: string;
  bankBookImageUrl: string;
  status: SellerApplicationStatus;
  adminNote: string | null;
  createdAt: string;
}
