# Security verification and approved exceptions

Updated 2026-10-06. The gate is not accepted: frontend audit fails while retaining Tailwind 3.3.3 and all original dependency majors, and backend Dependency Check still reports the unsuppressed DevTools/Spring Tools match described below. NVD initialization and scanning completed. Thresholds and gate scripts have not been changed following exception approval.

The original strict SpotBugs report contained 162 findings: 103 EI_EXPOSE_REP2, 58 EI_EXPOSE_REP, and one SPRING_CSRF_PROTECTION_DISABLED. With the user's explicit approved filter, SpotBugs + Find Security Bugs now reports zero findings. Only EI_EXPOSE_REP/EI_EXPOSE_REP2 and the CSRF finding in the exact class project.project.Config.SecurityConfig are excluded, in backend/spotbugs-exclude.xml with explanations. No @SuppressFBWarnings annotations were added. Every other SpotBugs/Find Security Bugs rule remains enabled.

JwtAuthenticationFilter reads only the Authorization header (getHeaders(HttpHeaders.AUTHORIZATION)); it does not read cookies. A real filter-chain test verifies that a valid JWT presented in cookies alone still receives 401. Next.js checks Origin on cookie-authenticated mutations. The representation warnings include mutable DTO/entity accessors as well as Spring constructor injection; this is an explicitly accepted exception, not a claim that every mutable-reference warning is harmless. The table below preserves the original findings for review.

