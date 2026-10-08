const { test } = require("node:test");
const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const { resolve } = require("node:path");
const ts = require("typescript");
const React = require("react");

function load(path, mocks = {}) {
  const compiled = ts.transpileModule(readFileSync(resolve(__dirname, "../src", path), "utf8"), {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020,
      jsx: ts.JsxEmit.ReactJSX, esModuleInterop: true,
    },
  }).outputText;
  const exports = {};
  new Function("require", "exports", compiled)((name) => {
    if (name in mocks) return mocks[name];
    if (name.startsWith("@/components/ui/")) {
      return new Proxy({}, { get: (_, key) => key === "__esModule" ? false :
        key === "Input" ? "input" : key === "Button" ? "button" : "div" });
    }
    if (name === "next/link") return "a";
    if (name === "@/components/Common/Breadcrumb") return () => null;
    return require(name);
  }, exports);
  return exports;
}

function find(tree, predicate) {
  if (!tree || typeof tree !== "object") return null;
  if (Array.isArray(tree)) {
    for (const child of tree) { const match = find(child, predicate); if (match) return match; }
    return null;
  }
  if (predicate(tree)) return tree;
  if (typeof tree.type === "function") return find(tree.type(tree.props), predicate);
  return find(tree.props?.children, predicate);
}

function harness(states, auth) {
  let index = 0;
  const updates = [];
  const pending = [];
  const destinations = [];
  return {
    updates, destinations,
    mocks: {
      react: { ...React,
        useState: (initial) => {
          const slot = index++;
          return [slot < states.length ? states[slot] : initial,
            (value) => updates.push({ slot, value })];
        },
        useTransition: () => [false, (action) => pending.push(Promise.resolve(action()))],
      },
      "next/navigation": { useRouter: () => ({
        replace: (path) => destinations.push(path), push: (path) => destinations.push(path), refresh() {},
      }) },
      "@/app/context/AuthContext": { useAuth: () => auth },
    },
    async flush() { while (pending.length) await pending.shift(); },
  };
}

const details = {
  username: "shopper", fullName: "Test Shopper", email: "shopper@example.com",
  phoneNumber: "0812345678", password: "sample123", confirmPassword: "sample123",
};

function signup(data, auth) {
  const h = harness([data, {}, false], auth);
  h.mocks["./CustomerForm"] = load("components/Auth/Signup/customer/CustomerForm.tsx");
  const Component = load("components/Auth/Signup/customer/index.tsx", h.mocks).default;
  return { h, form: find(Component(), (node) => node.type === "form") };
}

test("sign in prevents navigation and submits credentials through login", async () => {
  const calls = [];
  const h = harness([" shopper@example.com ", "sample123", ""], {
    login: async (...args) => { calls.push(args); return { success: true, user: { role: "CUSTOMER" } }; },
  });
  const Component = load("components/Auth/Signin/index.tsx", h.mocks).default;
  const form = find(Component(), (node) => node.type === "form");
  assert.equal(form.props.method, "post");
  let prevented = false;
  const previousWindow = global.window;
  global.window = { location: { search: "?callbackUrl=%2Fmy-account" } };
  try {
    form.props.onSubmit({ preventDefault() { prevented = true; } });
    await h.flush();
    assert.equal(prevented, true);
    assert.deepEqual(calls, [["shopper@example.com", "sample123"]]);
    assert.deepEqual(h.destinations, ["/my-account"]);
  } finally { global.window = previousWindow; }
});

test("wrong credentials show an error without navigating", async () => {
  const h = harness(["shopper", "wrong123", ""], {
    login: async () => ({ success: false, error: "Invalid credentials" }),
  });
  const Component = load("components/Auth/Signin/index.tsx", h.mocks).default;
  find(Component(), (node) => node.type === "form").props.onSubmit({ preventDefault() {} });
  await h.flush();
  assert.ok(h.updates.some(({ value }) => value === "Invalid credentials"));
  assert.deepEqual(h.destinations, []);
});

test("signup sends normalized fields, preserves password, and signs in after registration", async () => {
  const calls = [];
  const { h, form } = signup({ ...details, username: " shopper ", email: " shopper@example.com ", phoneNumber: "081-234 5678" }, {
    registerCustomer: async (body) => { calls.push(body); return { success: true }; },
    login: async (...args) => { calls.push(args); return { success: true }; },
  });
  assert.equal(form.props.method, "post");
  let prevented = false;
  form.props.onSubmit({ preventDefault() { prevented = true; } });
  await h.flush();
  assert.equal(prevented, true);
  assert.deepEqual(calls, [details, [details.email, details.password]]);
  assert.deepEqual(h.destinations, ["/"]);
});

test("signup rejects short normalized usernames, blank, mismatched, and oversized UTF-8 passwords before requests", async () => {
  for (const changes of [
    { username: " a " }, { password: "      ", confirmPassword: "      " },
    { confirmPassword: "different" }, { password: "ก".repeat(25), confirmPassword: "ก".repeat(25) },
  ]) {
    let calls = 0;
    const { h, form } = signup({ ...details, ...changes }, {
      registerCustomer: async () => { calls++; return { success: true }; },
    });
    form.props.onSubmit({ preventDefault() {} });
    await h.flush();
    assert.equal(calls, 0);
    assert.ok(h.updates.some(({ value }) => value && Object.keys(value).length > 0));
  }
});

test("signup accepts the 72 byte UTF-8 password boundary", async () => {
  let calls = 0;
  const password = "ก".repeat(24);
  const { h, form } = signup({ ...details, password, confirmPassword: password }, {
    registerCustomer: async () => { calls++; return { success: false, error: "Existing account" }; },
  });
  form.props.onSubmit({ preventDefault() {} });
  await h.flush();
  assert.equal(calls, 1);
  assert.ok(h.updates.some(({ value }) => value?.general === "Existing account"));
});

test("login API forwards credentials only in POST body and stores session in HttpOnly cookie", async () => {
  const route = load("app/api/auth/[action]/route.ts");
  const previousFetch = global.fetch;
  const forwarded = [];
  global.fetch = async (url, options) => {
    forwarded.push({ url, options });
    return { ok: true, status: 200, json: async () => ({ success: true, data: {
      token: "test-session", user: { userId: 1, username: "shopper", email: details.email, role: "CUSTOMER", status: "ACTIVE" },
    } }) };
  };
  try {
    const credentials = { usernameOrEmail: details.email, password: details.password };
    const response = await route.POST(new Request("http://localhost:3000/api/auth/login", {
      method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(credentials),
    }), { params: Promise.resolve({ action: "login" }) });
    assert.equal(response.status, 200);
    assert.equal(new URL(forwarded[0].url).pathname, "/api/auth/login");
    assert.equal(new URL(forwarded[0].url).search, "");
    assert.equal(forwarded[0].options.method, "POST");
    assert.deepEqual(JSON.parse(forwarded[0].options.body), credentials);
    const body = await response.text();
    assert.ok(!body.includes(details.password));
    assert.ok(!body.includes("test-session"));
    const cookie = response.headers.get("set-cookie");
    assert.match(cookie, /auth_token=test-session/);
    assert.match(cookie, /HttpOnly/i);
    assert.match(cookie, /SameSite=lax/i);
    const get = await route.GET(new Request("http://localhost:3000/api/auth/login"), {
      params: Promise.resolve({ action: "login" }),
    });
    assert.equal(get.status, 404);
    assert.equal(forwarded.length, 1);
  } finally { global.fetch = previousFetch; }
});
