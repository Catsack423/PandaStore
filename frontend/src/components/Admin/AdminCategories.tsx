"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import { Plus, RefreshCw, Tags, Trash2 } from "lucide-react";
import toast from "react-hot-toast";
import AdminShell from "./AdminShell";
import { adminRequest } from "./api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import ConfirmDialog from "@/components/Common/ConfirmDialog";

type Category = { categoryId: number; categoryName: string; description: string | null };

export default function AdminCategories() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [categoryToDelete, setCategoryToDelete] = useState<Category | null>(null);
  const [error, setError] = useState("");

  const reload = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setCategories(await adminRequest<Category[]>("categories"));
    } catch (cause) {
      const msg = cause instanceof Error ? cause.message : "Could not load categories";
      setError(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void reload();
  }, [reload]);

  async function add(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const categoryName = name.trim();
    if (!categoryName || categoryName.length > 20 || saving) return;
    setSaving(true);
    setError("");
    try {
      await adminRequest<Category>("categories", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ categoryName, description: description.trim() }),
      });
      setName("");
      setDescription("");
      toast.success(`Category "${categoryName}" created successfully!`);
      await reload();
    } catch (cause) {
      const msg = cause instanceof Error ? cause.message : "Could not add category";
      setError(msg);
      toast.error(msg);
    } finally {
      setSaving(false);
    }
  }

  async function remove(category: Category) {
    if (deletingId !== null) return;
    setDeletingId(category.categoryId);
    setError("");
    try {
      await adminRequest<void>(`categories/${category.categoryId}`, { method: "DELETE" });
      toast.success(`Category "${category.categoryName}" deleted successfully!`);
      setCategoryToDelete(null);
      await reload();
    } catch (cause) {
      const msg = cause instanceof Error ? cause.message : "Could not delete category";
      setError(msg);
      toast.error(msg);
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <AdminShell title="Categories">
      <section aria-labelledby="categories-heading" className="overflow-hidden rounded-2xl border border-gray-3 bg-white shadow-1">
        <header className="flex flex-col gap-4 border-b border-gray-3 px-5 py-6 sm:flex-row sm:items-center sm:justify-between sm:px-7">
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-3">
              <h2 id="categories-heading" className="text-xl font-semibold text-dark">Product categories</h2>
              <span aria-live="polite" className="rounded-full bg-blue/10 px-3 py-1 text-xs font-semibold text-blue">
                {loading ? "Loading…" : `${categories.length} total`}
              </span>
            </div>
            <p className="mt-2 text-sm text-dark-4">Organize products so shoppers can find what they need.</p>
          </div>
          <Button type="button" variant="outline" onClick={() => void reload()} disabled={loading || deletingId !== null} className="h-10 shrink-0 border-gray-3 bg-white px-3 text-dark hover:bg-gray-1">
            <RefreshCw className={`size-4 ${loading ? "animate-spin" : ""}`} /> Refresh
          </Button>
        </header>

        {error && <p role="alert" className="mx-5 mt-5 rounded-lg border border-red/20 bg-red-light-6 px-4 py-3 text-sm text-red sm:mx-7">{error}</p>}

        <div className="grid lg:grid-cols-[minmax(0,1fr)_320px]">
          <div className="order-2 min-w-0 lg:order-1">
            <div className="border-b border-gray-3 px-5 py-4 sm:px-7">
              <h3 className="font-semibold text-dark">All categories</h3>
            </div>
            {loading && categories.length === 0 ? (
              <p role="status" className="px-5 py-14 text-center text-sm text-dark-4">Loading categories…</p>
            ) : categories.length === 0 && !error ? (
              <div className="px-5 py-14 text-center">
                <div className="mx-auto flex size-12 items-center justify-center rounded-full bg-blue/10 text-blue"><Tags className="size-6" /></div>
                <p className="mt-4 font-medium text-dark">No categories yet</p>
                <p className="mt-1 text-sm text-dark-4">Add the first category using the form.</p>
              </div>
            ) : categories.length > 0 ? (
              <ul className="divide-y divide-gray-3">
                {categories.map((category) => (
                  <li key={category.categoryId} className="flex flex-col gap-4 px-5 py-5 sm:flex-row sm:items-center sm:justify-between sm:px-7">
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1">
                        <h4 className="break-words font-semibold text-dark">{category.categoryName}</h4>
                        <span className="text-xs text-dark-4">#{category.categoryId}</span>
                      </div>
                      <p className="mt-1 break-words text-sm leading-6 text-dark-4">{category.description || "No description"}</p>
                    </div>
                    <Button type="button" variant="outline" onClick={() => setCategoryToDelete(category)} disabled={deletingId !== null} aria-label={`Delete ${category.categoryName}`} className="h-9 shrink-0 self-start rounded-lg border-red/20 bg-white px-3 text-red hover:border-red/40 hover:bg-red/5 hover:text-red sm:self-auto">
                      <Trash2 className="size-4" /> Delete
                    </Button>
                  </li>
                ))}
              </ul>
            ) : null}
          </div>

          <aside aria-labelledby="add-category-heading" className="order-1 border-b border-gray-3 p-5 sm:p-7 lg:order-2 lg:border-b-0 lg:border-l">
            <h3 id="add-category-heading" className="font-semibold text-dark">Add a category</h3>
            <p className="mt-1 text-sm leading-6 text-dark-4">Give shoppers a clear name for this group of products.</p>
            <form onSubmit={(event) => void add(event)} className="mt-6 space-y-5">
              <div className="space-y-2">
                <Label htmlFor="category-name">Category name</Label>
                <Input id="category-name" required maxLength={20} value={name} onChange={(event) => setName(event.target.value)} placeholder="e.g. Home office" aria-describedby="category-name-limit" className="border-gray-3 bg-white" />
                <p id="category-name-limit" className="text-right text-xs text-dark-4">{name.length}/20 characters</p>
              </div>
              <div className="space-y-2">
                <Label htmlFor="category-description">Description <span className="font-normal text-dark-4">(optional)</span></Label>
                <Textarea id="category-description" maxLength={255} value={description} onChange={(event) => setDescription(event.target.value)} placeholder="What belongs in this category?" className="min-h-24 border-gray-3 bg-white" />
                <p className="text-right text-xs text-dark-4">{description.length}/255</p>
              </div>
              <Button type="submit" disabled={saving || !name.trim()} className="h-10 w-full bg-blue text-white hover:bg-blue-dark">
                <Plus className="size-4" /> {saving ? "Adding…" : "Add category"}
              </Button>
            </form>
          </aside>
        </div>
      </section>

      <ConfirmDialog
        isOpen={Boolean(categoryToDelete)}
        onClose={() => setCategoryToDelete(null)}
        onConfirm={() => {
          if (categoryToDelete) void remove(categoryToDelete);
        }}
        title={`Delete "${categoryToDelete?.categoryName}"?`}
        description="Are you sure you want to delete this category? Products associated with this category may need to be updated. This action cannot be undone."
        confirmText="Delete category"
        cancelText="Cancel"
        variant="danger"
        icon="trash"
        loading={deletingId !== null}
      />
    </AdminShell>
  );
}
