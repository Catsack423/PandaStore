import type { Category } from "@/types/category";

export const templateCategories: Category[] = [
  { id: 1, name: "Accessories" },
  { id: 2, name: "Phone" },
  { id: 3, name: "Desktop" },
  { id: 4, name: "Laptop" },
  { id: 5, name: "Watch" },
  { id: 6, name: "Tablet" },
  { id: 7, name: "Networking" },
];

const assignments: Record<number, { title: string; categoryId: number }> = {
  1: { title: "Havit HV-G69 USB Gamepad", categoryId: 1 },
  2: { title: "iPhone 14 Plus , 6/128GB", categoryId: 2 },
  3: { title: "Apple iMac M1 24-inch 2021", categoryId: 3 },
  4: { title: "MacBook Air M1 chip, 8/256GB", categoryId: 4 },
  5: { title: "Apple Watch Ultra", categoryId: 5 },
  6: { title: "Logitech MX Master 3 Mouse", categoryId: 1 },
  7: { title: "Apple iPad Air 5th Gen - 64GB", categoryId: 6 },
  8: { title: "Asus RT Dual Band Router", categoryId: 7 },
};

export function templateCategoryIds(product: { id: number; title: string }): number[] {
  const assignment = assignments[product.id];
  return assignment?.title === product.title ? [assignment.categoryId] : [];
}
