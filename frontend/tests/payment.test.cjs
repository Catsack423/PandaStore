const { test } = require("node:test");
const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const { resolve } = require("node:path");
const ts = require("typescript");

function load(file, mocks = {}) {
  const compiled = ts.transpileModule(readFileSync(resolve(__dirname, "../src", file), "utf8"), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, jsx: ts.JsxEmit.ReactJSX, target: ts.ScriptTarget.ES2020 },
  }).outputText;
  const exports = {};
  new Function("require", "exports", compiled)(name => name in mocks ? mocks[name] : require(name), exports);
  return exports;
}
const api = load("components/Payment/api.ts");
let token = "customer-session", calls = [], paid = false, transaction = null, failure = null;
const pending = { orderGroupId: 6, groupNumber: "group-six", grandTotal: 79, totalProductsAmount: 49,
  totalShippingFee: 30, paymentStatus: "PENDING", subOrders: [{ orderStatus: "PENDING_PAYMENT" }] };
const settled = { ...pending, paymentStatus: "PAID", subOrders: [{ orderStatus: "WAITING_SELLER_CONFIRM" }] };
const cookies = { cookies: async () => ({ get: () => token ? { value: token } : undefined }) };
const proxy = load("app/api/payment/[orderGroupId]/route.ts", {
  "next/headers": cookies, "next/server": { NextResponse: { json: (body, options) => ({ body, ...options }) } },
  "@/components/Payment/api": api,
});
async function isolated(action) {
  const previousFetch = global.fetch;
  token = "customer-session"; calls = []; paid = false; transaction = null; failure = null;
  global.fetch = async (url, options) => {
    calls.push({ url, options });
    if (failure === "network") throw new Error("private upstream error");
    const path = new URL(url).pathname;
    const status = failure?.path === path ? failure.status : 200;
    let data;
    if (path.startsWith("/api/checkout/orders/")) data = paid ? settled : pending;
    else if (path.includes("/order-group/")) data = { status: "PENDING", paymentMethod: "BANK_TRANSFER", gatewayTransactionId: transaction };
    else if (path.endsWith("/initiate")) { transaction = "existing-txn"; data = { gatewayTransactionId: transaction }; }
    else if (path.endsWith("/simulate")) { paid = true; data = { status: "SUCCESS" }; }
    return { status, ok: status === 200, json: async () => ({ success: status === 200, data, message: "Upstream error" }) };
  };
  try { await action(); } finally { global.fetch = previousFetch; }
}
const confirm = (id = "6") => proxy.POST({}, { params: Promise.resolve({ orderGroupId: id }) });

test("frontend verifies ownership, initializes the persisted method/total and calls the existing mock API", () => isolated(async () => {
  const result = await confirm();
  assert.equal(result.body.data, settled);
  assert.deepEqual(calls.map(call => new URL(call.url).pathname), [
    "/api/checkout/orders/6", "/api/payments/order-group/6", "/api/payments/initiate", "/api/payments/simulate", "/api/checkout/orders/6",
  ]);
  assert.deepEqual(JSON.parse(calls[2].options.body), { orderGroupId: 6, paymentMethod: "BANK_TRANSFER", amount: 79 });
  assert.deepEqual(JSON.parse(calls[3].options.body), { orderGroupId: 6, isSuccess: true });
  assert.ok(calls.every(call => call.options.cache === "no-store" && call.options.headers.Authorization === "Bearer customer-session"));
}));

test("existing transaction is reused without reinitializing payment", () => isolated(async () => {
  transaction = "already-initialized";
  await confirm();
  assert.equal(calls.some(call => call.url.endsWith("/initiate")), false);
}));

test("invalid identifiers and anonymous sessions cannot call payment APIs", () => isolated(async () => {
  for (const id of ["0", "-1", "abc", "9007199254740992"]) assert.equal((await confirm(id)).status, 404);
  token = null;
  assert.equal((await confirm()).status, 401);
  assert.equal(calls.length, 0);
}));

test("missing or foreign orders are rejected before payment initialization", () => isolated(async () => {
  for (const status of [403, 404]) {
    calls = []; failure = { path: "/api/checkout/orders/6", status };
    assert.equal((await confirm()).status, status);
    assert.equal(calls.length, 1);
  }
}));

test("paid orders reject a second confirmation", () => isolated(async () => {
  paid = true;
  assert.equal((await confirm()).status, 409);
  assert.equal(calls.length, 1);
}));

test("initialization errors stop simulation; outages provide retry guidance", () => isolated(async () => {
  failure = { path: "/api/payments/initiate", status: 409 };
  assert.equal((await confirm()).status, 409);
  assert.equal(calls.some(call => call.url.endsWith("/simulate")), false);
  failure = "network";
  const result = await confirm();
  assert.equal(result.status, 503);
  assert.match(result.body.message, /Refresh order status/);
  assert.doesNotMatch(result.body.message, /private upstream error/);
}));

const page = load("app/(site)/(pages)/payment/[orderGroupId]/page.tsx", {
  "next/headers": cookies,
  "next/navigation": { notFound: () => { throw new Error("NOT_FOUND"); }, redirect: () => { throw new Error("SIGN_IN"); } },
  "@/components/Payment": { default: () => null },
}).default;

test("Payment page loads the actual shipping-inclusive order without feature flags or caching", () => isolated(async () => {
  const element = await page({ params: Promise.resolve({ orderGroupId: "6" }) });
  assert.equal(element.props.initialOrder, pending);
  assert.equal(element.props.mockEnabled, undefined);
  assert.equal(element.props.qrSource, "/images/payment/qr-code.svg");
  assert.equal(calls[0].options.cache, "no-store");
}));

test("client confirmation calls the frontend proxy and returns backend order state", async () => {
  const previousFetch = global.fetch;
  let request;
  global.fetch = async (url, options) => { request = { url, options }; return { ok: true, json: async () => ({ success: true, data: settled }) }; };
  try {
    assert.equal(await api.confirmPayment(6), settled);
    assert.equal(request.url, "/api/payment/6");
    assert.equal(request.options.method, "POST");
  } finally { global.fetch = previousFetch; }
});

test("paid, cancelled, mixed and empty groups cannot be confirmed", () => {
  assert.equal(api.isPendingPayment(pending), true);
  assert.equal(api.isPendingPayment(settled), false);
  assert.equal(api.isPendingPayment({ ...pending, subOrders: [] }), false);
  assert.equal(api.isPendingPayment({ ...pending, subOrders: [{ orderStatus: "PENDING_PAYMENT" }, { orderStatus: "CANCELLED" }] }), false);
  assert.equal(api.paymentStatusLabel({ ...pending, paymentStatus: "FAILED", subOrders: [{ orderStatus: "CANCELLED" }] }), "Order cancelled");
});
