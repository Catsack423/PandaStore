const { test } = require("node:test");
const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const { resolve } = require("node:path");
const ts = require("typescript");

function load(file, mocks) {
  const source = readFileSync(resolve(__dirname, "../src", file), "utf8");
  const compiled = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, jsx: ts.JsxEmit.ReactJSX } }).outputText;
  const exports = {};
  new Function("require", "exports", compiled)(name => mocks[name] ?? require(name), exports);
  return exports;
}

process.env.UPLOADTHING_TOKEN = "test-only-uploadthing-token";
const removalTokens = load("lib/productImageRemoval.ts", { "server-only": {} });

function actions({ role = "SELLER", status = "ACTIVE", shopStatus = "ACTIVE", token = "session", userId = 7, deleteResult = { success: true }, deleteError = false, savedImages = [] } = {}) {
  const calls = [], paths = [], deletions = [];
  const api = load("ServerAction/products.ts", {
    "@/types/product": {}, "@/lib/templateShops": {}, "@/lib/templateCategories": {},
    "next/headers": { cookies: async () => ({ get: () => token ? { value: token } : undefined }) },
    "next/cache": { revalidatePath: path => paths.push(path) },
    "@/lib/productImageRemoval": removalTokens,
    "uploadthing/server": { UTApi: class { async deleteFiles(key) { deletions.push(key); if (deleteError) throw new Error("Offline"); return deleteResult; } } },
  });
  const fetch = async (url, options) => {
    calls.push({ url, options });
    const data = url.endsWith("/api/auth/me") ? { userId, role, status } : url.includes("/shops/user/") ? { sellerId: 12, status: shopStatus } : {
      productId: 8, sellerId: 12, sellerShopName: "Shop", name: "Item", description: "", price: 1.25, stock: 3,
      reviewCount: 0, imageUrls: ["https://example.com/main.png", "https://example.com/other.png"], categoryIds: [2, 3], status: "ACTIVE",
    };
    return { ok: true, json: async () => ({ success: true, data: url.includes("/api/products/seller/") ? savedImages.length ? [{ ...data, imageUrls: savedImages }] : [] : data }) };
  };
  return { api, fetch, calls, paths, deletions };
}
const input = { name: " Item ", description: "", price: "1.25", stock: 3, shippingInfo: "", categoryIds: [2, 3, 2], imageUrls: ["https://example.com/main.png", "https://example.com/other.png"] };
async function usingFetch(fetch, run) { const previous = global.fetch; global.fetch = fetch; try { await run(); } finally { global.fetch = previous; } }

test("creation derives the shop from the session, forwards token and categories, preserves primary, and revalidates", async () => {
  const mock = actions();
  await usingFetch(mock.fetch, async () => {
    const result = await mock.api.createSellerProduct({ ...input, sellerId: 999 });
    assert.equal(result.success, true);
    const request = mock.calls.at(-1);
    assert.match(request.url, /api\/products\?sellerId=12$/);
    assert.equal(request.options.headers.Authorization, "Bearer session");
    const body = JSON.parse(request.options.body);
    assert.equal(body.sellerId, undefined);
    assert.deepEqual(body.categoryIds, [2, 3]);
    assert.deepEqual(body.imageUrls, input.imageUrls);
    assert.equal(body.name, "Item");
    assert.deepEqual(mock.paths, ["/shop/12"]);
    assert.equal(result.data.imgs.previews[0], input.imageUrls[0]);
  });
});

test("anonymous, Customer, Admin, inactive account and inactive shop cannot create", async () => {
  for (const options of [{ token: null }, { role: "CUSTOMER" }, { role: "ADMIN" }, { status: "SUSPENDED" }, { shopStatus: "SUSPENDED" }]) {
    const mock = actions(options);
    await usingFetch(mock.fetch, async () => {
      assert.equal((await mock.api.createSellerProduct(input)).success, false);
      assert.equal(await mock.api.getSellerProductAccess(), false);
      assert.ok(mock.calls.every(call => call.options.method !== "POST"));
    });
  }
});

test("rejects invalid product fields and images before writing to backend", async () => {
  for (const invalid of [{ name: " " }, { name: "x".repeat(201) }, { price: "0" }, { price: "1.234" }, { price: "10000000000" }, { stock: 0 }, { stock: 1.5 }, { stock: 2147483648 }, { shippingInfo: "x".repeat(256) }, { categoryIds: [] }, { imageUrls: [] }, { imageUrls: Array(6).fill(input.imageUrls[0]) }, { imageUrls: ["https://example.com/" + "x".repeat(256)] }]) {
    const mock = actions();
    await usingFetch(mock.fetch, async () => {
      assert.equal((await mock.api.createSellerProduct({ ...input, ...invalid })).success, false);
      assert.equal(mock.calls.length, 0);
    });
  }
});

test("deleted category or network error returns failure without cache invalidation", async () => {
  for (const network of [false, true]) {
    const mock = actions();
    await usingFetch(async (url, options) => {
      if (options.method === "POST") {
        if (network) throw new Error("Network unavailable");
        return { ok: false, json: async () => ({ success: false, message: "Category no longer exists" }) };
      }
      return mock.fetch(url, options);
    }, async () => {
      assert.equal((await mock.api.createSellerProduct(input)).success, false);
      assert.deepEqual(mock.paths, []);
    });
  }
});

