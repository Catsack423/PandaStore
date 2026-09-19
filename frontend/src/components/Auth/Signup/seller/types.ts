export interface SellerFormData {
  // Stage 1: Account Information
  username: string;
  email: string;
  password: string;
  confirmPassword: string;

  // Stage 2: Shop Information
  shopName: string;
  shopDescription: string;
  shopPhone: string;
  shopEmail: string;
  shopAddress: string;

  // Stage 3: Identity & Bank Account (UC3)
  sellerFirstName: string;
  sellerLastName: string;
  idCardNumber: string;
  bankName: string;
  bankAccountName: string;
  bankAccountNumber: string;
}

export interface FormErrors {
  [key: string]: string | undefined;
}

export interface StageInfo {
  id: number;
  title: string;
  subtitle: string;
}

export const STAGES: StageInfo[] = [
  { id: 1, title: "Account Info", subtitle: "Account Info" },
  { id: 2, title: "Shop Info", subtitle: "Shop Info" },
  { id: 3, title: "Identity & Bank", subtitle: "Identity & Bank" },
];
