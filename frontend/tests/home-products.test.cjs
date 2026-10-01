const { test } = require("node:test");
const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const { dirname, resolve } = require("node:path");
const ts = require("typescript");
const React = require("react");
const { renderToStaticMarkup } = require("react-dom/server");

const root = resolve(__dirname, "../src");
const cache = new Map();
let searchResult = { items: [] };
let searchFailure = false;
let searchCalls = [];
let refreshes = 0;
let pending = false;
let auth = { user: null, isLoading: false };
const ProductCard = () => null;

function load(path) {
  const file = path.endsWith(".tsx") || path.endsWith(".ts") ? path : `${path}.tsx`;
  if (cache.has(file)) return cache.get(file);
  const compiled = ts.transpileModule(readFileSync(file, "utf8"), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, jsx: ts.JsxEmit.ReactJSX,
      target: ts.ScriptTarget.ES2020, esModuleInterop: true },
  }).outputText;
  const exports = {};
  const mocks = {
    react: { ...React, useTransition: () => [pending, callback => callback()] },
    "next/navigation": { useRouter: () => ({ refresh: () => { refreshes += 1; } }) },
    "next/link": { default: ({ children, ...props }) => React.createElement("a", props, children), __esModule: true },
    "@/app/context/AuthContext": { useAuth: () => auth },
    "@/components/Shop/SingleGridItem": { default: ProductCard, __esModule: true },
    "@/components/ui/skeleton": { Skeleton: props => React.createElement("div", { "data-slot": "skeleton", ...props }) },
    "@/ServerAction/products": { searchProducts: async params => {
      searchCalls.push(params);
      if (searchFailure) throw new Error("API unavailable");
      return searchResult;
    } },
  };
  new Function("require", "exports", compiled)(name => {
    if (name in mocks) return mocks[name];
    if (name.endsWith("constants")) return load(resolve(root, "components/Home/constants.ts"));
    if (name === "@/components/Home") return load(resolve(root, "components/Home/index.tsx"));
    if (name === "@/components/Home/NewArrivals") return load(resolve(root, "components/Home/NewArrivals/index.tsx"));
    if (name.startsWith("@/")) return load(resolve(root, name.slice(2)));
    if (name.startsWith(".")) return load(resolve(dirname(file), name));
    return require(name);
  }, exports);
  cache.set(file, exports);
  return exports;
}

const HomePage = load(resolve(root, "app/(site)/page.tsx")).default;
const Home = load(resolve(root, "components/Home/index.tsx")).default;
const NewArrival = load(resolve(root, "components/Home/NewArrivals/index.tsx")).default;
const Loading = load(resolve(root, "components/Home/NewArrivals/Skeleton.tsx")).default;
const products = count => Array.from({ length: count }, (_, index) => ({ id: 100 - index, title: `Product ${100 - index}` }));

test("Home requests the latest 16 products inside Suspense and passes API data directly", async () => {
  searchCalls = [];
  searchResult = { items: products(10) };
  searchFailure = false;
  const boundary = HomePage().props.children;
  assert.equal(boundary.type, React.Suspense);
  assert.equal(boundary.props.fallback.type, Loading);
  const result = await boundary.props.children.type();
  assert.deepEqual(searchCalls, [{ sort: "latest", page: 0, size: 16 }]);
  assert.equal(result.type, NewArrival);
  assert.equal(result.props.products, searchResult.items);
});

test("API failure shows an error without substituting sample products", async () => {
  searchFailure = true;
  const result = await HomePage().props.children.props.children.type();
  assert.deepEqual(result.props.products, []);
  assert.equal(result.props.loadError, true);
  searchFailure = false;
});

test("the grid caps results at 16, preserves API order and uses stable product keys", () => {
  const cards = NewArrival({ products: products(23) }).props.children;
  assert.equal(cards.length, 16);
  assert.deepEqual(cards.map(card => card.props.item.id), products(16).map(product => product.id));
  for (const card of cards) {
    assert.equal(card.key, String(card.props.item.id));
    assert.equal(card.props.showImageSkeleton, true);
  }
});

test("fewer than 16 products render without padding with sample cards", () => {
  assert.equal(NewArrival({ products: products(3) }).props.children.length, 3);
});

test("empty results and API errors have distinct messages and only errors offer Retry", () => {
  const empty = renderToStaticMarkup(NewArrival({ products: [] }));
  const error = NewArrival({ products: [], loadError: true });
  const errorHtml = renderToStaticMarkup(error);
  assert.match(empty, /No products available yet/);
  assert.doesNotMatch(empty, /Retry/);
  assert.match(errorHtml, /Could not load products/);
  const retry = error.props.children.find(child => child.type === "button");
  refreshes = 0;
  retry.props.onClick();
  assert.equal(refreshes, 1);
});

test("initial loading and Retry loading use 16 square shadcn skeleton cards", () => {
  const loading = Loading();
  const cards = loading.props.children.props.children;
  assert.equal(String(loading.props["aria-busy"]), "true");
  assert.equal(cards.length, 16);
  assert.match(cards[0].props.children[0].props.className, /aspect-square.*motion-reduce:animate-none/);
  pending = true;
  assert.equal(NewArrival({ products: [], loadError: true }).type, Loading);
  pending = false;
});

test("Home keeps View All, removes the category carousel and uses the same responsive grid during loading", () => {
  const html = renderToStaticMarkup(Home({ children: null }));
  assert.match(html, /New Arrivals/);
  assert.match(html, /href="\/shop-with-sidebar"/);
  assert.doesNotMatch(html, /Categories|This Week|Browse by Category/);
  assert.equal(NewArrival({ products: products(1) }).props.className, Loading().props.children.props.className);
});

test("customer and guest cards remain shoppable; seller and loading sessions are read-only", () => {
  for (const [user, isLoading, readOnly] of [
    [null, false, false], [{ role: "CUSTOMER" }, false, false],
    [{ role: "SELLER" }, false, true], [null, true, true],
  ]) {
    auth = { user, isLoading };
    assert.equal(NewArrival({ products: products(1) }).props.children[0].props.readOnly, readOnly);
  }
  auth = { user: null, isLoading: false };
});