test("category strict mode reports failure while existing callers keep empty results", async () => {
  const mock = actions();
  await usingFetch(async () => ({ ok: false }), async () => {
    assert.deepEqual(await mock.api.getCategories(), []);
    await assert.rejects(mock.api.getCategories({ throwOnError: true }));
  });
});

test("productImage checks role, active shop, file type and size; sellerDocument remains Customer only", async () => {
  const endpoints = load("app/api/uploadthing/core.ts", {
    "@/lib/productImageRemoval": removalTokens,
    "uploadthing/next": { createUploadthing: () => () => {
      const entry = { middleware(fn) { this.check = fn; return this; }, onUploadComplete(fn) { this.complete = fn; return this; } }; return entry;
    } },
    "uploadthing/server": { UploadThingError: Error },
  }).uploadRouter;
  const req = { cookies: { get: () => ({ value: "session" }) } };
  const files = [{ type: "image/png", size: 100 }];
  const mock = actions();
  await usingFetch(mock.fetch, async () => {
    assert.deepEqual(await endpoints.productImage.check({ req, files }), { userId: "7" });
    await assert.rejects(endpoints.sellerDocument.check({ req, files }));
    for (const file of [{ type: "image/gif", size: 10 }, { type: "image/png", size: 4194305 }]) await assert.rejects(endpoints.productImage.check({ req, files: [file] }));
    await assert.rejects(endpoints.productImage.check({ req: { cookies: { get: () => undefined } }, files }));
    assert.deepEqual(endpoints.productImage.complete({ file: { ufsUrl: input.imageUrls[0], key: "key" }, metadata: { userId: "7" } }), { url: input.imageUrls[0], key: "key", removalToken: removalTokens.productImageRemovalToken("key", "7"), userId: "7" });
  });
  for (const options of [{ role: "CUSTOMER" }, { role: "ADMIN" }, { status: "SUSPENDED" }, { shopStatus: "PENDING" }]) {
    const denied = actions(options);
    await usingFetch(denied.fetch, () => assert.rejects(endpoints.productImage.check({ req, files })));
  }
  const customer = actions({ role: "CUSTOMER" });
  await usingFetch(customer.fetch, async () => assert.deepEqual(await endpoints.sellerDocument.check({ req, files }), { userId: "7" }));
});

function formHarness(upload, create, getCategories = async () => [{ id: 2, name: "Category" }], remove = async () => ({ success: true })) {
  const hooks = [], effects = [], navigations = [];
  const listeners = new Map(), cleanupsRequested = [], remembered = [], forgotten = [];
  const previousWindow = global.window;
  global.window = { addEventListener: (type, fn) => listeners.set(type, fn), removeEventListener: type => listeners.delete(type) };
  let cursor = 0;
  const react = {
    useState(initial) {
      const index = cursor++;
      if (!(index in hooks)) hooks[index] = initial;
      return [hooks[index], next => { hooks[index] = typeof next === "function" ? next(hooks[index]) : next; }];
    },
    useRef(initial) { const index = cursor++; return hooks[index] ?? (hooks[index] = { current: initial }); },
    useEffect(effect) { const index = cursor++; if (!(index in hooks)) { hooks[index] = true; effects.push(effect); } },
  };
  const jsx = (type, props) => ({ type, props });
  const component = load("components/Seller/SellerAddProduct.tsx", {
    react, "react/jsx-runtime": { jsx, jsxs: jsx }, "lucide-react": {},
    "next/navigation": { useRouter: () => ({ push: path => navigations.push(path), refresh() {} }) },
    "@/components/Common/Breadcrumb": { default: "breadcrumb" }, "@/components/Common/ProductImage": { default: "preview", ProductImageGallery: "gallery" },
    "@/components/ui/input": { Input: "input" }, "@/components/ui/label": { Label: "label" },
    "@/components/ui/textarea": { Textarea: "textarea" }, "@/components/ui/button": { Button: "button" }, "@/components/ui/card": { Card: "card" },
    "@/ServerAction/products": { createSellerProduct: create, getCategories, removeSellerProductImage: remove },
    "@/lib/pendingProductImages": { activateProductImageForm: () => () => {}, cleanupPendingProductImages: async (...args) => { cleanupsRequested.push(args); }, rememberProductImage: image => remembered.push(image), forgetProductImage: key => forgotten.push(key), markProductImagesSubmitted() {} },
    "@/lib/uploadImage": { uploadSingleImage: upload, MAX_IMAGE_SIZE: 4194304, ALLOWED_IMAGE_TYPES: ["image/jpeg", "image/png", "image/webp"] },
  }).default;
  function walk(element) {
    if (!element || typeof element !== "object") return [];
    if (Array.isArray(element)) return element.flatMap(walk);
    return [element, ...walk(element.props?.children)];
  }
  const render = () => { cursor = 0; return walk(component()); };
  render(); const cleanups = effects.map(effect => effect());
  return { render, find: predicate => render().find(predicate), navigations, listeners, cleanupsRequested, remembered, forgotten, cleanup: () => { cleanups.forEach(fn => fn?.()); global.window = previousWindow; } };
}
const deferred = () => { let resolve, reject; const promise = new Promise((yes, no) => { resolve = yes; reject = no; }); return { promise, resolve, reject }; };
const flush = async () => { await new Promise(setImmediate); await new Promise(setImmediate); };

