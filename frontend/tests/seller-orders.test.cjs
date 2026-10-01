const { test } = require("node:test");
const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const { resolve } = require("node:path");
const ts = require("typescript");

function load(file, mocks = {}) {
  const compiled = ts.transpileModule(readFileSync(resolve(__dirname, "../src", file), "utf8"), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, jsx: ts.JsxEmit.ReactJSX } }).outputText;
  const exports = {};
  new Function("require", "exports", compiled)(name => mocks[name] ?? require(name), exports);
  return exports;
}
const model = load("lib/sellerOrders.ts");
const shop = { sellerId: 12, shopName: "Shop" };
const order = (changes = {}) => ({ orderId: 4, subOrderNumber: "ORDER-4", orderGroupId: 9, sellerId: 12, shopName: "Shop", subtotal: 20, shippingFee: 3, sellerDiscount: 1, totalAmount: 22, shippingMethod: "STANDARD", orderStatus: "WAITING_SELLER_CONFIRM", paymentStatus: "PAID", customerName: "Customer", createdAt: "2026-09-30T12:00:00", rejectionReason: null, shippingAddress: { receiverName: "Receiver", phoneNumber: "123", addressLine: "Road", district: "District", province: "Province", postalCode: "10000" }, items: [{ orderItemId: 1, productName: "Item", quantity: 2, unitPrice: 10, totalPrice: 20 }], shipment: null, ...changes });
const response = (data, status = 200, message = "Error") => ({ ok: status >= 200 && status < 300, status, json: async () => ({ success: status >= 200 && status < 300, data, message }) });
const deferred = () => { let resolve, reject; const promise = new Promise((yes, no) => { resolve = yes; reject = no; }); return { promise, resolve, reject }; };
const flush = async () => { await new Promise(setImmediate); await new Promise(setImmediate); };

test("metrics use each shop order amount, payment and status; latest first and empty shops", () => {
  const orders = [order(), order({ orderId: 5, paymentStatus: "PARTIALLY_REFUNDED", orderStatus: "PREPARING", totalAmount: 100, createdAt: "2026-10-01T01:00:00" }), order({ orderId: 6, orderStatus: "CANCELLED", totalAmount: 500 }), order({ orderId: 7, paymentStatus: "PENDING", orderStatus: "PENDING_PAYMENT", totalAmount: 800 }), order({ orderId: 8, orderStatus: "COMPLETED", totalAmount: 40 })];
  const summary = model.sellerOrderSummary(orders);
  assert.equal(summary.paidSales, 162); assert.equal(summary.awaiting, 1); assert.equal(summary.completed, 1);
  assert.deepEqual(summary.active.map(o => o.orderId), [5, 7, 4]); assert.equal(summary.history.length, 2);
  assert.deepEqual(model.sellerOrderSummary([]), { active: [], history: [], paidSales: 0, awaiting: 0, completed: 0 });
  assert.equal(model.hasOrderStatusConflict(order({ orderStatus: "PENDING_PAYMENT" })), true);
  assert.equal(model.hasOrderStatusConflict(order({ paymentStatus: null })), true);
});

function api({ token = "secret", role = "SELLER", status = "ACTIVE", shopStatus = "ACTIVE", result = order(), failure = false } = {}) {
  const calls = [];
  const route = load("app/api/seller-orders/[[...path]]/route.ts", {
    "next/headers": { cookies: async () => ({ get: () => token ? { value: token } : undefined }) },
    "next/server": { NextResponse: { json: (body, options) => ({ body, ...options }) } },
    "@/lib/sellerOrders": model,
  });
  const fetch = async (url, options) => {
    calls.push({ url, options });
    if (failure) throw new Error("Network error");
    return response(url.endsWith("/auth/me") ? { role, status, userId: 7 } : url.includes("/shops/user/") ? { ...shop, status: shopStatus } : options.method === "POST" ? null : url.includes("/seller/") ? [result] : result);
  };
  const request = (path = [], method = "GET", body = {}, query = "", origin = "http://localhost:3000") => route[method]({ method, nextUrl: new URL(`http://localhost:3000/api/seller-orders/${path.join("/")}${query}`), headers: { get: () => origin }, json: async () => body }, { params: Promise.resolve({ path }) });
  return { calls, fetch, request };
}
async function usingFetch(fetch, work) { const before = global.fetch; global.fetch = fetch; try { await work(); } finally { global.fetch = before; } }

