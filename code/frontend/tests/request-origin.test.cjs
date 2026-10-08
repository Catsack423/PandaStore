const { test } = require("node:test");
const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const { resolve } = require("node:path");
const ts = require("typescript");

function load(file, mocks = {}) {
  const compiled = ts.transpileModule(readFileSync(resolve(__dirname, "../src", file), "utf8"), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 },
  }).outputText;
  const exports = {};
  new Function("require", "exports", compiled)(name => mocks[name] ?? require(name), exports);
  return exports;
}

const originCheck = load("lib/requestOrigin.ts");
const publicOrigin = "https://kim-untakeable-shirlee.ngrok-free.dev";
function request(origin, payload = [], forwardedHost = "localhost:3000") {
  return {
    nextUrl: new URL("http://localhost:3000/api/seller-product-images/cleanup"),
    headers: new Headers({ ...(origin === null ? {} : { origin }), "x-forwarded-host": forwardedHost, "x-forwarded-proto": "https" }),
    json: async () => payload,
  };
}

async function withOrigins(value, work) {
  const previous = process.env.ALLOWED_REQUEST_ORIGINS;
  if (value === undefined) delete process.env.ALLOWED_REQUEST_ORIGINS;
  else process.env.ALLOWED_REQUEST_ORIGINS = value;
  try { await work(); } finally {
    if (previous === undefined) delete process.env.ALLOWED_REQUEST_ORIGINS;
    else process.env.ALLOWED_REQUEST_ORIGINS = previous;
  }
}

test("public allowlist uses exact origins and ignores spoofed forwarded hosts", async () => {
  await withOrigins(` http://localhost:3000, ${publicOrigin} , `, () => {
    for (const origin of ["http://localhost:3000", publicOrigin]) {
      assert.equal(originCheck.hasAllowedRequestOrigin(request(origin)), true);
    }
    for (const origin of [null, "null", "https://other.ngrok-free.dev", `${publicOrigin}.evil.test`, `${publicOrigin}/`, `${publicOrigin}:3000`, publicOrigin.replace("https:", "http:")]) {
      assert.equal(originCheck.hasAllowedRequestOrigin(request(origin, [], origin ?? "evil.test")), false);
    }
  });
  await withOrigins(publicOrigin, () => {
    assert.equal(originCheck.hasAllowedRequestOrigin(request("http://localhost:3000")), false);
  });
});

test("unset config preserves same-origin checks; empty or wildcard config never opens access", async () => {
  await withOrigins(undefined, () => {
    assert.equal(originCheck.hasAllowedRequestOrigin(request("http://localhost:3000")), true);
    for (const origin of [publicOrigin, null, "null"]) {
      assert.equal(originCheck.hasAllowedRequestOrigin(request(origin)), false);
    }
  });
  for (const value of ["", " , ", "*", "https://*.ngrok-free.dev"]) {
    await withOrigins(value, () => {
      for (const origin of ["http://localhost:3000", publicOrigin, null, "null"]) {
        assert.equal(originCheck.hasAllowedRequestOrigin(request(origin)), false);
      }
    });
  }
});

test("image cleanup accepts configured public origin and denies foreign origins before deletion", async () => {
  const removed = [];
  const route = load("app/api/seller-product-images/cleanup/route.ts", {
    "next/server": { NextResponse: { json: (body, options) => ({ body, status: 200, ...options }) } },
    "@/lib/requestOrigin": originCheck,
    "@/ServerAction/products": { removeSellerProductImage: async image => { removed.push(image); return { success: true }; } },
  });
  const image = { key: "pending.png", removalToken: "a".repeat(64) };
  await withOrigins(`http://localhost:3000,${publicOrigin}`, async () => {
    for (const origin of ["http://localhost:3000", publicOrigin]) {
      assert.equal((await route.POST(request(origin, [image]))).status, 200);
    }
    assert.deepEqual(removed, [image, image]);
    for (const origin of ["https://other.ngrok-free.dev", null, "null"]) {
      assert.equal((await route.POST(request(origin, [image]))).status, 403);
    }
    assert.equal(removed.length, 2);
    assert.equal((await route.POST(request(publicOrigin, [{ key: image.key }]))).status, 400);
    assert.equal(removed.length, 2);
  });
});
