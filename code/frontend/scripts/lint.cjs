const { spawnSync } = require("node:child_process");
const { dirname, join } = require("node:path");

const eslint = join(dirname(require.resolve("eslint/package.json")), "bin", "eslint.js");
const result = spawnSync(process.execPath, [eslint, "src", ...process.argv.slice(2)], {
  stdio: "inherit",
  env: { ...process.env, ESLINT_USE_FLAT_CONFIG: "false" },
});

if (result.error) throw result.error;
process.exit(result.status ?? 1);