test("BFF derives own shop from cookie identity; forwards Bearer, uses no-store and never returns token", async () => {
  const mock = api();
  await usingFetch(mock.fetch, async () => {
    const list = await mock.request(); assert.equal(list.status, 200); assert.deepEqual(list.body.data.orders, [order()]);
    assert.equal(list.headers["Cache-Control"], "no-store"); assert.ok(!JSON.stringify(list).includes("secret"));
    assert.match(mock.calls.at(-1).url, /sub-orders\/seller\/12$/);
    assert.ok(mock.calls.every(call => call.options.cache === "no-store" && call.options.headers.Authorization === "Bearer secret"));
    for (const [action, body, endpoint] of [["accept", {}, "sub-orders/4/accept"], ["reject", { reason: " Out of stock " }, "sub-orders/4/reject"], ["ship", { courierName: " Courier ", trackingNumber: " TRACK " }, "shipping/orders/4/assign-tracking"]]) {
      const result = await mock.request(["4", action], "POST", body); assert.equal(result.status, 200);
      assert.ok(mock.calls.at(-1).url.endsWith(`${endpoint}?sellerId=12`));
      const payload = JSON.parse(mock.calls.at(-1).options.body); assert.equal(payload.sellerId, undefined);
      if (action === "ship") assert.deepEqual(payload, { courierName: "Courier", trackingNumber: "TRACK" });
    }
  });
});

test("BFF denies anonymous, other roles, inactive accounts/shops and foreign order before POST", async () => {
  for (const config of [{ token: null }, { role: "CUSTOMER" }, { role: "ADMIN" }, { status: "SUSPENDED" }, { shopStatus: "SUSPENDED" }, { result: order({ sellerId: 999 }) }]) {
    const mock = api(config);
    await usingFetch(mock.fetch, async () => { assert.ok([401, 403].includes((await mock.request(["4", "accept"], "POST")).status)); assert.ok(mock.calls.every(c => c.options.method !== "POST")); });
  }
});

test("BFF validates identifiers, origin, sellerId and trimmed field lengths; upstream failure stays an error", async () => {
  const mock = api();
  await usingFetch(mock.fetch, async () => {
    assert.equal((await mock.request(["4", "accept"], "POST", {}, "?sellerId=999")).status, 400);
    assert.equal((await mock.request(["4", "accept"], "POST", {}, "", "https://foreign.test")).status, 403);
    for (const id of ["0", "-1", "1.5", "9007199254740992"]) assert.equal((await mock.request([id])).status, 404);
    for (const [action, payload] of [["accept", { sellerId: 999 }], ["reject", { reason: " " }], ["reject", { reason: "x".repeat(256) }], ["ship", { courierName: " ", trackingNumber: "1" }], ["ship", { courierName: "C", trackingNumber: "x".repeat(101) }]]) assert.equal((await mock.request(["4", action], "POST", payload)).status, 400);
    assert.ok(mock.calls.every(c => c.options.method !== "POST"));
  });
  for (const config of [{ failure: true }, { result: {} }]) { const broken = api(config); await usingFetch(broken.fetch, async () => assert.ok([502, 503].includes((await broken.request()).status))); }
});

