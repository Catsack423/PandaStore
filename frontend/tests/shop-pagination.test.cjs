const { test } = require("node:test");
const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const { resolve } = require("node:path");
const ts = require("typescript");

const source = readFileSync(
  resolve(__dirname, "../src/components/ShopWithSidebar/Pagination.tsx"),
  "utf8",
);

const compiled = ts.transpileModule(source, {
  compilerOptions: {
    module: ts.ModuleKind.CommonJS,
    jsx: ts.JsxEmit.ReactJSX,
    target: ts.ScriptTarget.ES2020,
  },
}).outputText;
const moduleExports = {};
new Function("require", "exports", compiled)(
  (name) =>
    name === "lucide-react"
      ? { ChevronLeft: () => null, ChevronRight: () => null }
      : require(name),
  moduleExports,
);
const Pagination = moduleExports.default;

function buttons(element) {
  if (!element || typeof element !== "object") return [];
  if (Array.isArray(element)) return element.flatMap(buttons);
  const found = element.type === "button" ? [element] : [];
  return found.concat(buttons(element.props?.children));
}

test("empty results have no pager; one page shows page 1 with both arrows disabled", () => {
  assert.equal(
    Pagination({ page: 1, totalPages: 0, totalItems: 0, onPageChange() {} }),
    null,
  );
  const controls = buttons(
    Pagination({ page: 1, totalPages: 1, totalItems: 3, onPageChange() {} }),
  );
  assert.deepEqual(
    controls.map((button) => button.props["aria-label"]),
    ["Previous page", "Page 1", "Next page"],
  );
  assert.equal(controls[0].props.disabled, true);
  assert.equal(controls[2].props.disabled, true);
});

test("clicking the current page does not request the same page again", () => {
  const selected = [];
  const controls = buttons(
    Pagination({ page: 1, totalPages: 1, totalItems: 3, onPageChange: (page) => selected.push(page) }),
  );
  controls[1].props.onClick();
  assert.deepEqual(selected, []);
});

test("number and arrow controls request the selected backend page", () => {
  const selected = [];
  const controls = buttons(
    Pagination({
      page: 2,
      totalPages: 4,
      totalItems: 36,
      onPageChange: (page) => selected.push(page),
    }),
  );
  controls
    .find((button) => button.props["aria-label"] === "Page 4")
    .props.onClick();
  controls
    .find((button) => button.props["aria-label"] === "Previous page")
    .props.onClick();
  controls
    .find((button) => button.props["aria-label"] === "Next page")
    .props.onClick();
  assert.deepEqual(selected, [4, 1, 3]);
});
