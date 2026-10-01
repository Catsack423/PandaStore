const { test } = require("node:test");
const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const { resolve } = require("node:path");
const ts = require("typescript");

function load(file, mocks) {
  const compiled = ts.transpileModule(readFileSync(resolve(__dirname, "../src", file), "utf8"), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, jsx: ts.JsxEmit.ReactJSX, target: ts.ScriptTarget.ES2020 },
  }).outputText;
  const exports = {};
  new Function("require", "exports", compiled)(name => name in mocks ? mocks[name] : require(name), exports);
  return exports;
}

let token = "customer-session";
const cookies = { cookies: async () => ({ get: () => token ? { value: token } : undefined }) };
const response = { NextResponse: { json: (body, options) => ({ body, ...options }) } };
const proxy = load("app/api/checkout/[[...path]]/route.ts", { "next/headers": cookies, "next/server": response });
let calls = [];
let upstreamStatus = 200;
let upstreamThrows = false;
const pending = { orderGroupId: 6, groupNumber: "group-six", grandTotal: 79, totalProductsAmount: 49, totalShippingFee: 30,
  paymentStatus: "PENDING", subOrders: [{ orderStatus: "PENDING_PAYMENT" }] };
let upstreamOrder = pending;

async function isolated(action) {
  const previous = { fetch: global.fetch, privateFlag: process.env.MOCK_PAYMENT_ENABLED, publicFlag: process.env.NEXT_PUBLIC_MOCK_PAYMENT };
  token = "customer-session"; calls = []; upstreamStatus = 200; upstreamThrows = false; upstreamOrder = pending;
  process.env.MOCK_PAYMENT_ENABLED = "true"; process.env.NEXT_PUBLIC_MOCK_PAYMENT = "true";
  global.fetch = async (url, options) => {
    calls.push({ url, options });
    if (upstreamThrows) throw new Error("private upstream error");
    return { status: upstreamStatus, ok: upstreamStatus === 200, json: async () => ({ success: upstreamStatus === 200, data: upstreamOrder, message: "Order not found" }) };
  };
  try { await action(); } finally {
    global.fetch = previous.fetch;
    for (const [name, value] of [["MOCK_PAYMENT_ENABLED", previous.privateFlag], ["NEXT_PUBLIC_MOCK_PAYMENT", previous.publicFlag]]) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
  }
}

function post(path) {
  return proxy.POST({ method: "POST", text: async () => "{}" }, { params: Promise.resolve({ path: path.split("/") }) });
}

test("payment proxy requires both flags and cannot be enabled by the public flag alone", () => isolated(async () => {
  for (const privateFlag of [undefined, "false", "TRUE", "true"]) {
    if (privateFlag === undefined) delete process.env.MOCK_PAYMENT_ENABLED; else process.env.MOCK_PAYMENT_ENABLED = privateFlag;
    for (const publicFlag of ["false", "true"]) {
      process.env.NEXT_PUBLIC_MOCK_PAYMENT = publicFlag;
      const result = await post("orders/6/confirm-payment");
      assert.equal(result.status, privateFlag === "true" && publicFlag === "true" ? 200 : 403);
    }
  }
  assert.equal(calls.length, 1);
}));

test("payment actions use session authentication and never cache backend responses", () => isolated(async () => {
  await post("orders/6/confirm-payment");
  assert.equal(calls[0].options.headers.Authorization, "Bearer customer-session");
  assert.equal(calls[0].options.cache, "no-store");
  assert.ok(calls[0].url.endsWith("/api/checkout/orders/6/confirm-payment"));
  token = null;
  assert.equal((await post("orders/6/cancel")).status, 401);
  assert.equal(calls.length, 1);
}));

test("cancellation remains available with mock payment disabled; invalid actions are rejected", () => isolated(async () => {
  process.env.MOCK_PAYMENT_ENABLED = "false";
  assert.equal((await post("orders/6/cancel")).status, 200);
  for (const path of ["orders/0/cancel", "orders/-1/confirm-payment", "orders/6/refund", "payments/simulate"]) {
    assert.equal((await post(path)).status, 404);
  }
  assert.equal(calls.length, 1);
}));

