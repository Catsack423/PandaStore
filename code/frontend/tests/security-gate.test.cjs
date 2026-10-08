const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { resolve } = require('node:path');
const ts = require('typescript');

function load(file, mocks = {}) {
  const compiled = ts.transpileModule(readFileSync(resolve(__dirname, '../src', file), 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 },
  }).outputText;
  const exports = {};
  new Function('require', 'exports', compiled)(name => mocks[name] ?? require(name), exports);
  return exports;
}
const origin = load('lib/requestOrigin.ts');
const { middleware } = load('middleware.ts', {
  './lib/requestOrigin': origin,
  'next/server': { NextResponse: {
    next: () => ({ status: 200 }),
    json: (body, options) => ({ body, ...options }),
  } },
});

test('cookie API writes require exact configured origin, including login and logout', async () => {
  const previous = process.env.ALLOWED_REQUEST_ORIGINS;
  process.env.ALLOWED_REQUEST_ORIGINS = 'https://store.example';
  try {
    for (const path of ['/api/auth/login', '/api/auth/logout', '/api/auth/reset-password', '/api/checkout/order', '/api/auth/me']) {
      for (const method of ['POST', 'PUT', 'PATCH', 'DELETE']) {
        for (const value of [null, 'null', 'https://evil.example', 'https://store.example.evil.test', 'https://store.example']) {
          const request = {
            method, nextUrl: new URL('https://store.example' + path),
            headers: new Headers(value ? { origin: value } : {}),
          };
          assert.equal((await middleware(request)).status, value === 'https://store.example' ? 200 : 403);
        }
      }
    }
    // UploadThing verifies its service webhook signatures inside its route handler.
    assert.equal((await middleware({ method: 'POST', nextUrl: new URL('https://store.example/api/uploadthing') })).status, 200);
    assert.equal((await middleware({ method: 'GET', nextUrl: new URL('https://store.example/api/auth/me') })).status, 200);
  } finally {
    if (previous === undefined) delete process.env.ALLOWED_REQUEST_ORIGINS;
    else process.env.ALLOWED_REQUEST_ORIGINS = previous;
  }
});

test('Next.js returns security headers for every path and production CSP forbids eval', async () => {
  const config = require('../next.config.js');
  const previous = process.env.NODE_ENV;
  process.env.NODE_ENV = 'production';
  try {
    const [rule] = await config.headers();
    const headers = Object.fromEntries(rule.headers.map(header => [header.key, header.value]));
    assert.equal(rule.source, '/:path*');
    assert.equal(headers['X-Content-Type-Options'], 'nosniff');
    assert.equal(headers['X-Frame-Options'], 'DENY');
    assert.equal(headers['Referrer-Policy'], 'strict-origin-when-cross-origin');
    assert.match(headers['Content-Security-Policy'], /frame-ancestors 'none'/);
    assert.match(headers['Content-Security-Policy'], /object-src 'none'/);
    assert.doesNotMatch(headers['Content-Security-Policy'], /unsafe-eval/);
  } finally {
    if (previous === undefined) delete process.env.NODE_ENV;
    else process.env.NODE_ENV = previous;
  }
});

test('login and password rotation return tokens only through secure HttpOnly cookies', async () => {
  const previousFetch = global.fetch;
  const previousMode = process.env.NODE_ENV;
  process.env.NODE_ENV = 'production';
  const user = { userId: 1, username: 'customer', email: 'customer@example.test', role: 'CUSTOMER' };
  global.fetch = async () => ({ ok: true, status: 200, json: async () => ({
    success: true, data: { success: true, token: 'private.session.token', user },
  }) });
  try {
    // Use a response mock whose cookie API retains the actual attributes passed by the route.
    let result;
    const responses = load('app/api/auth/[action]/route.ts', {
      'next/headers': { cookies: async () => ({ get: () => ({ value: 'old.session.token' }) }) },
      'next/server': { NextResponse: { json: (body, options) => {
        result = { body, ...options, storedCookies: [] };
        result.cookies = {
          set: (name, value, attributes) => result.storedCookies.push({ name, value, attributes }),
          delete() {},
        };
        return result;
      } } },
    });
    for (const action of ['login', 'reset-password']) {
      const response = await responses.POST({ json: async () => ({ password: 'new-password' }) }, {
        params: Promise.resolve({ action }),
      });
      assert.doesNotMatch(JSON.stringify(response.body), /private\.session\.token|"token"/);
      assert.deepEqual(response.storedCookies[0].attributes, {
        httpOnly: true, secure: true, sameSite: 'lax', path: '/', maxAge: 86400,
      });
      assert.equal(response.storedCookies[0].name, 'auth_token');
      assert.equal(response.storedCookies[0].value, 'private.session.token');
    }
  } finally {
    global.fetch = previousFetch;
    if (previousMode === undefined) delete process.env.NODE_ENV;
    else process.env.NODE_ENV = previousMode;
  }
});
