// Stream current Git-managed source and nonignored new files to Gitleaks stdin.
// .env, installed packages and generated build output follow the repository's existing .gitignore.
const fs = require('node:fs');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const root = path.resolve(__dirname, '..');
const files = new Set(execFileSync('git', ['ls-files', '--cached', '--others', '--exclude-standard', '-z'], {
  cwd: root, encoding: 'utf8', maxBuffer: 32 * 1024 * 1024,
}).split('\0').filter(Boolean));
for (const file of files) {
  const absolute = path.join(root, file);
  if (!fs.existsSync(absolute) || !fs.lstatSync(absolute).isFile()) continue;
  process.stdout.write(`\nFILE: ${file}\n`);
  process.stdout.write(fs.readFileSync(absolute));
  process.stdout.write('\n');
}