test("proxy preserves ownership and conflict errors and gives retry guidance on outage", () => isolated(async () => {
  for (const status of [404, 409, 403]) {
    upstreamStatus = status;
    assert.equal((await post("orders/6/confirm-payment")).status, status);
  }
  upstreamThrows = true;
  const result = await post("orders/6/cancel");
  assert.equal(result.status, 503);
  assert.match(result.body.message, /Refresh order status/);
  assert.doesNotMatch(result.body.message, /private upstream error/);
}));

const navigation = {
  notFound: () => { throw new Error("NOT_FOUND"); },
  redirect: url => { throw new Error(`REDIRECT:${url}`); },
};
const Payment = () => null;
const page = load("app/(site)/(pages)/payment/[orderGroupId]/page.tsx", {
  "next/headers": cookies, "next/navigation": navigation,
  "@/components/Payment": { default: Payment },
}).default;
const renderPage = id => page({ params: Promise.resolve({ orderGroupId: id }) });

test("Payment page loads actual shipping-inclusive order data without cache", () => isolated(async () => {
  const element = await renderPage("6");
  assert.equal(element.props.initialOrder, pending);
  assert.equal(element.props.initialOrder.grandTotal, 79);
  assert.equal(element.props.mockEnabled, true);
  assert.equal(element.props.qrSource, "/images/payment/qr-code.svg");
  assert.equal(calls[0].options.cache, "no-store");
  assert.equal(calls[0].options.headers.Authorization, "Bearer customer-session");
  process.env.MOCK_PAYMENT_ENABLED = "false";
  assert.equal((await renderPage("6")).props.mockEnabled, false);
}));

test("Payment page rejects invalid, missing, and non-owned groups; expired sessions sign in", () => isolated(async () => {
  for (const id of ["0", "-1", "abc", "9007199254740992"]) await assert.rejects(renderPage(id), /NOT_FOUND/);
  assert.equal(calls.length, 0);
  for (const status of [403, 404]) {
    upstreamStatus = status;
    await assert.rejects(renderPage("6"), /NOT_FOUND/);
  }
  upstreamStatus = 401;
  await assert.rejects(renderPage("6"), /REDIRECT:\/signin/);
  token = null;
  await assert.rejects(renderPage("6"), /REDIRECT:\/signin/);
}));

test("Payment page has a retryable service error instead of fake order data", () => isolated(async () => {
  upstreamThrows = true;
  const element = await renderPage("6");
  assert.equal(element.props.initialOrder, null);
  assert.equal(element.props.loadError, true);
}));

const serviceCalls = [];
const api = load("components/Payment/api.ts", { "@/components/Checkout/api": {
  checkoutApi: async (path, body) => { serviceCalls.push({ path, body }); return pending; },
} });
test("payment state disables transitions for paid, cancelled, failed, mixed or empty groups", () => {
  assert.equal(api.isPendingPayment(pending), true);
  for (const status of ["PAID", "FAILED", "REFUNDED", "PARTIALLY_REFUNDED"]) assert.equal(api.isPendingPayment({ ...pending, paymentStatus: status }), false);
  assert.equal(api.isPendingPayment({ ...pending, subOrders: [] }), false);
  assert.equal(api.isPendingPayment({ ...pending, subOrders: [{ orderStatus: "PENDING_PAYMENT" }, { orderStatus: "CANCELLED" }] }), false);
  assert.equal(api.paymentStatusLabel({ ...pending, paymentStatus: "FAILED", subOrders: [{ orderStatus: "CANCELLED" }] }), "Order cancelled");
  assert.equal(api.paymentStatusLabel({ ...pending, paymentStatus: "PAID", subOrders: [{ orderStatus: "WAITING_SELLER_CONFIRM" }] }), "Payment successful");
});

test("confirmPayment and cancelOrder send group IDs without browser-supplied amounts or statuses", async () => {
  await api.confirmPayment(6); await api.cancelOrder(6);
  assert.deepEqual(serviceCalls, [{ path: "orders/6/confirm-payment", body: {} }, { path: "orders/6/cancel", body: {} }]);
});