function hookHarness(fetch, id = "4") {
  const previous = { window: global.window, document: global.document, fetch: global.fetch };
  const hooks = [], pendingEffects = [], events = new Map(), timeouts = new Map(), intervals = new Map(), redirects = [];
  let cursor = 0, nextTimer = 0, currentId = id;
  const same = (a, b) => a && b && a.length === b.length && a.every((v, i) => Object.is(v, b[i]));
  const react = {
    useState(initial) { const i = cursor++; if (!(i in hooks)) hooks[i] = initial; return [hooks[i], value => hooks[i] = typeof value === "function" ? value(hooks[i]) : value]; },
    useRef(initial) { const i = cursor++; return hooks[i] ?? (hooks[i] = { current: initial }); },
    useCallback(fn, deps) { const i = cursor++; if (!same(hooks[i]?.deps, deps)) hooks[i] = { fn, deps }; return hooks[i].fn; },
    useEffect(fn, deps) { const i = cursor++; if (!same(hooks[i]?.deps, deps)) { pendingEffects.push(() => { hooks[i]?.cleanup?.(); hooks[i] = { deps, cleanup: fn() }; }); } },
  };
  const add = (kind, fn) => events.set(kind, fn), remove = kind => events.delete(kind);
  global.window = { addEventListener: add, removeEventListener: remove, setTimeout: (fn, ms) => { const id = ++nextTimer; timeouts.set(id, { fn, ms }); return id; }, clearTimeout: id => timeouts.delete(id), setInterval: (fn, ms) => { const id = ++nextTimer; intervals.set(id, { fn, ms }); return id; }, clearInterval: id => intervals.delete(id) };
  global.document = { visibilityState: "visible", addEventListener: add, removeEventListener: remove };
  global.fetch = fetch;
  const router = { replace: path => redirects.push(path) };
  const auth = { user: { id: "7", role: "SELLER", status: "ACTIVE" }, isLoading: false };
  const useHook = load("components/Seller/useSellerOrders.ts", { react, "next/navigation": { useRouter: () => router }, "@/app/context/AuthContext": { useAuth: () => auth }, "@/lib/sellerOrders": model }).useSellerOrders;
  const Harness = () => { cursor = 0; const result = useHook(currentId); pendingEffects.splice(0).forEach(fn => fn()); return result; };
  let disposed = false;
  const unmount = () => { if (disposed) return; disposed = true; hooks.forEach(hook => hook?.cleanup?.()); };
  return { render: Harness, events, timeouts, intervals, redirects, auth, setId: value => currentId = value, unmount, cleanup() { unmount(); Object.assign(global, previous); } };
}

test("visible polling/focus avoid overlap; hidden tabs stay idle; stale data survives failure and cleanup aborts requests", async () => {
  let calls = 0; const pending = deferred(); let lastSignal;
  const harness = hookHarness(async (_url, options) => { calls++; lastSignal = options.signal; if (calls === 1) return response({ shop, order: order() }); if (calls === 2) throw new Error("Offline"); return pending.promise; });
  try {
    harness.render(); await flush(); assert.equal(harness.render().data.orders.length, 1);
    assert.equal([...harness.intervals.values()][0].ms, 60000);
    global.document.visibilityState = "hidden"; [...harness.intervals.values()][0].fn(); assert.equal(calls, 1);
    global.document.visibilityState = "visible"; harness.events.get("visibilitychange")(); await flush();
    assert.equal(harness.render().error, "Offline"); assert.equal(harness.render().data.orders.length, 1);
    harness.render().refresh(); harness.events.get("focus")(); assert.equal(calls, 3);
    harness.unmount(); assert.equal(lastSignal.aborted, true); assert.equal(harness.events.size, 0); assert.equal(harness.intervals.size, 0);
    pending.resolve(response({ shop, order: order() })); await flush();
  } finally { harness.cleanup(); }
});

test("double action is blocked; timeout reconciles status before enabling actions without retrying POST", async () => {
  const pending = deferred(), latest = deferred(); const calls = [];
  const harness = hookHarness(async (url, options) => {
    calls.push({ url, options });
    if (options.method === "POST") { options.signal.addEventListener("abort", () => pending.reject(new Error("Aborted")), { once: true }); return pending.promise; }
    return calls.length === 1 ? response({ shop, order: order() }) : latest.promise;
  });
  try {
    harness.render(); await flush(); const before = harness.render();
    const running = before.perform("accept", {}); await before.perform("accept", {});
    assert.equal(calls.filter(c => c.options.method === "POST").length, 1); assert.equal(harness.render().acting, true);
    [...harness.timeouts.values()][0].fn(); await flush();
    assert.equal(harness.render().acting, true); assert.equal(calls.at(-1).options.method, undefined);
    latest.resolve(response({ shop, order: order({ orderStatus: "PREPARING" }) })); await running;
    assert.equal(harness.render().acting, false); assert.equal(harness.render().data.orders[0].orderStatus, "PREPARING");
    assert.match(harness.render().notice.text, /timed out.*status has changed/i);
    await harness.render().perform("accept", {}); assert.equal(calls.filter(c => c.options.method === "POST").length, 1);
  } finally { harness.cleanup(); }
});