test("full-width preview actions target the active photo, preserve main order and recover indices after removals", async () => {
  const originalCreate = URL.createObjectURL, originalRevoke = URL.revokeObjectURL;
  URL.createObjectURL = file => `blob:${file.name}`; URL.revokeObjectURL = () => {};
  const pending = deferred(), writes = [];
  let uploadCount = 0;
  const form = formHarness(file => ++uploadCount === 1 ? pending.promise : Promise.resolve({ url: `https://example.com/${file.name}`, key: file.name, removalToken: "receipt" }), data => { writes.push(data); return { success: false, message: "Keep the form open" }; });
  const gallery = () => form.find(node => node.type === "gallery");
  const remove = async index => { form.find(node => node.props?.["aria-label"] === `Remove photo ${index}`).props.onClick(); await flush(); };
  const assertActive = (id, index, total) => {
    assert.equal(gallery().props.activeImageId, id);
    assert.equal(gallery().props.images.length, total);
    const rendered = imageHarness().ProductImageGallery(gallery().props);
    const counter = rendered.props.children[0].props.children[2];
    assert.deepEqual(counter && counter.props.children, total > 1 ? [index, " / ", total] : false);
  };
  try {
    await flush();
    assert.equal(gallery(), undefined);
    assert.equal(form.find(node => node.props?.["aria-label"] === "Remove photo 1"), undefined);
    form.find(node => node.props?.type === "file").props.onChange({ target: { files: Array.from({ length: 5 }, (_, index) => ({ name: `${index + 1}.png`, type: "image/png", size: 12 })), value: "" } });
    const ids = gallery().props.images.map(image => image.id);
    assert.deepEqual(gallery().props.images.map(image => image.src), ["blob:1.png", "blob:2.png", "blob:3.png", "blob:4.png", "blob:5.png"]);
    assert.equal(gallery().props.activeImageId, ids[0]);
    assert.equal(form.find(node => node.type === "fieldset").props.disabled, true);
    pending.resolve({ url: "https://example.com/1.png", key: "1.png", removalToken: "receipt" }); await flush();
    assert.equal(form.find(node => node.props?.type === "radio"), undefined);
    assert.equal(form.find(node => node.props?.title === "1.png"), undefined);
    assert.equal(form.find(node => node.props?.["aria-label"] === "Photo 1 is the main image").props.disabled, true);
    gallery().props.onActiveImageChange(ids[4]);
    const setMain = form.find(node => node.props?.["aria-label"] === "Use photo 5 as main image");
    assert.equal(setMain.props.type, "button");
    assert.equal(setMain.props.disabled, false);
    assert.match(setMain.props.className, /focus-visible:ring-2/);
    setMain.props.onClick();
    assertActive(ids[4], 1, 5);
    assert.equal(gallery().props.images[0].id, ids[4]);
    gallery().props.onActiveImageChange(ids[1]);
    assertActive(ids[1], 3, 5);
    assert.equal(gallery().props.images[0].id, ids[4]);
    assert.match(form.find(node => node.props?.["aria-label"] === "Remove photo 3").props.className, /focus-visible:ring-2/);
    for (const [id, value] of [["product-name", "My product"], ["product-price", "12.34"], ["product-stock", "2"]]) form.find(node => node.props?.id === id).props.onChange({ target: { value } });
    form.find(node => node.props?.type === "checkbox").props.onChange({ target: { checked: true } });
    await form.find(node => node.type === "form").props.onSubmit({ preventDefault() {} }); await flush();
    assert.equal(writes[0].imageUrls[0], "https://example.com/5.png");
    await remove(3);
    assertActive(ids[2], 3, 4);
    assert.equal(gallery().props.images[0].id, ids[4]);
    gallery().props.onActiveImageChange(ids[3]);
    await remove(4);
    assertActive(ids[2], 3, 3);
    gallery().props.onActiveImageChange(ids[4]);
    await remove(1);
    assertActive(ids[0], 1, 2);
    assert.equal(gallery().props.images[0].id, ids[0]);
    assert.equal(form.find(node => node.props?.["aria-label"] === "Photo 1 is the main image").props.disabled, true);
    await remove(1);
    assertActive(ids[2], 1, 1);
    while (gallery()) await remove(1);
    assert.equal(form.find(node => node.props?.["aria-label"] === "Remove photo 1"), undefined);
    assert.equal(form.find(node => node.props?.["aria-label"] === "Photo 1 is the main image"), undefined);
    assert.ok(form.render().some(node => node.props?.children === "Choose images to preview your product."));
  } finally { form.cleanup(); URL.createObjectURL = originalCreate; URL.revokeObjectURL = originalRevoke; }
});

