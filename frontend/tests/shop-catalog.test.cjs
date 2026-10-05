const { test } = require("node:test");
const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const { resolve } = require("node:path");
const ts = require("typescript");

// Exercise the actual pure TypeScript helpers without adding a test dependency.
const source = readFileSync(resolve(__dirname, "../src/components/ShopWithSidebar/catalog.ts"), "utf8");
const compiled = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 },
}).outputText;
const catalog = {};
new Function("exports", compiled)(catalog);
const { catalogCategories, filterAndSortProducts, paginateProducts, PAGE_SIZE } = catalog;
const product = (id, overrides = {}) => ({
  id, title: `Product ${id}`, reviews: 0, price: 100, discountedPrice: 100, categoryIds: [], ...overrides,
});
const ids = (products) => products.map((item) => item.id);

test("category IDs include MacBook and exclude misleading titles; multiple categories use OR", () => {
  const products = [
    product(1, { title: "MacBook Air", categoryIds: [4] }),
    product(2, { title: "Laptop stand", categoryIds: [1] }),
    product(3, { categoryIds: [2, 5] }),
  ];
  assert.deepEqual(ids(filterAndSortProducts(products, new Set([4, 5]), 0, 100, "oldest")), [1, 3]);
});

test("category counts come from products and count duplicate IDs once", () => {
  assert.deepEqual(catalogCategories([
    product(1, { categoryIds: [4, 4] }), product(2, { categoryIds: [4, 9] }),
  ], [{ id: 4, name: "Laptop", products: 999 }]), [
    { id: 9, name: "Category 9", products: 1 }, { id: 4, name: "Laptop", products: 2 },
  ]);
});

test("price filtering uses the discounted price and includes both boundaries and expensive products", () => {
  const products = [
    product(1, { price: 200, discountedPrice: 50 }),
    product(2, { price: 15000, discountedPrice: 15000 }),
    product(3, { discountedPrice: 49 }),
  ];
  assert.deepEqual(ids(filterAndSortProducts(products, new Set(), 50, 15000, "price-asc")), [1, 2]);
  assert.deepEqual(filterAndSortProducts(products, new Set([99]), 0, 15000, "latest"), []);
});

test("latest/oldest use creation dates and leave the server array unchanged", () => {
  const products = [product(10, { createdAt: "2026-01-01T12:00:00" }), product(2, { createdAt: "2026-09-01T12:00:00" })];
  assert.deepEqual(ids(filterAndSortProducts(products, new Set(), 0, 100, "latest")), [2, 10]);
  assert.deepEqual(ids(filterAndSortProducts(products, new Set(), 0, 100, "oldest")), [10, 2]);
  assert.deepEqual(ids(products), [10, 2]);
  assert.deepEqual(ids(filterAndSortProducts([product(1), product(2)], new Set(), 0, 100, "latest")), [2, 1]);
});

test("reviews and both price sort directions change product order", () => {
  const products = [product(1, { reviews: 8, discountedPrice: 30 }), product(2, { reviews: 1, discountedPrice: 10 })];
  assert.deepEqual(ids(filterAndSortProducts(products, new Set(), 0, 100, "reviews")), [1, 2]);
  assert.deepEqual(ids(filterAndSortProducts(products, new Set(), 0, 100, "price-asc")), [2, 1]);
  assert.deepEqual(ids(filterAndSortProducts(products, new Set(), 0, 100, "price-desc")), [1, 2]);
});

test("pagination slices results, clamps after a smaller result set and handles empty results", () => {
  const products = Array.from({ length: PAGE_SIZE + 2 }, (_, index) => product(index + 1));
  assert.equal(paginateProducts(products, 1).products.length, PAGE_SIZE);
  assert.deepEqual(ids(paginateProducts(products, 2).products), [PAGE_SIZE + 1, PAGE_SIZE + 2]);
  assert.equal(paginateProducts(products.slice(0, 2), 8).page, 1);
  assert.deepEqual(paginateProducts([], 8), { page: 1, totalPages: 1, products: [] });
});