test("failed reconciliation, unpaid orders and inconsistent legacy records disable every mutation", async () => {
  for (const current of [order({ paymentStatus: "PENDING", orderStatus: "PENDING_PAYMENT" }), order({ orderStatus: "PENDING_PAYMENT" }), order({ paymentStatus: null }), order()]) {
    let reads = 0, posts = 0;
    const harness = hookHarness(async (_url, options) => { if (options.method === "POST") { posts++; return response(null); } if (++reads === 1) return response({ shop, order: current }); throw new Error("Offline"); });
    try {
      harness.render(); await flush(); await harness.render().perform("accept", {});
      const expected = current === undefined ? 0 : current.orderStatus === "WAITING_SELLER_CONFIRM" && current.paymentStatus === "PAID" ? 1 : 0;
      assert.equal(posts, expected);
      if (posts) { assert.equal(harness.render().error, "Offline"); await harness.render().perform("accept", {}); assert.equal(posts, 1); }
    } finally { harness.cleanup(); }
  }
});

test("an action finishing after a route change cannot refresh or unlock the next order", async () => {
  const pending = deferred(), calls = [];
  const harness = hookHarness(async (url, options) => { calls.push(url); return options.method === "POST" ? pending.promise : response({ shop, order: order({ orderId: url.endsWith("/5") ? 5 : 4 }) }); });
  try {
    harness.render(); await flush(); const running = harness.render().perform("accept", {});
    harness.setId("5"); harness.render(); await flush();
    pending.resolve(response(null)); await running;
    assert.equal(harness.render().data.orders[0].orderId, 5); assert.equal(harness.render().notice, null);
    assert.equal(calls.filter(url => url.endsWith("/4")).length, 1);
  } finally { harness.cleanup(); }
});

test("first-load failure leaves data null; unauthorized response clears data and redirects", async () => {
  for (const status of [401, 403, 503]) {
    const harness = hookHarness(async () => response(null, status));
    try { harness.render(); await flush(); assert.equal(harness.render().data, null); assert.ok(harness.render().error); assert.equal(harness.redirects.length, status === 503 ? 0 : 1); } finally { harness.cleanup(); }
  }
});

test("real detail renders amounts/address/tracking and no sample data; stale state disables forms", () => {
  const React = require("react"); const { renderToStaticMarkup } = require("react-dom/server");
  const state = { data: { shop, orders: [order({ shipment: { courierName: "Real Courier", trackingNumber: "REAL-TRACK", shippingStatus: "SHIPPED" } })] }, loading: false, refreshing: false, error: "Offline", refresh() {}, acting: false, notice: null, perform() {} };
  const mocks = { "./useSellerOrders": { useSellerOrders: () => state }, "@/lib/sellerOrders": model, "./Shared": { StatusBadge: ({ status }) => React.createElement("span", null, status) }, "next/link": { default: ({ href, children, ...props }) => React.createElement("a", { href, ...props }, children) }, "@/components/Common/Breadcrumb": { default: () => null } };
  for (const [file, names] of [["card", ["Card", "CardContent", "CardHeader", "CardTitle"]], ["input", ["Input"]], ["label", ["Label"]], ["button", ["Button"]], ["skeleton", ["Skeleton"]]]) mocks[`@/components/ui/${file}`] = Object.fromEntries(names.map(name => [name, ({ children, variant: _variant, ...props }) => React.createElement(file === "button" ? "button" : file === "input" ? "input" : "div", props, children)]));
  const component = load("components/Seller/SellerOrder.tsx", mocks).default;
  const html = renderToStaticMarkup(React.createElement(component, { orderId: "4" }));
  assert.match(html, /Receiver/); assert.match(html, /REAL-TRACK/); assert.match(html, /\$22\.00/); assert.match(html, /<fieldset disabled/); assert.ok(!html.includes("demo"));
  state.error = ""; state.data.orders = [order({ customerName: null, shippingAddress: null, orderStatus: "PENDING_PAYMENT" })];
  const legacy = renderToStaticMarkup(React.createElement(component, { orderId: "4" }));
  assert.match(legacy, /Shipping address unavailable/); assert.match(legacy, /inconsistent/); assert.ok(!legacy.includes("Accept and confirm"));
});
