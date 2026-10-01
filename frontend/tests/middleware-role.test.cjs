const { test } = require("node:test");
const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const { resolve } = require("node:path");
const ts = require("typescript");

const source = readFileSync(resolve(__dirname, "../src/middleware.ts"), "utf8");
const compiled = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 },
}).outputText;
const response = {
  next: () => ({ kind: "next" }),
  redirect: (url) => ({ kind: "redirect", url: String(url) }),
};
const moduleExports = {};
new Function("require", "exports", compiled)(() => ({ NextResponse: response }), moduleExports);
const { middleware } = moduleExports;

test("Add Product permits only a verified Seller, including nested product paths", async () => {
  assert.equal(new URL((await middleware(request("/seller/products/add"))).url).pathname, "/signin");
  for (const role of ["CUSTOMER", "ADMIN", "SELLER"]) await withIdentity(role, async () => {
    const result = await middleware(request("/seller/products/add", { auth_token: "session" }));
    assert.equal(result.kind, role === "SELLER" ? "next" : "redirect");
  });
});

function request(path, cookies = {}) {
  return {
    url: `http://localhost:3000${path}`,
    nextUrl: { pathname: path, searchParams: new URLSearchParams() },
    cookies: { get: (name) => cookies[name] && { value: cookies[name] } },
  };
}

async function withIdentity(role, action) {
  const previousFetch = global.fetch;
  global.fetch = async () => ({
    ok: role !== null,
    json: async () => ({ success: true, data: { role, userId: 1 } }),
  });
  try { await action(); } finally { global.fetch = previousFetch; }
}

test("forged admin role cookie cannot authorize a customer session", async () => {
  await withIdentity("CUSTOMER", async () => {
    const result = await middleware(request("/admin", { auth_token: "customer-token", user_role: "ADMIN" }));
    assert.equal(result.kind, "redirect");
    assert.equal(new URL(result.url).pathname, "/");
  });
});

test("forged role cookie cannot replace a missing or expired session", async () => {
  const noToken = await middleware(request("/admin", { user_role: "ADMIN" }));
  assert.equal(new URL(noToken.url).pathname, "/signin");
  await withIdentity(null, async () => {
    const expired = await middleware(request("/admin", { auth_token: "expired", user_role: "ADMIN" }));
    assert.equal(new URL(expired.url).pathname, "/signin");
  });
});

test("verified admin may enter admin; forged seller role does not redirect a customer", async () => {
  await withIdentity("ADMIN", async () => {
    assert.equal((await middleware(request("/admin", { auth_token: "admin-token" }))).kind, "next");
  });
  await withIdentity("CUSTOMER", async () => {
    assert.equal((await middleware(request("/", { auth_token: "customer-token", user_role: "SELLER" }))).kind, "next");
  });
});
