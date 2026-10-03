"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/app/context/AuthContext";
import DeliveryAddress from "@/components/Checkout/DeliveryAddress";
import { Address, CheckoutData, checkoutApi } from "@/components/Checkout/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import ConfirmDialog from "@/components/Common/ConfirmDialog";
import toast from "react-hot-toast";

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
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [confirmDeleteId, setConfirmDeleteId] = useState<number | null>(null);

  async function removeAddress(id: number) {
    setDeletingId(id);
    setDefaultError("");
    try {
      const response = await fetch(`/api/customers/${data.customerId}/addresses/${id}`, { method: "DELETE" });
      const result = await response.json().catch(() => null);
      if (!response.ok || (result && !result.success)) {
        throw new Error(result?.message || "Could not delete address");
      }
      queryClient.setQueryData<CheckoutData>(key, current => current ? {
        ...current,
        addresses: current.addresses.filter(address => address.addressId !== id),
      } : current);
      if (selected === id) {
        const remaining = data.addresses.filter(a => a.addressId !== id);
        setSelected(remaining.find(a => a.isDefault)?.addressId || remaining[0]?.addressId || null);
      }
      toast.success("Address deleted successfully");
      await queryClient.invalidateQueries({ queryKey: key });
    } catch (error) {
      const msg = error instanceof Error ? error.message : "Could not delete address";
      setDefaultError(msg);
      toast.error(msg);
    } finally {
      setDeletingId(null);
      setConfirmDeleteId(null);
    }
  }

  return (
    <div className="space-y-3">
      {defaultError && <p role="alert" className="text-sm text-red">{defaultError}</p>}
      <DeliveryAddress
        customerId={data.customerId}
        addresses={data.addresses}
        value={value}
        disabled={settingDefault || deletingId !== null}
        onSelect={setSelected}
        onAdded={added}
        onSetDefault={setDefault}
        settingDefault={settingDefault}
        onDelete={(id) => setConfirmDeleteId(id)}
        deletingId={deletingId}
      />
      <ConfirmDialog
        isOpen={confirmDeleteId !== null}
        onClose={() => setConfirmDeleteId(null)}
        onConfirm={() => confirmDeleteId !== null && removeAddress(confirmDeleteId)}
        title="Delete Address"
        description="Are you sure you want to remove this delivery address? This action cannot be undone."
        confirmText="Delete"
        variant="danger"
        icon="trash"
      />
    </div>
  );
}
