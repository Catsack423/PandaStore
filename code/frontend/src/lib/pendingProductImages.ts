"use client";

type PendingImage = { key: string; removalToken: string; userId: string; formId: string; submittedAt?: number; lastSeen?: number };
const storageKey = "seller-product-images-pending-v1";
const endpoint = "/api/seller-product-images/cleanup";
const activeForms = new Set<string>();
const inFlight = new Set<string>();
const lockPrefix = "seller-product-image-form:";

function read(): PendingImage[] {
  try {
    const legacy: unknown = JSON.parse(localStorage.getItem(storageKey) || "[]");
    const images = new Map<string, PendingImage>();
    const add = (image: PendingImage) => {
      if (typeof image?.key === "string" && typeof image?.removalToken === "string" && typeof image?.userId === "string" && typeof image?.formId === "string") images.set(image.key, image);
    };
    if (Array.isArray(legacy)) legacy.forEach(add);
    for (let index = 0; index < localStorage.length; index++) {
      const key = localStorage.key(index);
      if (!key?.startsWith(storageKey + ":")) continue;
      try { add(JSON.parse(localStorage.getItem(key) || "null")); } catch { /* Ignore a damaged record. */ }
    }
    return Array.from(images.values());
  } catch { return []; }
}
function write(image: PendingImage) {
  // Separate records prevent two tabs from overwriting each other's upload queue.
  try { localStorage.setItem(storageKey + ":" + image.key, JSON.stringify(image)); } catch { /* Keep page-exit cleanup available when storage is disabled. */ }
}
export function activateProductImageForm(formId: string) {
  activeForms.add(formId);
  let release = () => {};
  if (navigator.locks) {
    const lifetime = new Promise<void>(resolve => { release = resolve; });
    void navigator.locks.request(lockPrefix + formId, () => lifetime).catch(() => {});
  }
  const heartbeat = window.setInterval(() => {
    const images = read();
    images.filter(image => image.formId === formId).forEach(image => write({ ...image, lastSeen: Date.now() }));
  }, 15000);
  return () => { activeForms.delete(formId); release(); window.clearInterval(heartbeat); };
}
export function rememberProductImage(image: PendingImage) {
  write({ ...image, lastSeen: Date.now() });
}
export function forgetProductImage(key: string) {
  try {
    localStorage.removeItem(storageKey + ":" + key);
    const legacy = JSON.parse(localStorage.getItem(storageKey) || "[]");
    if (Array.isArray(legacy) && legacy.some(image => image?.key === key)) localStorage.setItem(storageKey, JSON.stringify(legacy.filter(image => image?.key !== key)));
  } catch { /* A later cleanup can safely retry the same key. */ }
}
export function markProductImagesSubmitted(keys: string[]) {
  read().filter(image => keys.includes(image.key)).forEach(image => write({ ...image, submittedAt: Date.now() }));
}

export async function cleanupPendingProductImages(userId: string, options: { formId?: string; beacon?: boolean; images?: PendingImage[] } = {}) {
  let heldForms: Set<string> | null = null;
  if (!options.formId && navigator.locks) {
    try { heldForms = new Set((await navigator.locks.query()).held?.map(lock => lock.name || "")); } catch { /* Use the persisted heartbeat fallback. */ }
  }
  // Allow an in-flight product save to settle. The server also checks saved products.
  const images = (options.images || read()).filter(image => image.userId === userId &&
    (options.formId ? image.formId === options.formId : !activeForms.has(image.formId)) &&
    (options.formId || (heldForms ? !heldForms.has(lockPrefix + image.formId) : Date.now() - (image.lastSeen || 0) >= 300000)) &&
    (!image.submittedAt || Date.now() - image.submittedAt >= 30000) && !inFlight.has(image.key));
  for (let offset = 0; offset < images.length; offset += 5) {
    const batch = images.slice(offset, offset + 5);
    const body = JSON.stringify(batch.map(({ key, removalToken }) => ({ key, removalToken })));
    if (options.beacon) {
      try { if (navigator.sendBeacon(endpoint, new Blob([body], { type: "application/json" }))) continue; } catch { /* Fall back to keepalive. */ }
    }
    batch.forEach(image => inFlight.add(image.key));
    try {
      const response = await fetch(endpoint, { method: "POST", headers: { "Content-Type": "application/json" }, body, credentials: "same-origin", keepalive: true });
      if (!response.ok) continue;
      const result = await response.json();
      for (const item of result.results || []) {
        if (batch.some(image => image.key === item.key) && (item.success || item.error === "IMAGE_IN_USE")) forgetProductImage(item.key);
      }
    } catch { /* Retry persisted records on the next authenticated visit or online event. */ }
    finally { batch.forEach(image => inFlight.delete(image.key)); }
  }
}
