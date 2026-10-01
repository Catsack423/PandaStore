"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import { Plus, RefreshCw, Tags, Trash2 } from "lucide-react";
import AdminShell from "./AdminShell";
import { adminRequest } from "./api";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";

type Category = { categoryId: number; categoryName: string; description: string | null };

export default function AdminCategories() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [confirmId, setConfirmId] = useState<number | null>(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const reload = useCallback(async () => {
    setLoading(true);
    setError("");
    try { setCategories(await adminRequest<Category[]>("categories")); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "Could not load categories"); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { void reload(); }, [reload]);

  async function add(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const categoryName = name.trim();
    if (!categoryName || saving) return;
    setSaving(true); setError(""); setNotice("");
    try {
      await adminRequest<Category>("categories", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ categoryName, description: description.trim() }) });
      setName(""); setDescription(""); setNotice(`Added “${categoryName}”.`);
      await reload();
    } catch (cause) { setError(cause instanceof Error ? cause.message : "Could not add category"); }
    finally { setSaving(false); }
  }

  async function remove(category: Category) {
    if (deletingId !== null) return;
    setDeletingId(category.categoryId); setError(""); setNotice("");
    try {
      await adminRequest<void>(`categories/${category.categoryId}`, { method: "DELETE" });
      setConfirmId(null); setNotice(`Deleted “${category.categoryName}”.`);
      await reload();
    } catch (cause) { setError(cause instanceof Error ? cause.message : "Could not delete category"); }
    finally { setDeletingId(null); }
  }

  return <AdminShell title="Categories">
    <div className="space-y-5">
      {notice && <p role="status" className="rounded-lg border border-blue/20 bg-blue/5 px-4 py-3 text-sm text-dark">{notice}</p>}
      {error && <p role="alert" className="rounded-lg border border-red/20 bg-red-light-6 px-4 py-3 text-sm text-red">{error}</p>}
      <div className="grid items-start gap-5 xl:grid-cols-[minmax(0,1fr)_300px]">
        <Card className="min-w-0 border-gray-3 bg-white shadow-1 xl:order-2"><CardHeader className="border-b border-gray-3 p-5 sm:p-6"><p className="text-xs font-semibold uppercase tracking-[0.16em] text-blue">Catalog setup</p><CardTitle className="mt-1 text-lg text-dark">Add a category</CardTitle><p className="text-sm text-dark-4">Names and descriptions come from the catalog API.</p></CardHeader><CardContent className="p-5 sm:p-6"><form onSubmit={(event) => void add(event)} className="space-y-4"><div className="space-y-2"><Label htmlFor="category-name">Category name</Label><Input id="category-name" required maxLength={100} value={name} onChange={(event) => setName(event.target.value)} placeholder="e.g. Home office" className="border-gray-3" /></div><div className="space-y-2"><Label htmlFor="category-description">Description <span className="font-normal text-dark-4">(optional)</span></Label><Textarea id="category-description" maxLength={255} value={description} onChange={(event) => setDescription(event.target.value)} placeholder="Help shoppers understand this category" className="min-h-24 border-gray-3" /><p className="text-right text-xs text-dark-4">{description.length}/255</p></div><Button type="submit" disabled={saving || !name.trim()} className="h-10 w-full bg-blue text-white hover:bg-blue-dark"><Plus className="size-4" /> {saving ? "Adding…" : "Add category"}</Button></form></CardContent></Card>
        <Card className="min-w-0 border-gray-3 bg-white shadow-1 xl:order-1"><CardHeader className="flex flex-row flex-wrap items-center justify-between gap-3 border-b border-gray-3 p-5 sm:p-6"><div><p className="text-xs font-semibold uppercase tracking-[0.16em] text-blue">Marketplace catalog</p><CardTitle className="mt-1 text-lg text-dark">{loading ? "Loading categories…" : `${categories.length} ${categories.length === 1 ? "category" : "categories"}`}</CardTitle></div><Button type="button" variant="outline" onClick={() => void reload()} disabled={loading || deletingId !== null} className="border-gray-3 text-dark"><RefreshCw className="size-4" /> Refresh</Button></CardHeader><CardContent className="p-3 sm:p-4">{!loading && categories.length === 0 ? <div className="py-14 text-center"><Tags className="mx-auto size-8 text-blue" /><p className="mt-3 font-medium text-dark">No categories yet</p><p className="mt-1 text-sm text-dark-4">Add the first one with the form.</p></div> : <ul className="divide-y divide-gray-3">{categories.map((category) => <li key={category.categoryId} className="flex flex-col gap-3 px-2 py-4 sm:flex-row sm:items-start sm:justify-between"><div className="min-w-0"><h3 className="font-semibold text-dark">{category.categoryName}</h3><p className="mt-1 break-words text-sm leading-5 text-dark-4">{category.description || "No description"}</p><p className="mt-2 text-xs text-dark-4">ID {category.categoryId}</p></div><div className="shrink-0">{confirmId === category.categoryId ? <div className="rounded-lg border border-gray-3 bg-gray-1 p-3"><p className="mb-2 max-w-44 text-xs text-dark">Delete “{category.categoryName}”?</p><div className="flex gap-2"><Button type="button" size="sm" variant="destructive" onClick={() => void remove(category)} disabled={deletingId !== null}>{deletingId === category.categoryId ? "Deleting…" : "Delete"}</Button><Button type="button" size="sm" variant="outline" onClick={() => setConfirmId(null)} disabled={deletingId !== null} className="border-gray-3 text-dark">Cancel</Button></div></div> : <Button type="button" size="sm" variant="outline" onClick={() => setConfirmId(category.categoryId)} className="border-gray-3 text-dark-4 hover:text-red"><Trash2 className="size-4" /> Delete</Button>}</div></li>)}</ul>}</CardContent></Card>
      </div>
    </div>
  </AdminShell>;
}
