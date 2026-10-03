"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Address, checkoutApi } from "./api";

export default function DeliveryAddress({ customerId, addresses, value, disabled, onSelect, onAdded, onSetDefault, settingDefault, onDelete, deletingId }: {
  customerId: number; addresses: Address[]; value: number | null; disabled: boolean;
  onSelect: (id: number) => void; onAdded: (address: Address) => void;
  onSetDefault?: (id: number) => void; settingDefault?: boolean;
  onDelete?: (id: number) => void; deletingId?: number | null;
}) {
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  async function save(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault(); setSaving(true); setError("");
    const form = event.currentTarget;
    const values = Object.fromEntries(new FormData(form));
    try {
      const address = await checkoutApi<Address>("addresses", { ...values, customerId, isDefault: addresses.length === 0 });
      onAdded(address); setOpen(false); form.reset();
    } catch (error) { setError(error instanceof Error ? error.message : "Could not save address"); }
    finally { setSaving(false); }
  }
  return <Card><CardHeader className="border-b border-gray-3"><CardTitle>Delivery address</CardTitle></CardHeader>
    <CardContent className="pt-6">
      <fieldset disabled={disabled || saving} className="space-y-3">
        {addresses.length === 0 && <p className="text-sm">Add a delivery address to continue.</p>}
        {addresses.map(address => <div key={address.addressId} className={`flex flex-wrap items-center gap-3 rounded-lg border p-4 ${value === address.addressId ? "border-blue bg-blue/5" : "border-gray-3"}`}><label className="flex min-w-0 flex-1 cursor-pointer gap-3">
          <input type="radio" name="delivery-address" checked={value === address.addressId} onChange={() => onSelect(address.addressId)} className="mt-1 accent-blue" />
          <span className="text-sm"><strong className="text-dark">{address.receiverName}</strong>{address.isDefault && <span className="ml-2 text-xs text-blue">Default</span>}
            <span className="mt-1 block">{address.phoneNumber}</span>
            <span className="mt-1 block">{address.addressLine}, {address.district}, {address.province} {address.postalCode}</span></span>
        </label>
        <div className="flex items-center gap-2">
          {onSetDefault && !address.isDefault && value === address.addressId && (
            <Button
              type="button"
              size="sm"
              variant="outline"
              disabled={settingDefault || disabled}
              onClick={() => onSetDefault(address.addressId)}
              className="border-blue text-blue hover:bg-blue hover:text-white"
            >
              {settingDefault ? "Saving…" : "Set Default"}
            </Button>
          )}
          {onDelete && <Button type="button" size="sm" variant="outline" disabled={disabled || deletingId === address.addressId} onClick={() => onDelete(address.addressId)} className="border-gray-3 text-dark-4 hover:border-red hover:bg-red/10 hover:text-red">{deletingId === address.addressId ? "Deleting…" : "Delete"}</Button>}
        </div></div>)}
        <Button type="button" variant="outline" onClick={() => setOpen(!open)}>{open ? "Close address form" : "Add delivery address"}</Button>
      </fieldset>
      {open && <form onSubmit={save} className="mt-5 border-t border-gray-3 pt-5">
        <fieldset disabled={saving || disabled} className="grid gap-4 sm:grid-cols-2">
          {([ ["receiverName", "Receiver name", "text", 100], ["phoneNumber", "Phone number", "tel", 10],
            ["addressLine", "Street address", "text", 255], ["district", "District", "text", 100],
            ["province", "Province", "text", 100], ["postalCode", "Postal code", "text", 5] ] as const).map(([name, label, type, maxLength]) =>
              <label key={name} className="text-sm text-dark">{label}
                <Input
                  name={name}
                  type={type}
                  required
                  maxLength={maxLength}
                  inputMode={name === "phoneNumber" || name === "postalCode" ? "numeric" : undefined}
                  onInput={(e) => {
                    if (name === "phoneNumber") {
                      e.currentTarget.value = e.currentTarget.value.replace(/\D/g, "").slice(0, 10);
                    } else if (name === "postalCode") {
                      e.currentTarget.value = e.currentTarget.value.replace(/\D/g, "").slice(0, 5);
                    }
                  }}
                  placeholder={name === "phoneNumber" ? "08XXXXXXXX (10 digits)" : undefined}
                  className="mt-2"
                  pattern={name === "phoneNumber" ? "[0-9]{9,10}" : name === "postalCode" ? "[0-9]{5}" : undefined}
                />
              </label>)}
          {error && <p role="alert" className="text-sm text-red sm:col-span-2">{error}</p>}
          <Button type="submit" className="bg-blue text-white sm:col-span-2">{saving ? "Saving…" : "Save address"}</Button>
        </fieldset>
      </form>}
    </CardContent></Card>;
}
