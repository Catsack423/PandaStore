"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import { Plus, RefreshCw, Tags, Trash2 } from "lucide-react";
import toast from "react-hot-toast";
import AdminShell from "./AdminShell";
import { adminRequest } from "./api";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
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
    if (!categoryName || saving) return;
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
      <div className="space-y-5">
        {error && (
          <p role="alert" className="rounded-lg border border-red/20 bg-red-light-6 px-4 py-3 text-sm text-red">
            {error}
          </p>
        )}
        <div className="grid items-start gap-5 xl:grid-cols-[minmax(0,1fr)_300px]">
          <Card className="min-w-0 border-gray-3 bg-white shadow-1 xl:order-2">
            <CardHeader className="border-b border-gray-3 p-5 sm:p-6">
              <p className="text-xs font-semibold uppercase tracking-[0.16em] text-blue">Catalog setup</p>
              <CardTitle className="mt-1 text-lg text-dark">Add a category</CardTitle>
              <p className="text-sm text-dark-4">Names and descriptions come from the catalog API.</p>
            </CardHeader>
            <CardContent className="p-5 sm:p-6">
              <form onSubmit={(event) => void add(event)} className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="category-name">Category name</Label>
                  <Input
                    id="category-name"
                    required
                    maxLength={100}
                    value={name}
                    onChange={(event) => setName(event.target.value)}
                    placeholder="e.g. Home office"
                    className="border-gray-3"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="category-description">
                    Description <span className="font-normal text-dark-4">(optional)</span>
                  </Label>
                  <Textarea
                    id="category-description"
                    maxLength={255}
                    value={description}
                    onChange={(event) => setDescription(event.target.value)}
                    placeholder="Help shoppers understand this category"
                    className="min-h-24 border-gray-3"
                  />
                  <p className="text-right text-xs text-dark-4">{description.length}/255</p>
                </div>
                <Button
                  type="submit"
                  disabled={saving || !name.trim()}
                  className="h-10 w-full bg-blue text-white hover:bg-blue-dark"
                >
                  <Plus className="size-4" /> {saving ? "Adding…" : "Add category"}
                </Button>
              </form>
            </CardContent>
          </Card>
          <Card className="min-w-0 border-gray-3 bg-white shadow-1 xl:order-1">
            <CardHeader className="flex flex-row flex-wrap items-center justify-between gap-3 border-b border-gray-3 p-5 sm:p-6">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.16em] text-blue">Marketplace catalog</p>
                <CardTitle className="mt-1 text-lg text-dark">
                  {loading ? "Loading categories…" : `${categories.length} ${categories.length === 1 ? "category" : "categories"}`}
                </CardTitle>
              </div>
              <Button
                type="button"
                variant="outline"
                onClick={() => void reload()}
                disabled={loading || deletingId !== null}
                className="border-gray-3 text-dark"
              >
                <RefreshCw className="size-4" /> Refresh
              </Button>
            </CardHeader>
            <CardContent className="p-3 sm:p-4">
              {!loading && categories.length === 0 ? (
                <div className="py-14 text-center">
                  <Tags className="mx-auto size-8 text-blue" />
                  <p className="mt-3 font-medium text-dark">No categories yet</p>
                  <p className="mt-1 text-sm text-dark-4">Add the first one with the form.</p>
                </div>
              ) : (
                <ul className="divide-y divide-gray-3">
                  {categories.map((category) => (
                    <li
                      key={category.categoryId}
                      className="flex flex-col gap-3 px-2 py-4 sm:flex-row sm:items-start sm:justify-between"
                    >
                      <div className="min-w-0">
                        <h3 className="font-semibold text-dark">{category.categoryName}</h3>
                        <p className="mt-1 break-words text-sm leading-5 text-dark-4">
                          {category.description || "No description"}
                        </p>
                        <p className="mt-2 text-xs text-dark-4">ID {category.categoryId}</p>
                      </div>
                      <div className="shrink-0">
                        <Button
                          type="button"
                          size="sm"
                          variant="outline"
                          onClick={() => setCategoryToDelete(category)}
                          disabled={deletingId !== null}
                          className="border-gray-3 text-dark-4 hover:border-red/40 hover:bg-red/5 hover:text-red transition-colors"
                        >
                          <Trash2 className="size-4" /> Delete
                        </Button>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </CardContent>
          </Card>
        </div>
      </div>

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