function imageHarness() {
  const hooks = [], effects = [];
  let cursor = 0;
  const jsx = (type, props) => ({ type, props });
  const api = load("components/Common/ProductImage.tsx", {
    react: {
      useState(initial) { const index = cursor++; if (!(index in hooks)) hooks[index] = initial; return [hooks[index], value => { hooks[index] = value; }]; },
      useEffect(effect, deps) { const index = cursor++; if (!hooks[index] || deps.some((dep, offset) => dep !== hooks[index][offset])) { hooks[index] = deps; effects.push(effect); } },
    },
    "react/jsx-runtime": { jsx, jsxs: jsx }, "next/image": { default: "image" },
    "@/components/ui/button": { Button: "button" }, "@/components/ui/skeleton": { Skeleton: "skeleton" },
  });
  return { ...api, render: props => { cursor = 0; const result = api.default(props); effects.splice(0).forEach(effect => effect()); return result; } };
}
function imageChildren(element) { return [element.props.children].flat().filter(Boolean); }

test("shared gallery uses stable image IDs, thumbnail buttons, active borders and a safe fallback", () => {
  const { ProductImageGallery, default: ProductImage } = imageHarness();
  const images = [{ id: "second", src: "blob:second" }, { id: "first", src: "blob:first" }], viewed = [];
  const gallery = ProductImageGallery({ images, activeImageId: "first", onActiveImageChange: id => viewed.push(id), alt: "Product" });
  const [main, thumbnails] = gallery.props.children;
  assert.equal(main.props.children[0].type, ProductImage);
  assert.equal(main.props.children[0].props.src, "blob:first");
  assert.equal(main.props.children[0].props.showSkeleton, true);
  assert.deepEqual(imageChildren(main).find(node => node.props["aria-live"] === "polite").props.children, [2, " / ", 2]);
  const buttons = thumbnails.props.children;
  assert.equal(buttons[1].props["aria-pressed"], true);
  assert.match(buttons[1].props.className, /border-blue/);
  assert.equal(buttons[0].props.type, "button");
  buttons[0].props.onClick(); assert.deepEqual(viewed, ["second"]);
  const fallback = ProductImageGallery({ images, activeImageId: "removed", onActiveImageChange() {}, alt: "Product" });
  assert.equal(fallback.props.children[0].props.children[0].props.src, "blob:second");
  assert.equal(ProductImageGallery({ images: [], activeImageId: "", onActiveImageChange() {}, alt: "Product" }), null);
});

test("Seller gallery fills a 16:9 contain frame, labels the main image and distinguishes the active thumbnail", () => {
  const { ProductImageGallery } = imageHarness();
  const images = Array.from({ length: 5 }, (_, index) => ({ id: `${index}`, src: `blob:${index}` })), selected = [];
  const main = ProductImageGallery({ images, activeImageId: "0", onActiveImageChange: id => selected.push(id), alt: "Product", compact: true });
  const frame = main.props.children[0];
  assert.match(frame.props.children[0].props.className, /!aspect-video/);
  assert.equal(frame.props.children[0].props.imageClassName, "p-0");
  assert.equal(frame.props.children[0].props.fit, "contain");
  assert.equal(frame.props.children[1].props.children, "Main");
  const viewing = ProductImageGallery({ images, activeImageId: "3", onActiveImageChange: id => selected.push(id), alt: "Product", compact: true });
  assert.equal(viewing.props.children[0].props.children[1], false);
  assert.deepEqual(viewing.props.children[0].props.children[2].props.children, [4, " / ", 5]);
  const buttons = viewing.props.children[1].props.children;
  assert.equal(buttons[0].props.children[1].props.children, "Main");
  assert.match(buttons[3].props.className, /ring-2 ring-blue ring-offset-2/);
  assert.equal(buttons[3].props["aria-pressed"], true);
  buttons[4].props.onClick(); assert.deepEqual(selected, ["4"]);
  const single = ProductImageGallery({ images: images.slice(0, 1), activeImageId: "0", onActiveImageChange() {}, alt: "Product", compact: true });
  assert.equal(single.props.children[0].props.children[2], false);
});

test("image selection rejects invalid files and excess photos, accepts the size boundary and revokes every local URL", async () => {
  const originalCreate = URL.createObjectURL, originalRevoke = URL.revokeObjectURL;
  const uploads = [], created = [], revoked = [];
  URL.createObjectURL = file => { const url = `blob:${file.name}`; created.push(url); return url; };
  URL.revokeObjectURL = url => revoked.push(url);
  const form = formHarness(async file => { uploads.push(file.name); return { url: `https://example.com/${file.name}`, key: file.name, removalToken: "receipt" }; }, () => {});
  const choose = files => form.find(node => node.props?.type === "file").props.onChange({ target: { files, value: "" } });
  let cleaned = false;
  try {
    await flush();
    for (const file of [{ name: "file.gif", type: "image/gif", size: 12 }, { name: "big.png", type: "image/png", size: 4 * 1024 * 1024 + 1 }]) {
      choose([file]);
      assert.equal(form.find(node => node.props?.role === "alert").props.children, "Choose JPG, PNG, or WebP images up to 4 MB each.");
      assert.equal(uploads.length, 0); assert.equal(created.length, 0);
    }
    choose(Array.from({ length: 6 }, (_, index) => ({ name: `${index}.png`, type: "image/png", size: 12 })));
    assert.equal(form.find(node => node.props?.role === "alert").props.children, "Choose no more than 5 product images.");
    assert.equal(created.length, 0);
    choose([{ name: "1.jpg", type: "image/jpeg", size: 4 * 1024 * 1024 }, { name: "2.webp", type: "image/webp", size: 12 }, { name: "3.png", type: "image/png", size: 12 }]); await flush();
    choose([{ name: "4.png", type: "image/png", size: 12 }, { name: "5.png", type: "image/png", size: 12 }]); await flush();
    assert.equal(uploads.length, 5);
    assert.equal(form.find(node => node.type === "button" && Array.isArray(node.props.children) && node.props.children.includes("Choose images")).props.disabled, true);
    choose([{ name: "6.png", type: "image/png", size: 12 }]);
    assert.equal(uploads.length, 5);
    form.find(node => node.props?.["aria-label"] === "Remove photo 1").props.onClick(); await flush();
    assert.deepEqual(revoked, ["blob:1.jpg"]);
    assert.equal(form.find(node => node.type === "gallery").props.images[0].src, "blob:2.webp");
    form.cleanup(); cleaned = true;
    assert.deepEqual(revoked.sort(), created.sort());
  } finally { if (!cleaned) form.cleanup(); URL.createObjectURL = originalCreate; URL.revokeObjectURL = originalRevoke; }
});

