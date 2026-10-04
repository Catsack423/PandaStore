"use client";

import { methodLabel, money } from "./api";

type Props = {
  sellerId: number; methods: string[]; value: string; fees: Record<string, number>;
  loading: boolean; disabled: boolean; onChange: (method: string) => void;
};
export default function ShippingMethod({ sellerId, methods, value, fees, loading, disabled, onChange }: Props) {
  return <fieldset disabled={disabled} className="mt-5 border-t border-gray-3 pt-5">
    <legend className="sr-only">Shipping method for shop {sellerId}</legend>
    <p className="mb-3 text-sm font-semibold text-dark">Shipping method and fee</p>
    <div className="grid gap-2 sm:grid-cols-2">
      {methods.map(method => <label key={method} className={`flex cursor-pointer items-center gap-3 rounded-lg border p-3 text-sm ${value === method ? "border-blue bg-blue/5" : "border-gray-3"} ${disabled ? "cursor-not-allowed opacity-60" : ""}`}>
        <input type="radio" name={`shipping-${sellerId}`} value={method} checked={value === method}
          onChange={() => onChange(method)} className="accent-blue" />
        <span className="min-w-0 flex-1 text-dark">{methodLabel(method)}</span>
        <span className="shrink-0 font-medium text-dark">{loading ? "…" : fees[method] === undefined ? "—" : money(fees[method])}</span>
      </label>)}
    </div>
    <p className="mt-3 text-xs text-dark-5">{disabled ? "Choose a delivery address to calculate shipping." : loading ? "Calculating shipping…" : "Shipping fees are calculated by the shop’s delivery service."}</p>
  </fieldset>;
}
