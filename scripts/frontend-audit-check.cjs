const fs = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');

const root = path.resolve(__dirname, '..');
const severities = ['info', 'low', 'moderate', 'high', 'critical'];

function canonical(value) {
  if (Array.isArray(value)) return value.map(canonical).sort((a, b) => JSON.stringify(a).localeCompare(JSON.stringify(b)));
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.keys(value).sort().map(key => [key, canonical(value[key])]));
  }
  return value;
}

function parseAuditResult(result) {
  if (result.error || result.signal || ![0, 1].includes(result.status)) {
    throw new Error('npm audit failed to run or complete');
  }
  let report;
  try { report = JSON.parse(result.stdout); } catch { throw new Error('npm audit returned invalid JSON'); }
  if (report.error || report.auditReportVersion !== 2 || !report.vulnerabilities
      || Array.isArray(report.vulnerabilities) || typeof report.vulnerabilities !== 'object'
      || !report.metadata?.vulnerabilities) throw new Error('npm audit returned an incomplete report');
  const counts = Object.fromEntries(severities.map(severity => [severity, 0]));
  for (const [name, finding] of Object.entries(report.vulnerabilities)) {
    if (finding.name !== name || !severities.includes(finding.severity)
        || typeof finding.isDirect !== 'boolean' || typeof finding.range !== 'string'
        || !Array.isArray(finding.via) || !finding.via.length
        || !Array.isArray(finding.nodes) || !finding.nodes.length) {
      throw new Error('npm audit returned an invalid finding');
    }
    counts[finding.severity]++;
  }
  const total = Object.values(counts).reduce((sum, count) => sum + count, 0);
  if (report.metadata.vulnerabilities.total !== total
      || severities.some(severity => report.metadata.vulnerabilities[severity] !== counts[severity])
      || result.status !== (counts.high + counts.critical > 0 ? 1 : 0)) {
    throw new Error('npm audit status/counts do not match its findings');
  }
  return report;
}

function fingerprint(finding, versions) {
  return canonical({
    name: finding.name, severity: finding.severity, isDirect: finding.isDirect, range: finding.range,
    via: finding.via.map(cause => typeof cause === 'string' ? cause : {
      source: cause.source, name: cause.name, dependency: cause.dependency,
      url: cause.url, severity: cause.severity, range: cause.range,
    }),
    nodes: finding.nodes.map(node => {
      if (typeof versions[node] !== 'string') throw new Error('Installed dependency version is missing');
      return { path: node, version: versions[node] };
    }),
  });
}

function evaluateAudit(report, policy, versions) {
  if (policy.schemaVersion !== 1 || policy.approvedBy !== 'user' || !policy.reason
      || !Array.isArray(policy.exceptions)) throw new Error('Frontend exception policy is invalid');
  const approved = new Set(policy.exceptions.map(exception => JSON.stringify(canonical(exception))));
  const accepted = [], blocked = [];
  for (const finding of Object.values(report.vulnerabilities)) {
    if (!['high', 'critical'].includes(finding.severity)) continue;
    const match = approved.has(JSON.stringify(fingerprint(finding, versions)));
    (match ? accepted : blocked).push(finding.name);
  }
  return { accepted, blocked };
}

function installedVersions(report) {
  const frontend = path.join(root, 'code', 'frontend');
  const modules = path.join(frontend, 'node_modules') + path.sep;
  const lock = JSON.parse(fs.readFileSync(path.join(frontend, 'package-lock.json'), 'utf8'));
  const versions = {};
  for (const finding of Object.values(report.vulnerabilities)) {
    for (const node of finding.nodes) {
      const directory = path.resolve(frontend, node);
      if (!directory.startsWith(modules)) throw new Error('Audit dependency path is invalid');
      const installed = JSON.parse(fs.readFileSync(path.join(directory, 'package.json'), 'utf8')).version;
      if (lock.packages?.[node]?.version !== installed) throw new Error('Installed dependency differs from lockfile');
      versions[node] = installed;
    }
  }
  return versions;
}

if (require.main === module) {
  try {
    const options = { cwd: path.join(root, 'code', 'frontend'), encoding: 'utf8', timeout: 180000, maxBuffer: 10 * 1024 * 1024 };
    const result = process.platform === 'win32'
      ? spawnSync('cmd.exe', ['/d', '/s', '/c', 'npm audit --json --audit-level=high'], options)
      : spawnSync('npm', ['audit', '--json', '--audit-level=high'], options);
    const report = parseAuditResult(result);
    const policy = JSON.parse(fs.readFileSync(path.join(__dirname, 'frontend-audit-exceptions.json'), 'utf8'));
    const { accepted, blocked } = evaluateAudit(report, policy, installedVersions(report));
    for (const name of accepted) console.log(`APPROVED DEMO EXCEPTION: ${name} (${report.vulnerabilities[name].severity}; vulnerability remains)`);
    for (const name of blocked) console.error(`FAIL: unapproved frontend vulnerability: ${name} (${report.vulnerabilities[name].severity})`);
    console.log(`Frontend audit: ${accepted.length} approved exceptions; ${blocked.length} unapproved high/critical findings`);
    process.exitCode = blocked.length ? 1 : 0;
  } catch (error) {
    console.error(`FAIL: frontend dependency audit: ${error.message}`);
    process.exitCode = 1;
  }
}

module.exports = { parseAuditResult, fingerprint, evaluateAudit };