test("image Skeleton settles on load or error, restarts on source change and stays optional", () => {
  const image = imageHarness(), props = { src: "blob:first", alt: "Product", size: "fill", showSkeleton: true };
  let rendered = image.render(props);
  assert.equal(rendered.props["aria-busy"], true);
  assert.equal(imageChildren(rendered)[1].type, "skeleton");
  assert.match(imageChildren(rendered)[1].props.className, /motion-reduce:animate-none/);
  imageChildren(rendered)[0].props.onLoad();
  rendered = image.render(props);
  assert.equal(rendered.props["aria-busy"], false);
  assert.equal(imageChildren(rendered).length, 1);
  rendered = image.render({ ...props, src: "blob:second" });
  assert.equal(rendered.props["aria-busy"], true);
  imageChildren(rendered)[0].props.onError();
  rendered = image.render({ ...props, src: "blob:second" });
  assert.equal(rendered.props["aria-busy"], false);
  assert.equal(imageChildren(rendered)[0].props.src, "/images/products/product-placeholder.svg");
  imageChildren(rendered)[0].props.onError();
  assert.equal(image.render({ ...props, src: "blob:second" }).props["aria-busy"], false);
  rendered = image.render({ ...props, src: "blob:cached" });
  imageChildren(rendered)[0].props.onLoad();
  assert.equal(image.render({ ...props, src: "blob:cached" }).props["aria-busy"], false);
  assert.equal(imageHarness().render({ ...props, showSkeleton: false }).props["aria-busy"], undefined);
});

test("Product Detail reuses the gallery and keeps Customer purchase controls and Seller restrictions", () => {
  const product = { title: "Product", price: 12.34, reviews: 0, imgs: { previews: ["https://example.com/main.png", "https://example.com/second.png"] } };
  const jsx = (type, props) => ({ type, props });
  function walk(element) {
    if (!element || typeof element !== "object") return [];
    if (Array.isArray(element)) return element.flatMap(walk);
    return [element, ...walk(element.props?.children)];
  }
  for (const role of ["CUSTOMER", "SELLER"]) {
    const additions = [];
    const ProductDetail = load("components/Shop/ProductDetail.tsx", {
      react: { useState: initial => [initial, () => {}] }, "react/jsx-runtime": { jsx, jsxs: jsx }, "lucide-react": {},
      "@/components/Common/ProductImage": { ProductImageGallery: "gallery" },
      "@/app/context/CartContext": { useCart: () => ({ addItemToCart: item => additions.push(item) }) },
      "@/app/context/AuthContext": { useAuth: () => ({ user: { role }, isLoading: false }) },
      "@/components/Common/ProductStock": { useProductAvailability: () => ({ stock: 3, remaining: 3, canAdd: true }) },
      "@/components/Common/Breadcrumb": { default: "breadcrumb" }, "@/components/ui/button": { Button: "button" },
    }).default;
    const rendered = walk(ProductDetail({ product, categoryNames: [], reviews: [], sample: false }));
    const gallery = rendered.find(node => node.type === "gallery");
    assert.deepEqual(gallery.props.images.map(image => image.src), product.imgs.previews);
    assert.equal(gallery.props.activeImageId, gallery.props.images[0].id);
    const purchase = rendered.find(node => node.type === "button" && Array.isArray(node.props.children) && node.props.children.includes("Add to cart"));
    if (role === "CUSTOMER") { assert.ok(purchase); purchase.props.onClick(); assert.deepEqual(additions, [{ ...product, quantity: 1 }]); }
    else { assert.equal(purchase, undefined); assert.deepEqual(additions, []); }
  }
});

