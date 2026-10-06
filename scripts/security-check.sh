#!/usr/bin/env bash
set -uo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)" || exit 1
cd -- "$ROOT" || exit 1
FAIL=0
FE_URL="${FE_URL:-http://localhost:3000}"
BE_URL="${BE_URL:-http://localhost:8080}"
OUTPUT="$(mktemp)" || exit 1
trap 'rm -f -- "$OUTPUT"' EXIT

check() {
  local name="$1"
  shift
  if "$@" >"$OUTPUT" 2>&1; then
    printf 'PASS: %s\n' "$name"
  else
    printf 'FAIL: %s\n' "$name"
    tail -n 20 "$OUTPUT"
    FAIL=1
  fi
}

fe_audit() { node scripts/frontend-audit-check.cjs; }
fe_tests() { (cd frontend && npm test); }
be_dependency() { (cd backend && bash ./mvnw -B -ntp dependency-check:check); }
be_sast() { (cd backend && bash ./mvnw -B -ntp compile spotbugs:check); }
be_tests() { (cd backend && bash ./mvnw -B -ntp test); }
aco_dependency() { mvn -B -ntp -f aco-testgen/pom.xml dependency-check:check; }
aco_sast() { mvn -B -ntp -f aco-testgen/pom.xml compile spotbugs:check; }
aco_tests() { mvn -B -ntp -f aco-testgen/pom.xml test; }
working_tree_secrets() { node scripts/security-files.cjs | gitleaks stdin --no-banner --redact=100; }

check "FE dependencies: no unapproved high/critical (explicit demo exceptions)" fe_audit
check "FE tests (required)" fe_tests
check "FE dangerous APIs" node scripts/security-static-check.cjs fe-dangerous
check "FE NEXT_PUBLIC has no secrets" node scripts/security-static-check.cjs fe-public-secrets
check "FE auth tokens stay out of browser storage" node scripts/security-static-check.cjs fe-token-storage
check "BE dependencies: CVSS < 7" be_dependency
check "BE SpotBugs + Find Security Bugs" be_sast
# SecurityGateTest exercises authentication, authorization, rate limiting, headers and CORS.
check "BE tests including SecurityGateTest" be_tests
check "ACO dependencies: CVSS < 7" aco_dependency
check "ACO SpotBugs + Find Security Bugs" aco_sast
check "ACO generator tests (required)" aco_tests
check "BE parameterized SQL" node scripts/security-static-check.cjs be-sql
check "BE BCrypt/Argon2 password hashing" node scripts/security-static-check.cjs be-password
check "BE CORS has no wildcard" node scripts/security-static-check.cjs be-cors
check "BE actuator is restricted" node scripts/security-static-check.cjs be-actuator
check "BE every RequestBody has Valid" node scripts/security-static-check.cjs be-validation
check "gitleaks repository history" gitleaks detect --no-banner --redact=100
check "gitleaks current tracked and nonignored files" working_tree_secrets
check "Semgrep FE + BE + ACO" semgrep --config auto --error --strict --quiet frontend/src backend/src aco-testgen/src
check "env ignored and config uses environment secrets" node scripts/security-static-check.cjs env-config

for url in "$FE_URL" "$BE_URL"; do
  if ! curl --silent --show-error --connect-timeout 5 --max-time 15 --head "$url" >"$OUTPUT" 2>&1; then
    printf 'FAIL: server unavailable: %s\n' "$url"
    FAIL=1
    continue
  fi
  for header in content-security-policy x-content-type-options x-frame-options referrer-policy; do
    if grep -qiE "^${header}:[[:space:]]*[^[:space:]]" "$OUTPUT"; then
      printf 'PASS: %s %s\n' "$url" "$header"
    else
      printf 'FAIL: %s missing %s\n' "$url" "$header"
      FAIL=1
    fi
  done
done
exit "$FAIL"
