"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { ImagePlus, LoaderCircle, RotateCcw, Trash2 } from "lucide-react";
import Breadcrumb from "@/components/Common/Breadcrumb";
import ProductImage, { ProductImageGallery } from "@/components/Common/ProductImage";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { createSellerProduct, getCategories, removeSellerProductImage } from "@/ServerAction/products";
import { ALLOWED_IMAGE_TYPES, MAX_IMAGE_SIZE, uploadSingleImage } from "@/lib/uploadImage";
import type { Category } from "@/types/category";
import { activateProductImageForm, cleanupPendingProductImages, forgetProductImage, markProductImagesSubmitted, rememberProductImage } from "@/lib/pendingProductImages";

type ImageEntry = { id: string; file: File; preview: string; url?: string; key?: string; removalToken?: string; userId?: string; submittedAt?: number; status: "queued" | "uploading" | "uploaded" | "error"; error?: string };

export default function SellerAddProduct() {
  const router = useRouter();
  const [categories, setCategories] = useState<Category[]>([]);
  const [categoryLoading, setCategoryLoading] = useState(true);
  const [categoryError, setCategoryError] = useState("");
  const [search, setSearch] = useState("");
  const [categoryIds, setCategoryIds] = useState<number[]>([]);
  const [images, setImages] = useState<ImageEntry[]>([]);
  const [primaryId, setPrimaryId] = useState("");
  const [previewId, setPreviewId] = useState("");
  const [uploading, setUploading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [removingId, setRemovingId] = useState("");
  const [error, setError] = useState("");
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [price, setPrice] = useState("");
  const [stock, setStock] = useState("");
  const [shippingInfo, setShippingInfo] = useState("");
  const imagesRef = useRef<ImageEntry[]>([]);
  const uploadLock = useRef(false);
  const saveLock = useRef(false);
  const removeLock = useRef(false);
  const categoryLock = useRef(false);
  const mounted = useRef(true);
  const previews = useRef(new Set<string>());
  const fileInput = useRef<HTMLInputElement>(null);
  const formId = useRef(crypto.randomUUID());
  const committed = useRef(false);
  const uploadGeneration = useRef(0);

  function updateImages(next: ImageEntry[]) { imagesRef.current = next; setImages(next); }

  async function loadCategories() {
    if (categoryLock.current) return;
    categoryLock.current = true;
    setCategoryLoading(true);
    setCategoryError("");
    try {
      const result = await getCategories({ throwOnError: true });
      if (mounted.current) {
        setCategories(result);
        setCategoryIds(ids => ids.filter(id => result.some(category => category.id === id)));
      }
    } catch {
      if (mounted.current) setCategoryError("Categories could not be loaded. Please try again.");
    } finally {
      categoryLock.current = false;
      if (mounted.current) setCategoryLoading(false);
    }
  }

  useEffect(() => {
    mounted.current = true;
    void loadCategories();
    const urls = previews.current;
    return () => { mounted.current = false; urls.forEach(url => URL.revokeObjectURL(url)); urls.clear(); };
  }, []);

  useEffect(() => {
    const deactivate = activateProductImageForm(formId.current);
    function discard(beacon: boolean) {
      if (committed.current) return;
      const images = imagesRef.current.flatMap(image => image.key && image.removalToken && image.userId ? [{ key: image.key, removalToken: image.removalToken, userId: image.userId, formId: formId.current, submittedAt: image.submittedAt }] : []);
      if (images[0]) void cleanupPendingProductImages(images[0].userId, { formId: formId.current, beacon, images });
    }
    const stopUploads = () => { uploadGeneration.current++; };
    const onPageHide = () => { stopUploads(); discard(true); };
    const onPageShow = (event: PageTransitionEvent) => {
      if (!event.persisted || committed.current) return;
      discard(false);
      imagesRef.current.forEach(image => URL.revokeObjectURL(image.preview));
      previews.current.clear();
      updateImages([]);
      setPrimaryId("");
      setPreviewId("");
      setError("Choose your photos again after returning to this page.");
    };
    window.addEventListener("pagehide", onPageHide);
    window.addEventListener("pageshow", onPageShow);
    return () => {
      deactivate();
      stopUploads();
      window.removeEventListener("pagehide", onPageHide);
      window.removeEventListener("pageshow", onPageShow);
      discard(false);
    };
  }, []);

  async function upload(entries: ImageEntry[]) {
    if (uploadLock.current || saveLock.current || removeLock.current) return;
    uploadLock.current = true;
    const generation = uploadGeneration.current;
    setUploading(true);
    try {
      for (const entry of entries) {
        if (!mounted.current || generation !== uploadGeneration.current) break;
        updateImages(imagesRef.current.map(image => image.id === entry.id ? { ...image, status: "uploading", error: undefined } : image));
        try {
          const result = await uploadSingleImage(entry.file, "productImage");
          if (result.removalToken && result.userId) rememberProductImage({ key: result.key, removalToken: result.removalToken, userId: result.userId, formId: formId.current });
          if (!mounted.current || generation !== uploadGeneration.current) {
            if (result.removalToken && result.userId) void cleanupPendingProductImages(result.userId, { formId: formId.current, images: [{ key: result.key, removalToken: result.removalToken, userId: result.userId, formId: formId.current }] });
            break;
          }
          updateImages(imagesRef.current.map(image => image.id === entry.id ? { ...image, ...result } : image));
          if (result.url.length > 255) throw new Error("Image URL is too long. Please try another image.");
          if (mounted.current) updateImages(imagesRef.current.map(image => image.id === entry.id ? { ...image, status: "uploaded", url: result.url } : image));
        } catch (failure) {
          if (mounted.current && generation === uploadGeneration.current) updateImages(imagesRef.current.map(image => image.id === entry.id ? { ...image, status: "error", error: failure instanceof Error ? failure.message : "Upload failed. Please try again." } : image));
        }
      }
    } finally {
      uploadLock.current = false;
      if (mounted.current) setUploading(false);
    }
  }

  function addImages(files: FileList | null) {
    if (!files || uploadLock.current || saveLock.current || removeLock.current) return;
    const selected = Array.from(files);
    if (imagesRef.current.length + selected.length > 5) { setError("Choose no more than 5 product images."); return; }
    if (selected.some(file => !ALLOWED_IMAGE_TYPES.includes(file.type as typeof ALLOWED_IMAGE_TYPES[number]) || file.size > MAX_IMAGE_SIZE)) {
      setError("Choose JPG, PNG, or WebP images up to 4 MB each."); return;
    }
    setError("");
    const entries: ImageEntry[] = selected.map(file => {
      const preview = URL.createObjectURL(file);
      previews.current.add(preview);
      return { id: crypto.randomUUID(), file, preview, status: "queued" };
    });
    updateImages([...imagesRef.current, ...entries]);
    if (!primaryId && entries[0]) { setPrimaryId(entries[0].id); setPreviewId(entries[0].id); }
    void upload(entries);
  }

  async function removeImage(id: string) {
    if (uploadLock.current || saveLock.current || removeLock.current) return;
    const entry = imagesRef.current.find(image => image.id === id);
    if (!entry) return;
    removeLock.current = true;
    setRemovingId(id);
    setError("");
    try {
      if (entry.url || entry.key) {
        if (!entry.key || !entry.removalToken) throw new Error("This photo was uploaded before Cloud removal was enabled. Reload the page and upload it again.");
        const result = await removeSellerProductImage({ key: entry.key, removalToken: entry.removalToken });
        if (!result.success) throw new Error(result.message);
        forgetProductImage(entry.key);
      }
      if (!mounted.current) return;
      URL.revokeObjectURL(entry.preview);
      previews.current.delete(entry.preview);
      const remaining = imagesRef.current.filter(image => image.id !== id);
      updateImages(remaining);
      const nextPrimaryId = primaryId === id ? remaining[0]?.id || "" : primaryId;
      if (primaryId === id) setPrimaryId(nextPrimaryId);
      if (previewId === id) setPreviewId(nextPrimaryId);
    } catch (failure) {
      if (mounted.current) setError(failure instanceof Error ? failure.message : "Unable to remove image. Please try again.");
    } finally {
      removeLock.current = false;
      if (mounted.current) setRemovingId("");
    }
  }

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (saveLock.current || uploadLock.current || removeLock.current) return;
    setError("");
    const entries = imagesRef.current;
    if (!entries.length || entries.some(image => image.status !== "uploaded" || !image.url)) { setError("Upload at least one image and retry or remove failed images."); return; }
    if (!categoryIds.length || categoryLoading || categoryError) { setError("Select at least one category."); return; }
    if (!/^\d{1,10}(\.\d{1,2})?$/.test(price) || Number(price) < 0.01 || Number(price) > 9999999999.99) { setError("Enter a price from 0.01 to 9,999,999,999.99 with up to two decimal places."); return; }
    saveLock.current = true;
    setSaving(true);
    const keys = entries.flatMap(image => image.key ? [image.key] : []);
    markProductImagesSubmitted(keys);
    updateImages(entries.map(image => ({ ...image, submittedAt: Date.now() })));
    try {
      const ordered = [...entries.filter(image => image.id === primaryId), ...entries.filter(image => image.id !== primaryId)];
      const result = await createSellerProduct({ name, description, price, stock: Number(stock), shippingInfo, categoryIds, imageUrls: ordered.map(image => image.url!) });
      if (!result.success) throw new Error(result.message);
      committed.current = true;
      keys.forEach(forgetProductImage);
      router.push(`/shop/${result.data.sellerId}`);
      router.refresh();
    } catch (failure) {
      if (mounted.current) { setError(failure instanceof Error ? failure.message : "Unable to add product. Please try again."); setSaving(false); }
      saveLock.current = false;
      if (mounted.current) void loadCategories();
    }
  }

  const busy = uploading || saving || !!removingId;
  const ready = images.length > 0 && images.every(image => image.status === "uploaded") && categoryIds.length > 0 && categories.length > 0 && !categoryLoading && !categoryError;
  const filtered = categories.filter(category => category.name?.toLowerCase().includes(search.toLowerCase()));
  const fieldClass = "mt-2 bg-white";
  const orderedImages = [...images.filter(image => image.id === primaryId), ...images.filter(image => image.id !== primaryId)];
  const previewImages = orderedImages.map(image => ({ id: image.id, src: image.preview }));

  return <main>
    <Breadcrumb title="Add Product" pages={["Add Product"]} rootHref="/seller-dashboard" />
    <section className="bg-gray-2 py-10 sm:py-14">
      <form onSubmit={submit} className="mx-auto max-w-[1170px] px-4 sm:px-8 xl:px-0" aria-label="Add product">
        {error && <p role="alert" className="mb-6 rounded-lg border border-red/20 bg-white p-4 text-sm text-red">{error}</p>}
        <fieldset disabled={busy} className="min-w-0">
          <Card className="min-w-0 p-5 sm:p-7">
            <h3 className="mb-6 text-lg font-semibold">Product details</h3>
            <section className="mb-6 border-b border-gray-3 pb-6" aria-labelledby="image-preview-title">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <h4 id="image-preview-title" className="text-lg font-semibold">Image preview *</h4>
                <Button type="button" variant="outline" className="h-11 w-full gap-2 border-gray-3 text-blue sm:w-auto" disabled={busy || images.length >= 5} onClick={() => fileInput.current?.click()}><ImagePlus aria-hidden="true" />Choose images</Button>
              </div>
              <p id="image-help" className="mt-2 text-sm text-dark-4">1–5 photos. JPG, PNG, or WebP, up to 4 MB each. Your main photo appears first.</p>
              <input ref={fileInput} type="file" multiple accept={ALLOWED_IMAGE_TYPES.join(",")} className="sr-only" tabIndex={-1} aria-label="Product images" aria-describedby="image-help" onChange={event => { addImages(event.target.files); event.target.value = ""; }} />
              {previewImages.length ? <div className="mt-4 grid items-start gap-3 md:grid-cols-[minmax(0,2fr)_minmax(0,1fr)] md:gap-5">
                <ProductImageGallery images={previewImages} activeImageId={previewId} onActiveImageChange={setPreviewId} alt={name.trim() || "Product image preview"} sizes="(min-width: 1280px) 730px, (min-width: 768px) 65vw, 100vw" compact />
                <div className="min-w-0 space-y-3" aria-live="polite">
                  {orderedImages.map((image, index) => <div key={image.id} className={`flex min-w-0 gap-3 rounded-lg border p-3 ${image.id === primaryId ? "border-blue/40 bg-blue/5" : "border-gray-3"}`}>
                    <ProductImage src={image.preview} alt={`Product photo ${index + 1}`} size="sm" />
                    <div className="min-w-0 flex-1"><p className="truncate text-sm font-medium" title={image.file.name}>{image.file.name}</p>{image.id === primaryId && <span className="mt-1 inline-block rounded bg-blue px-1.5 py-0.5 text-[10px] font-medium text-white">Main</span>}<p className={`mt-1 text-xs ${image.status === "error" ? "text-red" : "text-dark-4"}`}>{image.status === "uploaded" ? "Uploaded" : image.status === "error" ? image.error : image.status === "uploading" ? "Uploading…" : "Waiting to upload"}</p>
                      <label className="mt-2 flex items-center gap-2 text-xs"><input type="radio" name="primaryImage" className="size-4 accent-blue focus-visible:ring-2 focus-visible:ring-blue" checked={primaryId === image.id} onChange={() => { setPrimaryId(image.id); setPreviewId(image.id); }} aria-label={`Use photo ${index + 1} as main image`} />Main image</label>
                      <div className="mt-2 flex flex-wrap gap-2">{image.status === "error" && !image.key && <Button type="button" variant="ghost" size="sm" onClick={() => void upload([image])} aria-label={`Retry photo ${index + 1}`} className="gap-1 text-blue"><RotateCcw aria-hidden="true" />Retry</Button>}<Button type="button" variant="ghost" size="sm" onClick={() => void removeImage(image.id)} aria-label={`Remove photo ${index + 1}`} className="gap-1 text-dark-4">{removingId === image.id ? <LoaderCircle className="animate-spin motion-reduce:animate-none" aria-hidden="true" /> : <Trash2 aria-hidden="true" />}{removingId === image.id ? "Removing…" : "Remove"}</Button></div>
                    </div>
                  </div>)}
                </div>
              </div> : <p className="mt-3 rounded-lg bg-[#F6F7FB] px-4 py-3 text-sm text-dark-4">Choose images to preview your product.</p>}
            </section>
            <div className="space-y-6">
              <div><Label htmlFor="product-name">Product name *</Label><Input className={fieldClass} id="product-name" name="name" value={name} onChange={event => setName(event.target.value)} required maxLength={200} placeholder="Enter product name" /></div>
              <div><Label htmlFor="product-description">Description</Label><Textarea className={`${fieldClass} min-h-[160px]`} id="product-description" name="description" value={description} onChange={event => setDescription(event.target.value)} placeholder="Describe your product" /></div>
              <div className="grid gap-6 sm:grid-cols-2">
                <div><Label htmlFor="product-price">Price *</Label><Input className={fieldClass} id="product-price" name="price" type="number" inputMode="decimal" min="0.01" max="9999999999.99" step="0.01" required value={price} onChange={event => setPrice(event.target.value)} placeholder="0.00" /></div>
                <div><Label htmlFor="product-stock">Stock quantity *</Label><Input className={fieldClass} id="product-stock" name="stock" type="number" inputMode="numeric" min="1" max="2147483647" step="1" required value={stock} onChange={event => setStock(event.target.value)} placeholder="1" /></div>
              </div>
              <div><Label htmlFor="product-shipping">Shipping information</Label><Textarea className={fieldClass} id="product-shipping" name="shippingInfo" maxLength={255} value={shippingInfo} onChange={event => setShippingInfo(event.target.value)} placeholder="Delivery times and shipping details" /><p className="mt-2 text-xs text-dark-4">Up to 255 characters</p></div>
              <div className="border-t border-gray-3 pt-6">
                <h3 className="text-lg font-semibold">Categories *</h3><p className="mt-2 text-sm text-dark-4">Select one or more categories.</p>
                {categoryLoading ? <p role="status" className="mt-5 flex items-center gap-2 text-sm text-dark-4"><LoaderCircle className="size-4 animate-spin motion-reduce:animate-none" aria-hidden="true" />Loading categories…</p> : categoryError ? <div className="mt-5"><p role="alert" className="text-sm text-red">{categoryError}</p><Button type="button" variant="outline" className="mt-3 text-blue" onClick={() => void loadCategories()}>Retry categories</Button></div> : !categories.length ? <p role="status" className="mt-5 text-sm text-dark-4">No categories available. Please wait for an Admin to add a category before saving.</p> : <>
                  <Label htmlFor="category-search" className="sr-only">Search categories</Label><Input id="category-search" type="search" value={search} onChange={event => setSearch(event.target.value)} placeholder="Search categories" className="mt-5 bg-white" />
                  <div className="mt-3 max-h-60 overflow-y-auto" role="group" aria-label="Product categories">{filtered.map(category => <label key={category.id} className="flex cursor-pointer items-center gap-3 rounded-lg px-1 py-3 text-sm"><input type="checkbox" className="size-4 shrink-0 accent-blue focus-visible:ring-2 focus-visible:ring-blue" checked={categoryIds.includes(category.id!)} onChange={event => setCategoryIds(ids => event.target.checked ? [...ids, category.id!] : ids.filter(id => id !== category.id))} />{category.name}</label>)}{!filtered.length && <p className="py-3 text-sm text-dark-4">No matching categories.</p>}</div><p className="mt-3 border-t border-gray-3 pt-3 text-xs text-dark-4">{categoryIds.length} selected</p>
                </>}
              </div>
            </div>
          </Card>
        </fieldset>
        <div className="mt-7 flex flex-wrap items-center justify-end gap-3 border-t border-gray-3 pt-6">
          <p role="status" className="mr-auto text-sm text-dark-4">{saving ? "Adding product…" : uploading ? "Uploading images…" : removingId ? "Removing image from Cloud…" : ""}</p>
          <Button type="button" variant="outline" className="h-11 border-gray-3 bg-white px-6 text-dark" disabled={busy} onClick={() => router.push("/seller-dashboard")}>Cancel</Button>
          <Button type="submit" className="h-11 gap-2 bg-blue px-6 text-white hover:bg-blue-dark" disabled={busy || !ready}>{saving && <LoaderCircle className="size-4 animate-spin motion-reduce:animate-none" aria-hidden="true" />}Add product</Button>
        </div>
      </form>
    </section>
  </main>;
}