test("form uploads sequentially, retries only failed photos, keeps state on save error, orders main photo and prevents duplicate submission", async () => {
  const originalCreate = URL.createObjectURL, originalRevoke = URL.revokeObjectURL;
  URL.createObjectURL = file => `blob:${file.name}`; URL.revokeObjectURL = () => {};
  const first = deferred(), second = deferred(), saved = deferred();
  const uploads = [], writes = [];
  const form = formHarness(file => {
    uploads.push(file.name);
    return uploads.length === 1 ? first.promise : uploads.length === 2 ? second.promise : Promise.resolve({ url: "https://example.com/second.png" });
  }, data => { writes.push(data); return writes.length === 1 ? Promise.resolve({ success: false, message: "Category was deleted" }) : saved.promise; });
  try {
    await flush();
    const input = form.find(node => node.type === "input" && node.props.type === "file");
    input.props.onChange({ target: { files: [{ name: "first.png", type: "image/png", size: 12 }, { name: "second.png", type: "image/png", size: 12 }], value: "" } });
    assert.deepEqual(uploads, ["first.png"]);
    assert.equal(form.find(node => node.type === "fieldset").props.disabled, true);
    first.resolve({ url: "https://example.com/first.png" }); await flush();
    assert.deepEqual(uploads, ["first.png", "second.png"]);
    second.reject(new Error("Upload interrupted")); await flush();
    const gallery = () => form.find(node => node.type === "gallery");
    gallery().props.onActiveImageChange(gallery().props.images[1].id);
    form.find(node => node.props?.["aria-label"] === "Retry photo 2").props.onClick(); await flush();
    assert.deepEqual(uploads, ["first.png", "second.png", "second.png"]);
    for (const [id, value] of [["product-name", "My product"], ["product-price", "12.34"], ["product-stock", "2"]]) form.find(node => node.props?.id === id).props.onChange({ target: { value } });
    form.find(node => node.props?.type === "checkbox").props.onChange({ target: { checked: true } });
    form.find(node => node.props?.["aria-label"] === "Use photo 2 as main image").props.onClick();
    await form.find(node => node.type === "form").props.onSubmit({ preventDefault() {} });
    assert.equal(form.find(node => node.props?.id === "product-name").props.value, "My product");
    assert.deepEqual(writes[0].imageUrls, ["https://example.com/second.png", "https://example.com/first.png"]);
    assert.equal(form.find(node => node.props?.role === "alert").props.children, "Category was deleted");
    const submit = form.find(node => node.type === "form").props.onSubmit;
    const pending = submit({ preventDefault() {} });
    await submit({ preventDefault() {} });
    assert.equal(writes.length, 2);
    assert.equal(form.find(node => node.type === "fieldset").props.disabled, true);
    saved.resolve({ success: true, data: { sellerId: 12 } }); await pending;
    assert.deepEqual(form.navigations, ["/shop/12"]);
    assert.deepEqual(uploads, ["first.png", "second.png", "second.png"]);
  } finally { form.cleanup(); URL.createObjectURL = originalCreate; URL.revokeObjectURL = originalRevoke; }
});

test("cloud removal verifies uploader, key, session and active shop before calling UploadThing", async () => {
  const key = "my-product-image", removalToken = removalTokens.productImageRemovalToken(key, "7");
  const allowed = actions();
  await usingFetch(allowed.fetch, async () => {
    assert.equal((await allowed.api.removeSellerProductImage({ key, removalToken })).success, true);
    assert.deepEqual(allowed.deletions, [key]);
  });
  for (const options of [{ userId: 8 }, { role: "CUSTOMER" }, { role: "ADMIN" }, { status: "SUSPENDED" }, { shopStatus: "SUSPENDED" }, { token: null }]) {
    const denied = actions(options);
    await usingFetch(denied.fetch, async () => {
      assert.equal((await denied.api.removeSellerProductImage({ key, removalToken })).success, false);
      assert.deepEqual(denied.deletions, []);
    });
  }
  for (const request of [{ key: "another-file", removalToken }, { key, removalToken: "0".repeat(64) }, { key, removalToken: "malformed" }]) {
    const denied = actions();
    await usingFetch(denied.fetch, async () => {
      assert.equal((await denied.api.removeSellerProductImage(request)).success, false);
      assert.deepEqual(denied.deletions, []);
    });
  }
});

test("cloud deletion failure is reported instead of returning success", async () => {
  for (const options of [{ deleteResult: { success: false } }, { deleteError: true }]) {
    const mock = actions(options);
    await usingFetch(mock.fetch, async () => {
      const result = await mock.api.removeSellerProductImage({ key: "key", removalToken: removalTokens.productImageRemovalToken("key", "7") });
      assert.equal(result.success, false);
      assert.equal(result.error, "DELETE_FAILED");
    });
  }
});

