"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/app/context/AuthContext";
import DeliveryAddress from "@/components/Checkout/DeliveryAddress";
import { Address, CheckoutData, checkoutApi } from "@/components/Checkout/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";

export default function Addresses() {
  const { user, isLoading: authLoading } = useAuth();
  const queryClient = useQueryClient();
  const [selected, setSelected] = useState<number | null>(null);
  const [settingDefault, setSettingDefault] = useState(false);
  const [defaultError, setDefaultError] = useState("");
  const customer = user?.role === "CUSTOMER";
  const key = ["account-addresses", user?.id];
  const addresses = useQuery({ queryKey: key,
    queryFn: ({ signal }) => checkoutApi<CheckoutData>("", undefined, signal),
    enabled: !authLoading && customer, staleTime: 0, gcTime: 0,
    refetchOnWindowFocus: "always", retry: 1 });

  if (authLoading || (customer && addresses.isPending)) return <Card><CardContent role="status" className="p-6">Loading delivery addresses…</CardContent></Card>;
  if (!customer) return <Card><CardHeader><CardTitle>Delivery address</CardTitle></CardHeader><CardContent className="pb-6"><p className="text-sm">{user ? "Delivery addresses are available for customer accounts." : "Sign in to view and save your delivery addresses."}</p>{!user && <Link href="/signin" className="mt-4 inline-block text-sm text-blue hover:underline">Sign in</Link>}</CardContent></Card>;
  if (addresses.isError) return <Card><CardContent role="alert" className="p-6"><p className="text-sm text-red">{addresses.error.message}</p><Button variant="outline" className="mt-4" disabled={addresses.isFetching} onClick={() => void addresses.refetch()}>Try again</Button></CardContent></Card>;

  const data = addresses.data!;
  const value = data.addresses.some(address => address.addressId === selected) ? selected
    : data.addresses.find(address => address.isDefault)?.addressId || data.addresses[0]?.addressId || null;
  function added(address: Address) {
    queryClient.setQueryData<CheckoutData>(key, current => current
      ? { ...current, addresses: [...current.addresses, address] } : current);
    setSelected(address.addressId);
    void queryClient.invalidateQueries({ queryKey: key });
  }
  async function setDefault(id: number) {
    setSettingDefault(true); setDefaultError("");
    try {
      const response = await fetch(`/api/customers/${data.customerId}/addresses/${id}/default`, { method: "PUT" });
      const result = await response.json();
      if (!response.ok || !result.success) throw new Error(result.message || "Could not update default address");
      queryClient.setQueryData<CheckoutData>(key, current => current ? { ...current,
        addresses: current.addresses.map(address => ({ ...address, isDefault: address.addressId === id })) } : current);
      setSelected(id);
      await queryClient.invalidateQueries({ queryKey: key });
    } catch (error) { setDefaultError(error instanceof Error ? error.message : "Could not update default address"); }
    finally { setSettingDefault(false); }
  }
  return <div className="space-y-3">{defaultError && <p role="alert" className="text-sm text-red">{defaultError}</p>}<DeliveryAddress customerId={data.customerId} addresses={data.addresses} value={value}
    disabled={settingDefault} onSelect={setSelected} onAdded={added} onSetDefault={setDefault} settingDefault={settingDefault} /></div>;
}
