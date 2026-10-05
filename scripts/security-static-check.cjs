const fs = require('node:fs');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const root = path.resolve(__dirname, '..');
const mode = process.argv[2];
let failures = 0;

function fail(file, message) {
  // Never print matching source: it might contain a credential.
  console.error(`${path.relative(root, file)}: ${message}`);
  failures++;
}
function files(directory, accept) {
  const result = [];
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    if (['node_modules', '.next', '.git', 'target', 'coverage'].includes(entry.name)) continue;
    const name = path.join(directory, entry.name);
    if (entry.isDirectory()) result.push(...files(name, accept));
    else if (entry.isFile() && accept(name)) result.push(name);
  }
  return result;
}
function scan(directory, accept, inspect) {
  const input = files(path.join(root, directory), accept);
  if (!input.length) throw new Error(`No source files found in ${directory}`);
  for (const file of input) inspect(file, fs.readFileSync(file, 'utf8'));
}
const java = name => name.endsWith('.java');
const frontend = name => /\.(?:[cm]?[jt]sx?|json)$/.test(name) || /^\.env/.test(path.basename(name));
const config = name => /\.(properties|ya?ml)$/.test(name);
const reject = (expression, message) => (file, source) => {
  expression.lastIndex = 0;
  if (expression.test(source)) fail(file, message);
};

try {
  switch (mode) {
    case 'fe-dangerous':
      scan('frontend/src', frontend, reject(/\beval\s*\(|dangerouslySetInnerHTML|\binnerHTML\b|document\s*\.\s*write\s*\(/,
        'Dangerous HTML/script API'));
      break;
    case 'fe-public-secrets':
      scan('frontend', frontend, reject(/NEXT_PUBLIC_[A-Z0-9_]*(?:SECRET|KEY|PASSWORD|PRIVATE|TOKEN)/i,
        'Sensitive NEXT_PUBLIC variable name'));
      break;
    case 'fe-token-storage':
      scan('frontend/src', frontend, reject(/(?:localStorage|sessionStorage)\s*\.\s*setItem\s*\([\s\S]{0,150}?(?:auth_token|accessToken|refreshToken|["']token["'])/i,
        'Authentication token stored in browser storage'));
      break;
    case 'be-sql':
      scan('backend/src/main', java, reject(/(?:createNativeQuery|createQuery|prepareStatement)\s*\([^;]*\+|\bStatement\s+\w+\s*=/,
        'Possible SQL string concatenation or unparameterized Statement'));
      break;
    case 'be-password': {
      let strong = false;
      scan('backend/src/main', java, (file, source) => {
        reject(/NoOpPasswordEncoder|MessageDigest\s*\.\s*getInstance\s*\(\s*["'](?:MD5|SHA-?1)["']/i,
          'Weak password encoder/hash')(file, source);
        strong ||= /BCryptPasswordEncoder|Argon2PasswordEncoder/.test(source);
      });
      if (!strong) fail(root, 'BCrypt/Argon2 encoder is required');
      break;
    }
    case 'be-cors':
      scan('backend/src/main', name => java(name) || config(name), reject(
        /(?:allowedOrigins|allowedOriginPatterns|setAllowedOrigins|setAllowedOriginPatterns)\s*\([^;]*["']\*["']|@CrossOrigin\s*\([^)]*\*|allowed[-.]?origins?\s*[:=]\s*["']?\*/i,
        'Wildcard CORS origin'));
      break;
    case 'be-actuator':
      scan('backend/src/main/resources', config, reject(/management\.endpoints\.web\.exposure\.include\s*=\s*[^\r\n]*\*|\binclude\s*:\s*[^\r\n]*\*/,
        'Actuator wildcard exposure'));
      break;
    case 'be-validation':
      scan('backend/src/main', java, (file, source) => {
        const code = source.replace(/\/\*[\s\S]*?\*\/|\/\/[^\r\n]*/g, '');
        for (const match of code.matchAll(/@RequestBody\b/g)) {
          // Include annotations on either side, allowing @Valid @RequestBody and the reverse.
          const before = code.slice(0, match.index).split(/[{},;]/).pop();
          const after = code.slice(match.index).split(/[{},;]/)[0];
          if (!/@(?:jakarta\.validation\.)?Valid\b/.test(before + after))
            fail(file, `RequestBody without Valid (line ${source.slice(0, source.indexOf('@RequestBody')).split('\n').length})`);
        }
      });
      break;
    case 'env-config': {
      for (const directory of ['', 'frontend', 'backend']) {
        for (const name of ['.env', '.env.local', '.env.production', '.env.security-gate']) {
          const relative = path.posix.join(directory, name);
          try { execFileSync('git', ['check-ignore', '--no-index', '-q', relative], { cwd: root, stdio: 'pipe' }); }
          catch { fail(path.join(root, relative), 'Environment file is not ignored'); }
        }
      }
      const tracked = execFileSync('git', ['ls-files', '-z'], { cwd: root, encoding: 'utf8' }).split('\0');
      for (const name of tracked) {
        if (/^\.env(?:\.|$)/.test(path.basename(name)) && !name.endsWith('.example'))
          fail(path.join(root, name), 'Real environment file is tracked');
      }
      scan('backend/src/main/resources', config, (file, source) => {
        for (const line of source.split(/\r?\n/)) {
          if (/^\s*[#!]/.test(line)) continue;
          const match = line.match(/^\s*(?:spring\.datasource\.password|[\w.-]*(?:password|secret|api[-_.]?key))\s*[:=]\s*(.+)$/i);
          if (match && !/^\$\{[A-Z0-9_]+\}$/.test(match[1].trim()))
            fail(file, 'Secret config must reference an environment variable without a default');
        }
      });
      break;
    }
    default: throw new Error(`Unknown static check: ${mode}`);
  }
} catch (error) {
  console.error(`Security check could not run: ${error.message}`);
  failures++;
}
process.exitCode = failures ? 1 : 0;
