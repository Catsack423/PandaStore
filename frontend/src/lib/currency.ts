const amountFormatter = new Intl.NumberFormat("th-TH", {
  minimumFractionDigits: 0,
  maximumFractionDigits: 2,
});

/** Format the existing baht amount for display; never convert stored prices. */
export function formatBaht(amount: number): string {
  return `${amountFormatter.format(amount)} บาท`;
}

export const bahtCurrency = { format: formatBaht };