| Rule | Class | Source line |
| --- | --- | --- |
| SPRING_CSRF_PROTECTION_DISABLED | `project.project.Config.SecurityConfig` | 31 |
| EI_EXPOSE_REP2 | `project.project.Controller.AddressController` | 31 |
| EI_EXPOSE_REP2 | `project.project.Controller.CartController` | 22 |
| EI_EXPOSE_REP2 | `project.project.Controller.CategoryController` | 32 |
| EI_EXPOSE_REP2 | `project.project.Controller.CustomerCheckoutController` | 19 |
| EI_EXPOSE_REP2 | `project.project.Controller.CustomerController` | 24 |
| EI_EXPOSE_REP2 | `project.project.Controller.SellerShopController` | 23 |
| EI_EXPOSE_REP | `project.project.DTO.cart.CartDtos$CartResponse` | 25 |
| EI_EXPOSE_REP2 | `project.project.DTO.cart.CartDtos$CartResponse` | 25 |
| EI_EXPOSE_REP | `project.project.DTO.cart.CartDtos$Item` | 16 |
| EI_EXPOSE_REP2 | `project.project.DTO.cart.CartDtos$Item` | 16 |
| EI_EXPOSE_REP | `project.project.DTO.cart.CartDtos$StockSyncResponse` | 30 |
| EI_EXPOSE_REP | `project.project.DTO.cart.CartDtos$StockSyncResponse` | 30 |
| EI_EXPOSE_REP2 | `project.project.DTO.cart.CartDtos$StockSyncResponse` | 30 |
| EI_EXPOSE_REP2 | `project.project.DTO.cart.CartDtos$StockSyncResponse` | 30 |
| EI_EXPOSE_REP | `project.project.DTO.order.CreateOrderGroupRequest` | 11 |
| EI_EXPOSE_REP2 | `project.project.DTO.order.CreateOrderGroupRequest` | 11 |
| EI_EXPOSE_REP | `project.project.DTO.order.OrderGroupResponse` | 13 |
| EI_EXPOSE_REP2 | `project.project.DTO.order.OrderGroupResponse` | 13 |
| EI_EXPOSE_REP | `project.project.DTO.order.OrderGroupResponse$SubOrderResponse` | 38 |
| EI_EXPOSE_REP2 | `project.project.DTO.order.OrderGroupResponse$SubOrderResponse` | 38 |
| EI_EXPOSE_REP | `project.project.DTO.order.SubOrderResponse` | 239 |
| EI_EXPOSE_REP | `project.project.DTO.order.SubOrderResponse` | 247 |
| EI_EXPOSE_REP2 | `project.project.DTO.order.SubOrderResponse` | 75 |
| EI_EXPOSE_REP2 | `project.project.DTO.order.SubOrderResponse` | 243 |
| EI_EXPOSE_REP2 | `project.project.DTO.order.SubOrderResponse` | 251 |
| EI_EXPOSE_REP | `project.project.DTO.product.CatalogSummary` | 6 |
| EI_EXPOSE_REP2 | `project.project.DTO.product.CatalogSummary` | 6 |
| EI_EXPOSE_REP | `project.project.DTO.product.CreateProductRequest` | 102 |
| EI_EXPOSE_REP | `project.project.DTO.product.CreateProductRequest` | 110 |
| EI_EXPOSE_REP2 | `project.project.DTO.product.CreateProductRequest` | 57 |
| EI_EXPOSE_REP2 | `project.project.DTO.product.CreateProductRequest` | 106 |
| EI_EXPOSE_REP | `project.project.DTO.product.PageResponse` | 6 |
| EI_EXPOSE_REP2 | `project.project.DTO.product.PageResponse` | 6 |
| EI_EXPOSE_REP | `project.project.DTO.product.ProductResponse` | 194 |
| EI_EXPOSE_REP | `project.project.DTO.product.ProductResponse` | 198 |
| EI_EXPOSE_REP | `project.project.DTO.product.ProductResponse` | 186 |
| EI_EXPOSE_REP2 | `project.project.DTO.product.ProductResponse` | 202 |
| EI_EXPOSE_REP2 | `project.project.DTO.product.ProductResponse` | 190 |
| EI_EXPOSE_REP | `project.project.DTO.product.UpdateProductRequest` | 98 |
| EI_EXPOSE_REP2 | `project.project.DTO.product.UpdateProductRequest` | 46 |
| EI_EXPOSE_REP2 | `project.project.DTO.product.UpdateProductRequest` | 102 |
| EI_EXPOSE_REP | `project.project.Entity.notification.Notification` | 64 |
| EI_EXPOSE_REP2 | `project.project.Entity.notification.Notification` | 41 |
| EI_EXPOSE_REP2 | `project.project.Entity.notification.Notification` | 68 |
| EI_EXPOSE_REP | `project.project.Entity.order.Cart` | 41 |
| EI_EXPOSE_REP | `project.project.Entity.order.Cart` | 49 |
| EI_EXPOSE_REP2 | `project.project.Entity.order.Cart` | 29 |
| EI_EXPOSE_REP2 | `project.project.Entity.order.Cart` | 45 |
| EI_EXPOSE_REP2 | `project.project.Entity.order.Cart` | 53 |
| EI_EXPOSE_REP | `project.project.Entity.order.CartItem` | 49 |
| EI_EXPOSE_REP | `project.project.Entity.order.CartItem` | 57 |
| EI_EXPOSE_REP2 | `project.project.Entity.order.CartItem` | 34 |
| EI_EXPOSE_REP2 | `project.project.Entity.order.CartItem` | 35 |
| EI_EXPOSE_REP2 | `project.project.Entity.order.CartItem` | 53 |
| EI_EXPOSE_REP2 | `project.project.Entity.order.CartItem` | 61 |
| EI_EXPOSE_REP | `project.project.Entity.order.Order` | 114 |
| EI_EXPOSE_REP | `project.project.Entity.order.Order` | 202 |
| EI_EXPOSE_REP | `project.project.Entity.order.Order` | 122 |
| EI_EXPOSE_REP | `project.project.Entity.order.Order` | 210 |
| EI_EXPOSE_REP2 | `project.project.Entity.order.Order` | 77 |
| EI_EXPOSE_REP2 | `project.project.Entity.order.Order` | 78 |
| EI_EXPOSE_REP2 | `project.project.Entity.order.Order` | 118 |
| EI_EXPOSE_REP2 | `project.project.Entity.order.Order` | 206 |
| EI_EXPOSE_REP2 | `project.project.Entity.order.Order` | 126 |
| EI_EXPOSE_REP2 | `project.project.Entity.order.Order` | 214 |
| EI_EXPOSE_REP | `project.project.Entity.order.OrderGroup` | 96 |
| EI_EXPOSE_REP | `project.project.Entity.order.OrderGroup` | 168 |
| EI_EXPOSE_REP | `project.project.Entity.order.OrderGroup` | 104 |
| EI_EXPOSE_REP | `project.project.Entity.order.OrderGroup` | 160 |
| EI_EXPOSE_REP2 | `project.project.Entity.order.OrderGroup` | 62 |
| EI_EXPOSE_REP2 | `project.project.Entity.order.OrderGroup` | 63 |
| EI_EXPOSE_REP2 | `project.project.Entity.order.OrderGroup` | 100 |
| EI_EXPOSE_REP2 | `project.project.Entity.order.OrderGroup` | 172 |
| EI_EXPOSE_REP2 | `project.project.Entity.order.OrderGroup` | 108 |
| EI_EXPOSE_REP2 | `project.project.Entity.order.OrderGroup` | 164 |
| EI_EXPOSE_REP | `project.project.Entity.order.OrderItem` | 67 |
| EI_EXPOSE_REP | `project.project.Entity.order.OrderItem` | 75 |
| EI_EXPOSE_REP2 | `project.project.Entity.order.OrderItem` | 44 |
| EI_EXPOSE_REP2 | `project.project.Entity.order.OrderItem` | 45 |
| EI_EXPOSE_REP2 | `project.project.Entity.order.OrderItem` | 71 |
| EI_EXPOSE_REP2 | `project.project.Entity.order.OrderItem` | 79 |
| EI_EXPOSE_REP | `project.project.Entity.order.Payment` | 69 |
| EI_EXPOSE_REP2 | `project.project.Entity.order.Payment` | 45 |
| EI_EXPOSE_REP2 | `project.project.Entity.order.Payment` | 73 |
| EI_EXPOSE_REP | `project.project.Entity.order.Shipment` | 62 |
| EI_EXPOSE_REP2 | `project.project.Entity.order.Shipment` | 40 |
| EI_EXPOSE_REP2 | `project.project.Entity.order.Shipment` | 66 |
| EI_EXPOSE_REP | `project.project.Entity.product.Category` | 60 |
| EI_EXPOSE_REP2 | `project.project.Entity.product.Category` | 64 |
| EI_EXPOSE_REP | `project.project.Entity.product.Product` | 122 |
| EI_EXPOSE_REP | `project.project.Entity.product.Product` | 220 |
| EI_EXPOSE_REP | `project.project.Entity.product.Product` | 114 |
| EI_EXPOSE_REP2 | `project.project.Entity.product.Product` | 77 |
| EI_EXPOSE_REP2 | `project.project.Entity.product.Product` | 126 |
| EI_EXPOSE_REP2 | `project.project.Entity.product.Product` | 224 |
| EI_EXPOSE_REP2 | `project.project.Entity.product.Product` | 118 |
| EI_EXPOSE_REP | `project.project.Entity.product.ProductImage` | 47 |
| EI_EXPOSE_REP2 | `project.project.Entity.product.ProductImage` | 32 |
| EI_EXPOSE_REP2 | `project.project.Entity.product.ProductImage` | 51 |
| EI_EXPOSE_REP | `project.project.Entity.review.Review` | 99 |
| EI_EXPOSE_REP | `project.project.Entity.review.Review` | 83 |
| EI_EXPOSE_REP | `project.project.Entity.review.Review` | 91 |
| EI_EXPOSE_REP | `project.project.Entity.review.Review` | 147 |
| EI_EXPOSE_REP2 | `project.project.Entity.review.Review` | 55 |
| EI_EXPOSE_REP2 | `project.project.Entity.review.Review` | 53 |
| EI_EXPOSE_REP2 | `project.project.Entity.review.Review` | 54 |
| EI_EXPOSE_REP2 | `project.project.Entity.review.Review` | 103 |
| EI_EXPOSE_REP2 | `project.project.Entity.review.Review` | 87 |
| EI_EXPOSE_REP2 | `project.project.Entity.review.Review` | 95 |
| EI_EXPOSE_REP2 | `project.project.Entity.review.Review` | 151 |
| EI_EXPOSE_REP | `project.project.Entity.review.ReviewReply` | 55 |
| EI_EXPOSE_REP | `project.project.Entity.review.ReviewReply` | 63 |
| EI_EXPOSE_REP2 | `project.project.Entity.review.ReviewReply` | 35 |
| EI_EXPOSE_REP2 | `project.project.Entity.review.ReviewReply` | 36 |
| EI_EXPOSE_REP2 | `project.project.Entity.review.ReviewReply` | 59 |
| EI_EXPOSE_REP2 | `project.project.Entity.review.ReviewReply` | 67 |
| EI_EXPOSE_REP | `project.project.Entity.seller.Seller` | 79 |
| EI_EXPOSE_REP2 | `project.project.Entity.seller.Seller` | 51 |
| EI_EXPOSE_REP2 | `project.project.Entity.seller.Seller` | 83 |
| EI_EXPOSE_REP | `project.project.Entity.seller.SellerApplication` | 247 |
| EI_EXPOSE_REP | `project.project.Entity.seller.SellerApplication` | 119 |
| EI_EXPOSE_REP2 | `project.project.Entity.seller.SellerApplication` | 97 |
| EI_EXPOSE_REP2 | `project.project.Entity.seller.SellerApplication` | 81 |
| EI_EXPOSE_REP2 | `project.project.Entity.seller.SellerApplication` | 251 |
| EI_EXPOSE_REP2 | `project.project.Entity.seller.SellerApplication` | 123 |
| EI_EXPOSE_REP | `project.project.Entity.seller.SellerBankAccount` | 51 |
| EI_EXPOSE_REP2 | `project.project.Entity.seller.SellerBankAccount` | 35 |
| EI_EXPOSE_REP2 | `project.project.Entity.seller.SellerBankAccount` | 55 |
| EI_EXPOSE_REP | `project.project.Entity.user.Address` | 63 |
| EI_EXPOSE_REP2 | `project.project.Entity.user.Address` | 44 |
| EI_EXPOSE_REP2 | `project.project.Entity.user.Address` | 67 |
| EI_EXPOSE_REP | `project.project.Entity.user.AuthSession` | 32 |
| EI_EXPOSE_REP2 | `project.project.Entity.user.AuthSession` | 27 |
| EI_EXPOSE_REP | `project.project.Entity.user.Customer` | 49 |
| EI_EXPOSE_REP2 | `project.project.Entity.user.Customer` | 35 |
| EI_EXPOSE_REP2 | `project.project.Entity.user.Customer` | 28 |
| EI_EXPOSE_REP2 | `project.project.Entity.user.Customer` | 53 |
| EI_EXPOSE_REP2 | `project.project.Filter.JwtAuthenticationFilter` | 52 |
| EI_EXPOSE_REP2 | `project.project.Filter.SellerApplicationAdminFilter` | 29 |
| EI_EXPOSE_REP2 | `project.project.Security.SessionAuthenticator` | 21 |
| EI_EXPOSE_REP2 | `project.project.Service.implement.AuthServiceImp` | 49 |
| EI_EXPOSE_REP2 | `project.project.Service.implement.AuthServiceImp` | 51 |
| EI_EXPOSE_REP2 | `project.project.Service.implement.AuthServiceImp` | 57 |
| EI_EXPOSE_REP2 | `project.project.Service.implement.AuthServiceImp` | 50 |
| EI_EXPOSE_REP2 | `project.project.Service.implement.CustomerCheckoutService` | 41 |
| EI_EXPOSE_REP2 | `project.project.Service.implement.CustomerCheckoutService` | 40 |
| EI_EXPOSE_REP | `project.project.Service.implement.CustomerCheckoutService$Context` | 49 |
| EI_EXPOSE_REP | `project.project.Service.implement.CustomerCheckoutService$Context` | 49 |
| EI_EXPOSE_REP | `project.project.Service.implement.CustomerCheckoutService$Context` | 49 |
| EI_EXPOSE_REP | `project.project.Service.implement.CustomerCheckoutService$Context` | 49 |
| EI_EXPOSE_REP2 | `project.project.Service.implement.CustomerCheckoutService$Context` | 49 |
| EI_EXPOSE_REP2 | `project.project.Service.implement.CustomerCheckoutService$Context` | 49 |
| EI_EXPOSE_REP2 | `project.project.Service.implement.CustomerCheckoutService$Context` | 49 |
| EI_EXPOSE_REP2 | `project.project.Service.implement.CustomerCheckoutService$Context` | 49 |
| EI_EXPOSE_REP | `project.project.Service.implement.CustomerCheckoutService$PlaceOrder` | 54 |
| EI_EXPOSE_REP2 | `project.project.Service.implement.CustomerCheckoutService$PlaceOrder` | 54 |
| EI_EXPOSE_REP | `project.project.Service.implement.CustomerCheckoutService$SyncCart` | 51 |
| EI_EXPOSE_REP2 | `project.project.Service.implement.CustomerCheckoutService$SyncCart` | 51 |
| EI_EXPOSE_REP2 | `project.project.Service.implement.OrderStateLock` | 19 |
| EI_EXPOSE_REP2 | `project.project.Service.implement.ProductServiceImp` | 45 |
| EI_EXPOSE_REP2 | `project.project.Service.strategy.shipping.ShippingFeeStrategyFactory` | 13 |

