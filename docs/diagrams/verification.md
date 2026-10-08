# Documentation verification

Verification date: **2026-10-08 (Asia/Bangkok)**.

## Diagram and source checks

| Check | Result |
| --- | --- |
| Required diagram categories | All eight categories covered by 19 standalone diagrams |
| PlantUML syntax and local rendering | PASS using PlantUML 1.2026.8, Java 21 and bundled Windows Graphviz |
| Editable source / rendered image pairing | 19 `.puml` sources and 19 corresponding `.svg` images; `_style.puml` is the shared include |
| Visual review | All 19 reviewed using temporary white-background PNG previews; labels and notes checked for clipping/overlap |
| Use case consistency | 27 unique diagram IDs match 27 descriptions with actors, triggers, preconditions, main/alternative flows, postconditions and source links |
| ER coverage | 21 tables, 158 columns and 28 foreign keys; includes the two-column composite-PK `product_categories` join table |
| JPA / SQL cross-check | 20 JPA entity tables, 156 mapped columns plus the two join columns; no mapped column/type/explicit-nullability/unique mismatch found |
| Behavioral source review | Actual authentication, ownership guards, transactions, payment paths, notifications and status assignments checked against controllers/services/frontend proxies |
| Application changes | None: public APIs, schema, application behavior and dependency versions unchanged; pre-existing `backend/db/delete-product.sql` untouched |

The [README](README.md) documents existing behavior differences and limitations. These diagrams describe repository code and SQL definitions; they do not establish that a deployed database or production installation matches them.

## Security gate: cancelled at the user's request

The user explicitly requested cancellation of security checking and said they would run it themselves. The running `bash scripts/security-check.sh` and its remaining Dependency-Check process were stopped. The temporary frontend/backend servers started for verification were stopped as well. **The full gate did not complete and no security gate pass is claimed.** No further security scans were run after cancellation.

Results produced before cancellation:

- Frontend tests passed.
- Frontend dangerous-API, public-secret and browser-token-storage checks passed.
- Frontend dependency audit failed on the unapproved **high** `tailwindcss` aggregate finding. The existing exception policy accepted seven other findings; it was not modified.
- The backend dependency scan was still running when cancelled. Subsequent backend, secret and Semgrep checks were not completed by the gate.
- Separate preflight HTTP HEAD requests returned frontend 200 and backend 401, each with CSP, content type, frame and referrer headers. This preflight is not a substitute for the cancelled full gate.

The Tailwind finding includes the transitive `braces <=3.0.3` stack-exhaustion issue. At inspection, npm listed 3.0.3 as the latest braces 3.x release and suggested a Tailwind major upgrade. The [published advisory](https://github.com/advisories/GHSA-vfj7-8cjw-p6xm) lists no patched release. Tailwind remains pinned to **3.3.3**, and no overrides, suppressions, ignores, new exceptions, thresholds or gate rules were changed.

The partial execution log and raw audit JSON are local build artifacts in `backend/target/diagram-tools/`; they are not included as documentation deliverables. To finish runtime verification independently, start the intended test servers and run the unchanged gate as described in [the README](README.md#verification).

## Reproduction

- Renderer: PlantUML **1.2026.8** from Maven Central, SHA-256 `0f77e5f769836b3dee340e207fe497c3e4c43e973d559e3c306915da9c32e34c`.
- JVM: Microsoft OpenJDK **21.0.11**.
- Graphviz: bundled Windows **2.44.1**.
- Use `bash doc/diagrams/render.sh check` for diagram syntax and `bash doc/diagrams/render.sh` to regenerate SVGs, after setting `PLANTUML_JAR` as documented.