test("Remove waits for cloud success, preserves images on failure, retries and blocks duplicate remove/save", async () => {
  const originalCreate = URL.createObjectURL, originalRevoke = URL.revokeObjectURL;
  URL.createObjectURL = file => `blob:${file.name}`;
  const revoked = []; URL.revokeObjectURL = url => revoked.push(url);
  const pending = deferred(), removals = [], writes = [];
  const form = formHarness(async file => ({ url: `https://example.com/${file.name}`, key: file.name, removalToken: "receipt" }), data => { writes.push(data); return { success: true, data: { sellerId: 12 } }; }, undefined, data => { removals.push(data); return removals.length === 1 ? pending.promise : Promise.resolve({ success: true }); });
  try {
    await flush();
    form.find(node => node.type === "input" && node.props.type === "file").props.onChange({ target: { files: [{ name: "main.png", type: "image/png", size: 12 }, { name: "second.png", type: "image/png", size: 12 }], value: "" } }); await flush();
    for (const [id, value] of [["product-name", "My product"], ["product-price", "12.34"], ["product-stock", "2"]]) form.find(node => node.props?.id === id).props.onChange({ target: { value } });
    form.find(node => node.props?.type === "checkbox").props.onChange({ target: { checked: true } });
    const remove = form.find(node => node.props?.["aria-label"] === "Remove photo 1").props.onClick;
    remove(); remove();
    await form.find(node => node.type === "form").props.onSubmit({ preventDefault() {} });
    assert.equal(writes.length, 0); assert.equal(removals.length, 1);
    assert.equal(form.find(node => node.type === "fieldset").props.disabled, true);
    assert.equal(form.find(node => node.type === "gallery").props.images.length, 2);
    assert.deepEqual(revoked, []);
    pending.resolve({ success: false, message: "Cloud unavailable" }); await flush();
    assert.equal(form.find(node => node.props?.role === "alert").props.children, "Cloud unavailable");
    assert.equal(form.find(node => node.type === "gallery").props.images.length, 2);
    form.find(node => node.props?.["aria-label"] === "Remove photo 1").props.onClick(); await flush();
    assert.deepEqual(removals, [{ key: "main.png", removalToken: "receipt" }, { key: "main.png", removalToken: "receipt" }]);
    assert.equal(form.find(node => node.type === "gallery").props.images.length, 1);
    assert.deepEqual(revoked, ["blob:main.png"]);
    assert.equal(form.find(node => node.props?.["aria-label"] === "Photo 1 is the main image").props.disabled, true);
    await form.find(node => node.type === "form").props.onSubmit({ preventDefault() {} });
    assert.deepEqual(writes[0].imageUrls, ["https://example.com/second.png"]);
  } finally { form.cleanup(); URL.createObjectURL = originalCreate; URL.revokeObjectURL = originalRevoke; }
});

test("a failed upload with no cloud file is removed locally without a deletion request", async () => {
  const originalCreate = URL.createObjectURL, originalRevoke = URL.revokeObjectURL;
  URL.createObjectURL = () => "blob:failed"; URL.revokeObjectURL = () => {};
  const removals = [];
  const form = formHarness(async () => { throw new Error("Upload failed"); }, () => {}, undefined, data => { removals.push(data); });
  try {
    await flush();
    form.find(node => node.type === "input" && node.props.type === "file").props.onChange({ target: { files: [{ name: "failed.png", type: "image/png", size: 12 }], value: "" } }); await flush();
    form.find(node => node.props?.["aria-label"] === "Remove photo 1").props.onClick(); await flush();
    assert.deepEqual(removals, []);
    assert.equal(form.find(node => node.type === "gallery"), undefined);
  } finally { form.cleanup(); URL.createObjectURL = originalCreate; URL.revokeObjectURL = originalRevoke; }
});

test("category loading failure can be retried; empty results keep saving disabled", async () => {
  let requests = 0;
  const form = formHarness(() => {}, () => {}, async () => { if (++requests === 1) throw new Error("Offline"); return []; });
  try {
    await flush();
    form.find(node => node.type === "button" && node.props.children === "Retry categories").props.onClick(); await flush();
    assert.equal(requests, 2);
    assert.equal(form.find(node => node.type === "button" && node.props.type === "submit").props.disabled, true);
    assert.ok(form.render().some(node => typeof node.props?.children === "string" && node.props.children.includes("wait for an Admin")));
  } finally { form.cleanup(); }
});

test("saved product references prevent cloud deletion even after page-close cleanup", async () => {
  const mock = actions({ savedImages: ["https://app.ufs.sh/f/key"] });
  await usingFetch(mock.fetch, async () => {
    const result = await mock.api.removeSellerProductImage({ key: "key", removalToken: removalTokens.productImageRemovalToken("key", "7") });
    assert.equal(result.error, "IMAGE_IN_USE");
    assert.deepEqual(mock.deletions, []);
  });
});

test("leaving a form cleans up uploaded files, including an upload that completes after unmount", async () => {
  const originalCreate = URL.createObjectURL, originalRevoke = URL.revokeObjectURL;
  URL.createObjectURL = () => "blob:photo"; URL.revokeObjectURL = () => {};
  const second = deferred(); let requests = 0;
  const form = formHarness(async file => ++requests === 1 ? { key: file.name, url: `https://example.com/${file.name}`, removalToken: "receipt", userId: "7" } : second.promise, () => {});
  try {
    await flush();
    form.find(node => node.type === "input" && node.props.type === "file").props.onChange({ target: { files: [{ name: "first.png", type: "image/png", size: 12 }, { name: "second.png", type: "image/png", size: 12 }], value: "" } }); await flush();
    form.listeners.get("pagehide")();
    assert.equal(form.cleanupsRequested[0][1].beacon, true);
    assert.equal(form.cleanupsRequested[0][1].images[0].key, "first.png");
    form.cleanup();
    assert.ok(form.cleanupsRequested.some(([, options]) => !options.beacon && options.images[0].key === "first.png"));
    second.resolve({ key: "second.png", url: "https://example.com/second.png", removalToken: "receipt", userId: "7" }); await flush();
    assert.equal(form.cleanupsRequested.at(-1)[1].images[0].key, "second.png");
  } finally { URL.createObjectURL = originalCreate; URL.revokeObjectURL = originalRevoke; }
});

