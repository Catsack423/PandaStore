const { test } = require("node:test");
const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const { resolve } = require("node:path");
const ts = require("typescript");

function load(file) {
  const compiled = ts.transpileModule(readFileSync(resolve(__dirname, "../src", file), "utf8"), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 },
  }).outputText;
  const exports = {};
  new Function("require", "exports", compiled)(name =>
    name === "@/lib/currency" ? currency : require(name), exports);
  return exports;
}
const currency = load("lib/currency.ts");

test("baht amounts retain their value and hide unnecessary decimal places", () => {
  for (const [value, expected] of [[0, "0 THB"], [0.01, "0.01 THB"],
    [1290, "1,290 THB"], [35.5, "35.5 THB"], [35.75, "35.75 THB"], [-25, "-25 THB"],
    [9999999999.99, "9,999,999,999.99 THB"]]) {
    assert.equal(currency.formatBaht(value), expected);
  }
});

test("checkout, payment and seller orders share the same currency formatter", () => {
  const checkout = load("components/Checkout/api.ts");
  const seller = load("lib/sellerOrders.ts");
  assert.equal(checkout.money(3545), "3,545 THB");
  assert.equal(seller.sellerOrderCurrency.format(3545), checkout.money(3545));
});
