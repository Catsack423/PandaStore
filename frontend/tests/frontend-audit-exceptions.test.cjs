const { test } = require('node:test');
const assert = require('node:assert/strict');
const { parseAuditResult, evaluateAudit } = require('../../scripts/frontend-audit-check.cjs');
const policy = require('../../scripts/frontend-audit-exceptions.json');

function swiperFinding() {
  return {
    name: 'swiper', severity: 'critical', isDirect: true, range: '6.5.1 - 12.1.1',
    nodes: ['node_modules/swiper'],
    via: [{ source: 1113431, name: 'swiper', dependency: 'swiper',
      url: 'https://github.com/advisories/GHSA-hmx5-qpq5-p643', severity: 'critical', range: '>=6.5.1 <12.1.2' }],
  };
}

function auditResult(findings = [swiperFinding()]) {
  const counts = { info: 0, low: 0, moderate: 0, high: 0, critical: 0, total: findings.length };
  for (const finding of findings) counts[finding.severity]++;
  return {
    status: counts.high + counts.critical ? 1 : 0,
    stdout: JSON.stringify({ auditReportVersion: 2,
      vulnerabilities: Object.fromEntries(findings.map(finding => [finding.name, finding])),
      metadata: { vulnerabilities: counts } }),
  };
}

test('only the explicitly approved Swiper advisory at its current installed version is accepted', () => {
  assert.deepEqual(evaluateAudit(parseAuditResult(auditResult()), policy, { 'node_modules/swiper': '10.3.1' }),
    { accepted: ['swiper'], blocked: [] });
});

test('new advisories, changed severity/range/version and new vulnerable packages still fail the gate', () => {
  const additionalAdvisory = swiperFinding();
  additionalAdvisory.via.push({ ...additionalAdvisory.via[0], source: 9999999, url: 'https://github.com/advisories/GHSA-new-advisory' });
  const changedRange = swiperFinding();
  changedRange.via[0].range = '>=6.5.1 <13.0.0';
  const changedSeverity = swiperFinding();
  changedSeverity.severity = 'high';
  for (const finding of [additionalAdvisory, changedRange, changedSeverity]) {
    assert.deepEqual(evaluateAudit(parseAuditResult(auditResult([finding])), policy, { 'node_modules/swiper': '10.3.1' }),
      { accepted: [], blocked: ['swiper'] });
  }
  assert.deepEqual(evaluateAudit(parseAuditResult(auditResult()), policy, { 'node_modules/swiper': '10.3.2' }),
    { accepted: [], blocked: ['swiper'] });
  const newPackage = { ...swiperFinding(), name: 'new-package', nodes: ['node_modules/new-package'] };
  assert.deepEqual(evaluateAudit(parseAuditResult(auditResult([swiperFinding(), newPackage])), policy,
    { 'node_modules/swiper': '10.3.1', 'node_modules/new-package': '1.0.0' }),
    { accepted: ['swiper'], blocked: ['new-package'] });
});

test('scanner errors, incomplete JSON and inconsistent exit codes/counts cannot become an audit pass', () => {
  for (const result of [
    { status: 2, stdout: '{}' }, { status: null, signal: 'SIGTERM', stdout: '{}' },
    { status: 0, error: new Error('npm is missing'), stdout: '{}' },
    { status: 1, stdout: 'network failure' }, { status: 1, stdout: JSON.stringify({ error: { code: 'ENOTFOUND' } }) },
    { status: 0, stdout: '{}' }, { ...auditResult(), status: 0 },
  ]) assert.throws(() => parseAuditResult(result));
  const incomplete = JSON.parse(auditResult().stdout);
  incomplete.metadata.vulnerabilities.total = 0;
  assert.throws(() => parseAuditResult({ status: 1, stdout: JSON.stringify(incomplete) }));
  assert.throws(() => evaluateAudit(parseAuditResult(auditResult()), policy, {}));
});

test('a clean audit and findings below the original high threshold remain allowed', () => {
  assert.deepEqual(evaluateAudit(parseAuditResult(auditResult([])), policy, {}), { accepted: [], blocked: [] });
  const moderate = { ...swiperFinding(), name: 'moderate-package', severity: 'moderate', nodes: ['node_modules/moderate-package'] };
  assert.deepEqual(evaluateAudit(parseAuditResult(auditResult([moderate])), policy, {}), { accepted: [], blocked: [] });
});