test("successful product save removes pending records and exit does not delete its photos", async () => {
  const originalCreate = URL.createObjectURL, originalRevoke = URL.revokeObjectURL;
  URL.createObjectURL = () => "blob:photo"; URL.revokeObjectURL = () => {};
  const form = formHarness(async () => ({ key: "photo", url: "https://example.com/photo", removalToken: "receipt", userId: "7" }), async () => ({ success: true, data: { sellerId: 12 } }));
  try {
    await flush();
    form.find(node => node.type === "input" && node.props.type === "file").props.onChange({ target: { files: [{ name: "photo", type: "image/png", size: 12 }], value: "" } }); await flush();
    for (const [id, value] of [["product-name", "Product"], ["product-price", "12.34"], ["product-stock", "2"]]) form.find(node => node.props?.id === id).props.onChange({ target: { value } });
    form.find(node => node.props?.type === "checkbox").props.onChange({ target: { checked: true } });
    await form.find(node => node.type === "form").props.onSubmit({ preventDefault() {} });
    form.listeners.get("pagehide")();
    assert.deepEqual(form.forgotten, ["photo"]);
    form.cleanup();
    assert.deepEqual(form.cleanupsRequested, []);
  } finally { URL.createObjectURL = originalCreate; URL.revokeObjectURL = originalRevoke; }
});

test("back/forward cache restoration discards departed photos and keeps entered product details", async () => {
  const originalCreate = URL.createObjectURL, originalRevoke = URL.revokeObjectURL;
  URL.createObjectURL = () => "blob:photo"; URL.revokeObjectURL = () => {};
  const form = formHarness(async () => ({ key: "photo", url: "https://example.com/photo", removalToken: "receipt", userId: "7" }), () => {});
  try {
    await flush();
    form.find(node => node.props?.id === "product-name").props.onChange({ target: { value: "Keep my details" } });
    form.find(node => node.type === "input" && node.props.type === "file").props.onChange({ target: { files: [{ name: "photo", type: "image/png", size: 12 }], value: "" } }); await flush();
    form.listeners.get("pagehide")();
    form.listeners.get("pageshow")({ persisted: true });
    assert.equal(form.find(node => node.type === "gallery"), undefined);
    assert.equal(form.find(node => node.props?.id === "product-name").props.value, "Keep my details");
    assert.equal(form.find(node => node.props?.role === "alert").props.children, "Choose your photos again after returning to this page.");
  } finally { form.cleanup(); URL.createObjectURL = originalCreate; URL.revokeObjectURL = originalRevoke; }
});

test("pending cleanup uses beacon/keepalive, retries failed storage records, protects other tabs and waits for saving", async () => {
  const previousStorage = global.localStorage, previousWindow = global.window, previousNavigator = Object.getOwnPropertyDescriptor(global, "navigator"), previousFetch = global.fetch;
  const stored = new Map(), requests = [], beacons = [];
  global.localStorage = { getItem: key => stored.get(key) ?? null, setItem: (key, value) => stored.set(key, value), removeItem: key => stored.delete(key), get length() { return stored.size; }, key: index => Array.from(stored.keys())[index] ?? null };
  global.window = { setInterval: () => 1, clearInterval() {} };
  let held = [];
  Object.defineProperty(global, "navigator", { configurable: true, value: { sendBeacon: (...args) => { beacons.push(args); return true; }, locks: { query: async () => ({ held }), request: () => Promise.resolve() } } });
  const pending = load("lib/pendingProductImages.ts", {});
  const image = { key: "photo", removalToken: "receipt", userId: "7", formId: "draft" };
  try {
    pending.rememberProductImage(image);
    const deactivate = pending.activateProductImageForm("draft");
    global.fetch = async (url, options) => { requests.push({ url, options }); throw new Error("Offline"); };
    await pending.cleanupPendingProductImages("7");
    assert.equal(requests.length, 0);
    await pending.cleanupPendingProductImages("7", { formId: "draft", beacon: true });
    assert.equal(beacons.length, 1);
    deactivate();
    held = [{ name: "seller-product-image-form:draft" }];
    await pending.cleanupPendingProductImages("7");
    assert.equal(requests.length, 0);
    held = [];
    await pending.cleanupPendingProductImages("8");
    assert.equal(requests.length, 0);
    await pending.cleanupPendingProductImages("7");
    assert.equal(requests.length, 1);
    assert.equal(requests[0].options.keepalive, true);
    pending.markProductImagesSubmitted(["photo"]);
    await pending.cleanupPendingProductImages("7", { formId: "draft" });
    assert.equal(requests.length, 1);
    for (const key of stored.keys()) stored.set(key, stored.get(key).replace(/"submittedAt":\d+/, `"submittedAt":${Date.now() - 31000}`));
    global.fetch = async (url, options) => { requests.push({ url, options }); return { ok: true, json: async () => ({ results: [{ key: "photo", success: false, error: "IMAGE_IN_USE" }] }) }; };
    await pending.cleanupPendingProductImages("7");
    assert.equal(requests.length, 2);
    await pending.cleanupPendingProductImages("7");
    assert.equal(requests.length, 2);
  } finally {
    global.localStorage = previousStorage; global.window = previousWindow; global.fetch = previousFetch;
    if (previousNavigator) Object.defineProperty(global, "navigator", previousNavigator); else delete global.navigator;
  }
});
