# PandaStore security gate

Run from the repository root with Git Bash or WSL:

```bash
bash scripts/security-check.sh
```

The gate returns 1 for findings, failed tests, missing tools, unavailable servers, incomplete Semgrep scans, or dependency scanner failures. Frontend tests are required; there is no `--if-present` fallback. Logs use an isolated temporary file. Secret findings are redacted. Current files are scanned with Git's existing ignore rules, including new nonignored files; the history scan remains enabled separately.

Requirements: Java 21, Node/npm, Git, Bash, curl, Gitleaks 8.30.1 or compatible, and Semgrep 1.179.0 or compatible. Install frontend dependencies with `npm ci`. Use `backend/mvnw` in Bash or `backend/mvnw.cmd` in PowerShell. Keep scanner commands on PATH. The Semgrep auto configuration requires network access and its default anonymous metrics; strict parsing is enabled.

Dependency Check is pinned to 13.0.0, SpotBugs Maven to 4.10.4.1, and Find Security Bugs to 1.14.0. Versions were checked against the [Dependency Check documentation](https://dependency-check.github.io/DependencyCheck/dependency-check-maven/check-mojo.html), [SpotBugs release](https://github.com/spotbugs/spotbugs-maven-plugin/releases/tag/spotbugs-maven-plugin-4.10.4.1), and [Find Security Bugs release](https://github.com/find-sec-bugs/find-sec-bugs/releases/tag/version-1.14.0).

## Start the application

Frontend:

```bash
cd frontend
npm run dev
```

Backend, in another terminal:

```bash
cd backend
./mvnw spring-boot:run
```

Configure DB_URL, DB_USERNAME, DB_PASSWORD and a securely generated JWT_SECRET using the existing backend environment setup. The PostgreSQL container shown during verification exposes port 32768 on the host; a local JDBC URL needs that host port, not the container's 5432. Keep the existing database name and credentials. Never enable demo seeding against a populated database.

Set ALLOWED_REQUEST_ORIGINS to exact frontend origins separated by commas, without trailing slashes. Backend and frontend read this setting. HttpOnly auth cookies use SameSite=Lax and Secure in production, so deploy production behind HTTPS. FE_URL and BE_URL override the default live header targets, http://localhost:3000 and http://localhost:8080.

Set NVD_API_KEY in the shell that runs the gate. Do not place it in Git, command examples, or NEXT_PUBLIC variables. Request a key from [NVD](https://nvd.nist.gov/developers/request-an-api-key). This is access to the public vulnerability database, separate from the application's PostgreSQL database. The first scan downloads the CVE database and can take considerable time. The plugin reads the key from the process environment; Spring's `.env` import does not export it to Maven.

Verification used the [official NIST JSON 2.0 feed supported by OWASP](https://dependency-check.github.io/DependencyCheck/data/mirrornvd.html), because individual API responses were slow. Metadata was checked as updated on 2026-10-05; the first complete scan took 20:27 minutes. To reuse that completed local cache from this workspace in PowerShell, without changing the gate or its thresholds:

```powershell
$env:MAVEN_ARGS='-DnvdDatafeedUrl=https://nvd.nist.gov/feeds/json/cve/2.0/nvdcve-2.0-{0}.json.gz -DdataDirectory=E:/JavaEclipe/pos/PandaStore/backend/target/security-gate/nvd-feed-cache'
& 'C:\Program Files\Git\bin\bash.exe' scripts/security-check.sh
```

Adjust the absolute cache path if the checkout moves. This configuration uses feeds instead of the API and needs no API key. The scanner still updates and checks vulnerabilities normally; nothing is skipped. Maven clean removes this target cache, so a later scan would need to initialize it again. Official feeds can lag; inspect freshness when changing the data source rather than treating successful downloading alone as a passing vulnerability check.

## Authentication and public routes

Spring Security authenticates every route by default. The backend accepts explicit Authorization: Bearer headers; it does not authenticate browser cookies or enable Basic/form login. JWT session verification still checks expiry, active user status, the session table and current password hash.

| Method | Public routes |
| --- | --- |
| POST | `/api/auth/login`, `/api/auth/register/customer`, `/api/auth/register/seller`, `/api/sellers/register` |
| GET | `/api/auth/token` (the controller validates the provided Bearer token) |
| GET | `/api/products`, `/api/products/*`, `/api/products/seller/*` |
| GET | `/api/categories`, `/api/categories/*/products` |
| GET | `/api/reviews/product/*` |
| GET | `/api/seller/shops/*`, `/api/seller/shops/user/*` (public shop profiles; bank accounts remain protected) |
| HEAD | `/api/products`, `/api/products/*`, `/api/categories`, `/api/reviews/product/*` |

Patterns with `*` match one path segment. CORS preflight is handled by Spring Security for configured origins.

Customer lists, administrative endpoints, payment callbacks, payment simulation, and refunds require ADMIN. Individual customer records require the account owner or ADMIN. Payment reads/initiation check order-group ownership. Shop changes and bank-account changes require the owning active seller; bank-account reads also permit ADMIN. Review replies require the owning active seller, with review ownership checked by the service.

Demo account seeding defaults to false. `/api/test/reset-data` exists only under the explicit `test-support` profile and still requires ADMIN. Reset SQL consists of fixed literal statements and no user-controlled identifiers.

Cookie-authenticated frontend API mutations require an exact allowed Origin. UploadThing callbacks reach its signature-verifying route handler; upload initialization still uses the existing account/role checks. Password rotation replaces the HttpOnly cookie and omits the token from its browser JSON response.

## Tests and limits

`npm test` runs all frontend Node tests. `backend/mvnw test` runs all backend tests, including SecurityGateTest through the real Spring Security filter chain. The tests cover anonymous and invalid-token denials, ADMIN restrictions, authenticated identity, private account access, Bean Validation, exact CORS origins, headers and 429 responses. LoginRateLimitFilterTest covers concurrent requests, window expiry and separate source addresses. Frontend regression tests cover origin rejection and production cookie/token handling.

Login allows five attempts per source address per 60-second fixed window, with Retry-After on the sixth request. Buckets are atomic and capped at 10,000; saturation denies new sources rather than dropping existing limits. Untrusted forwarded headers cannot reset a bucket. This implementation is per JVM. Requests proxied by Next.js share its backend source address; production needs a verified client-address strategy and a shared limiter for multiple instances. Restarts reset the in-memory limiter.

The frontend CSP permits inline scripts/styles for the existing Next.js application, and eval only in development for HMR. A nonce-based production CSP would provide stronger script isolation. Payment gateway callbacks currently require ADMIN until an actual gateway signature verifier is implemented.

Static pattern checks are supplementary heuristics. They do not prove complete injection/XSS coverage or complete authorization for every object. Dependency databases and Semgrep auto rules evolve; re-run the gate when dependencies or code change.

## Acceptance status

Implementation and functional tests are available, but the gate remains failed until every mandatory check passes. See SECURITY-REVIEW.md for actual results and the exact user-approved exceptions. Tailwind remains pinned to its original 3.3.3 at the user's instruction. The final frontend dependency tree retains all original major versions and reports seven high findings plus one critical Swiper finding. No threshold or gate script has been changed following exception approval, and no test has been skipped. The only exceptions are the approved SpotBugs XML patterns/class and the known historical dummy keys in two exact test paths.

Other frontend dependencies were updated within their original major or removed when unused. Next.js runtime is 16.3.8, Swiper is 10.3.1, PostCSS is 8.5.29, ESLint is 9.39.5, and eslint-config-next is 15.5.27. Temporary Swiper 14 and lint-tool major changes were reverted after reading the additional AGENTS.md major-version constraint; the cross-major Glob override was removed. Unused next-auth, next-sanity, nodemailer, sanity and shadcn packages were removed after checking source references. UploadThing uses a patched Effect 3 release through an override. Tailwind, its CSS integration and its application classes are retained as before. The unresolved dependency findings cannot be treated as accepted exceptions. The registry flags ESLint 9.39.5 as no longer supported, so its major-version constraint remains a maintenance limitation.