## Git history secret scan

Gitleaks originally reported two generic-api-key matches in JWT test fixtures introduced by commit f11288c2923f5d1798264… . Historical fixture literals were compared in memory against the application's configured secret values; no matches were found. No configured secret value was printed or saved in this report.

Current Spring test contexts generate a fresh 32-byte SecureRandom JWT key through TestJwtEnvironment on the test classpath. JwtTokenServiceTest generates its signing keys at runtime through JJWT's HS256 key builder. The historical findings still required a narrowly scoped .gitleaks.toml allowlist because Git history must be preserved: each exception requires both one exact backend/src/test/ file path and its exact known dummy value pattern. Default rules and generic-api-key remain enabled, including for other values in those test files. No history was rewritten. A fresh scan of all 222 commits and current Git-managed/nonignored files reports zero leaks.

A separate stdin probe supplied a temporary random synthetic key to the same configuration. Gitleaks correctly detected generic-api-key and exited 1 as expected; its report was fully redacted and the canary was not added to source/history. This verifies the rule remains active outside the approved exceptions.

- `backend/src/test/resources/application.properties:2` (generic-api-key)
- `backend/src/test/java/project/project/Service/JwtTokenServiceTest.java:13` (generic-api-key)

## Functional verification

- Backend: 371 tests, zero failures/errors/skips.
- Frontend: 80 tests, zero failures/skips.
- Final npm audit: seven high findings (braces, chokidar, fast-glob, micromatch, tailwindcss, @next/eslint-plugin-next, eslint-config-next) and one critical finding (Swiper). Tailwind is exactly 3.3.3; Swiper 10.3.1, ESLint 9.39.5 and eslint-config-next 15.5.27 retain their original major versions. Temporary major changes and the cross-major Glob override were reverted. Earlier zero- or five-finding audits do not represent this final dependency tree. The [Swiper vendor advisory](https://github.com/nolimits4web/swiper/security/advisories/GHSA-hmx5-qpq5-p643) identifies 12.1.2 as patched, which exceeds the permitted major; no scanner exception or package patch was used to conceal this finding.
- Production frontend build with Tailwind 3.3.3: PASS. PostCSS configuration, application CSS and compatibility class edits were restored to their previous contents.
- Frontend lint: zero errors; ten existing React hook dependency warnings.
- Semgrep strict scan: zero findings and zero parsing errors.
- Both live servers return CSP, X-Content-Type-Options, X-Frame-Options and Referrer-Policy.
- Live backend verification uses an isolated in-memory H2 database with demo seeding disabled and a temporary random JWT key. The user's PostgreSQL container/database was not changed; this run does not verify that database's connectivity or existing data.
- Dependency Check: official NIST JSON 2.0 feed initialization and scanning completed in 20:27 minutes, using metadata updated on 2026-10-05. The initial scan found nine Tomcat findings with CVSS >= 7 plus the DevTools match. Tomcat was patched from 11.0.24 to 11.0.26 within the same major version; all 371 backend tests passed again. A fresh completed dependency scan reports no Tomcat vulnerabilities, but still fails on spring-boot-devtools-4.1.1 / CVE-2022-31691 (CVSS 9.8).

## Unapproved Dependency Check match

CVE-2022-31691 is identified in the [Spring advisory](https://spring.io/security/cve-2022-31691/) as a Spring Tools IDE extension vulnerability. The Maven artifact scanned here is Spring Boot DevTools. Comparing the affected product with this artifact indicates a probable CPE/name-matching false positive. This is an inference from the report and vendor advisory, not an approved scanner exception: the finding remains unsuppressed and Backend dependency acceptance remains FAIL. DevTools functionality is retained. No additional Dependency Check exclusion was added.

The real Tomcat security updates are documented in the [Apache Tomcat 11 security advisories](https://tomcat.apache.org/security-11.html). Spring Boot remains at 4.1.1, and Tailwind remains at 3.3.3.

Raw reports are under backend/target/ and are intentionally not committed. No API key is included in this review.

## Metric acceptance

| # | Metric | Result |
| --- | --- | --- |
| 1 | Frontend dependencies | FAIL: seven high findings plus one critical Swiper finding with all original dependency majors retained |
| 2 | Backend dependencies | FAIL: completed NVD scan still reports the unapproved DevTools/Spring Tools match; Tomcat findings are resolved |
| 3 | Secret leakage | PASS: zero leaks in history/current files after runtime random fixtures and the approved exact test-path/dummy-value exceptions |
| 4 | Semgrep frontend/backend | PASS: strict scan, zero findings and parsing errors |
| 5 | SpotBugs + Find Security Bugs | PASS: zero findings with only the explicitly approved XML exclusions |
| 6 | Dangerous frontend APIs | PASS: source pattern check |
| 7 | NEXT_PUBLIC secrets | PASS: source/config variable-name check |
| 8 | SQL string concatenation | PASS: pattern check; reset statements now use fixed literals |
| 9 | Password hashing | PASS: BCrypt at customer/seller creation; authentication tests pass |
| 10 | CORS | PASS: exact origin configuration plus allow/deny integration tests |
| 11 | Actuator exposure | PASS: health only; no wildcard exposure |
| 12 | Request body validation | PASS: every RequestBody is annotated Valid; invalid profile integration test returns 400 |
| 13 | Access control | PASS: real filter-chain tests for 401/403 and account boundaries |
| 14 | Login rate limit | PASS: sixth request returns 429; expiry/concurrency/source separation tests pass |
| 15 | Security headers | PASS: all four required headers from live ports 3000 and 8080 |
| 16 | Tests | PASS: backend 371 and frontend 80; no skips |
| 17 | Environment/config | PASS: existing ignore policy verified; main secret config uses environment references |

The final complete Bash gate after the Tomcat patch and restoration of all original dependency majors finished with exit code 1 on 2026-10-06. Its actual log/status are backend/target/security-gate/full-gate-original-majors.log and full-gate-original-majors.exit. All checks completed: only frontend audit and backend Dependency Check fail. Semgrep and both servers' required headers passed in this final run. The cached Dependency Check completed in 14.204 seconds; initial official-feed initialization/scanning took 20:27 minutes. No script, CVSS threshold or additional scanner exception was changed. Frontend dependency findings and the unapproved DevTools match prevent full acceptance. Temporary validation servers are stopped after verification; the PostgreSQL container is left running.
