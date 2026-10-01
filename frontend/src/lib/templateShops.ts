// Store names for the template-only catalog. Real catalog products use their seller metadata.
const templateProducts: Record<number, { title: string; shop: string }> = {
  1: { title: "Havit HV-G69 USB Gamepad", shop: "Playfield Goods" },
  2: { title: "iPhone 14 Plus , 6/128GB", shop: "Playfield Goods" },
  3: { title: "Apple iMac M1 24-inch 2021", shop: "Playfield Goods" },
  4: { title: "MacBook Air M1 chip, 8/256GB", shop: "Playfield Goods" },
  5: { title: "Apple Watch Ultra", shop: "Everyday Devices" },
  6: { title: "Logitech MX Master 3 Mouse", shop: "Everyday Devices" },
  7: { title: "Apple iPad Air 5th Gen - 64GB", shop: "Everyday Devices" },
  8: { title: "Asus RT Dual Band Router", shop: "Everyday Devices" },
};

export function templateShopName(product: { id: number; title: string }): string | undefined {
  const preview = templateProducts[product.id];
  return preview?.title === product.title ? preview.shop : undefined;
}
