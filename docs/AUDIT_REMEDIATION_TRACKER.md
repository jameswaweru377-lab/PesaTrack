# PesaTrack — Whole-System Audit Remediation Tracker

**Audit baseline:** 29 October 2026 PDF report, audit commit `87096582`.  
**Tracker created:** 30 October 2026. **Repository snapshot checked:** `main` at `e82a7cd10a346fefe07870624418f88d6282b7b1`.  
**Purpose:** Every audit finding stays visible until implementation **and** its required verification are evidenced. This is a historical snapshot, not an automatic live CI dashboard; check GitHub again before changing any status.

## How to read this tracker

- **Open** = work not verified as resolved, even if some infrastructure exists.
- **Partial** = code or documentation progressed, but an important acceptance gate remains.
- **Code merged; verification pending** = PR landed but deployed/device behavior is not yet proven.
- **Deferred** = deliberately postponed by James. It is **not** fixed.
- **Closed** only when the relevant PR, CI, merge, and real-world acceptance evidence are recorded. A green CI build alone never proves a real Redmi import, update or backup restore.

**Concurrent ownership:** The "Continue PesaTrack UI Design" chat owns the ongoing Statements/import/reconciliation track. Its [PR #476](https://github.com/jameswaweru377-lab/PesaTrack/pull/476) was **OPEN / NOT MERGED** at this snapshot. Do not merge it from this tracker or modify its branch. Keep unrelated audit fixes in separate feature branches, rebase on the latest `main`, and review each diff before combining. No cross-chat automatic synchronization exists except through merged commits on `main`.

## Complete original findings register (20 of 20)

| ID | Priority | Finding / risk | Current evidence and status | Next acceptance gate |
|---|---|---|---|---|
| R1 | P0 | End-to-end M-Pesa + Equity import and reconciliation on a real phone not proven | **Open; other chat actively working.** Latest source/overview PRs add read-only coverage and reconciliation tests, not full real-device acceptance. | Private Redmi import → review → reconcile → repeat import → verify Activity, source Statements, account totals and backup; no double counts. |
| R2 | P0 | APK is ARM64-only despite `minSdk 24`; older 32-bit devices excluded | **Open.** Android Gradle config uses `abiFilters` for `arm64-v8a`; Chaquopy/PDF dependency needs ABI review. | Agree supported devices, verify ABI outputs, install matrix or explicitly document the restriction. |
| R3 | P0 | Release lacks R8/minify/resource shrinking | **Open.** No verified enabled `isMinifyEnabled` / `isShrinkResources` release flags. | Build shrunk signed APK; regression-test reflection, PDF/Python, DB migrations, backup, import, native libs. |
| R4 | P0 | Unstable debug signing prevents reliable in-place APK upgrades | **Partial.** [#469](https://github.com/jameswaweru377-lab/PesaTrack/pull/469) and [#471](https://github.com/jameswaweru377-lab/PesaTrack/pull/471) established the permanent signed rolling two-asset release; CI publication verified. | Verify same package, certificate, increasing `versionCode`, actual Redmi in-place update and retained local data; archive offline key recovery copy. |
| S1 | P1 | App-lock grace and PIN lockout tied to changeable wall clock | **Code merged; device verification pending.** [#470](https://github.com/jameswaweru377-lab/PesaTrack/pull/470) moved timing to boot-scoped elapsed time. | Test screen lock/background, clock change, reboot and PIN lockout on device. |
| S3 | P1 | Backup passphrase policy too weak (min 8, PBKDF2 180k) | **Deferred at owner's request.** Existing backups and recovery compatibility must be preserved. | Agree password/KDF migration and backward compatibility; test encrypted restore and wrong-password handling. |
| S4 | P1 | Worker rate limiter can be evaded via client-supplied install UUID | **Open.** IP-based limits have shared-mobile/NAT fairness implications; design decision not made. | Threat model and layered abuse controls that protect legitimate users; negative tests and deployed verification. |
| S5 | P1 | Merchant enrichment descriptor privacy sanitization | **Partial.** Android + Worker sanitizer tests merged in [#473](https://github.com/jameswaweru377-lab/PesaTrack/pull/473); Worker production deployment and end-to-end privacy behavior not proven. | Owner review of heuristic limitations; controlled Worker deploy, negative input corpus and network payload verification. |
| A1 | P1 | Very large `TransactionStore.kt`, `ActivityScreen.kt`, other monoliths | **Open.** No verified incremental split accepted as audit closure. | Small behavior-preserving extractions, tests, compare performance and stable UI. |
| U1 | P1 | Design tokens not consistently used; hard-coded colors/dimensions/radii | **Open, partial infrastructure exists.** `Color.kt` and `PesaDesignTokens.kt` exist but screens still embed values. | Gradual component/token adoption; verify approved screenshots; avoid blanket palette changes. |
| U2 | P1 | Obsolete emerald `DARK_MODE_V2_APPROVED.md` conflicts with current indigo/light UI | **Documentation work in this PR** (if merged). Current `DESIGN.md` and a historical notice distinguish the approvals; no UI pixels changed. | Verify documentation references and approved four-tab light design. |
| U3 | P1 | Financial totals differ across Home, Activity, Statements, reports | **Open.** Gross source cash movements remain distinct from unified economic income/expenses; #476 is read-only evidence work. | Shared financial definitions and invariant tests across all surfaces, including transfers/refunds/loans. |
| X1 | P1 | Stale/contradictory engineering docs and Python runtime claims | **Partial documentation cleanup in this PR** (if merged). Full `CURRENT_STATE.md` factual refresh and wider doc review remain. | Audit the current source and refresh outdated copy without masking unknown runtime or device limits. |
| S2 | P2 | PIN/biometric UI lock but unencrypted at-rest SQLite | **Open.** App lock is not the same as encrypted database. | Explicit threat model, encryption/key management/migration/backup design and device tests. |
| S6 | P2 | Embedded Python PDF and transitive dependency/isolation maintenance risk | **Open.** Different extraction routes exist; do not confuse offline Python test laboratory with Chaquopy packaged parser. | Map process isolation, dependency pins, updates, failure modes and package footprint. |
| D1 | P2 | Multiple statement database helpers vs a single owner | **Open.** Refactor only with migration and data ownership tests. | Single access boundary, safe migrations and replay/rollback tests. |
| D2 | P2 | Backup memory cost and hard-coded schema bridge (`40`) | **Open.** Exact current behavior must be re-measured/rechecked before a fix. | Stream/limit large backups safely; schema compatibility and lossless restore tests. |
| U4 | P2 | Visual and navigation inconsistencies / misleading labels or empty states | **Open.** Separate approved screen designs and source tests exist; whole-app audit not passed. | Screen-by-screen review of format, spacing, actions, contrast, access and small-screen layout. |
| S7 | P3 | Worker-provided `logoUrl` needs strict validation | **Open.** | Domain, scheme, MIME and size allowlist plus attack-path tests. |
| A2 | P3 | Pure domain code depends on Android notification `BillCadence` enum | **Open.** | Move neutral contract to domain without behavior changes; unit tests. |

**Count: 20 findings** (P0 4, P1 9, P2 5, P3 2). Their priority is from the audit, not a promise that they are independent of one another.

## Standards and additional proposals — separate from the 20 findings

The audit also offered engineering standards, not 20 additional completed features:

| Standard / proposal | Where to enforce | Current position |
|---|---|---|
| Single semantic color system, no new screen-local literal colors | `DESIGN.md`, theme review | Documented; whole-app conversion still open (U1). |
| Shared spacing, radius, component hierarchy | Named tokens and reused Compose components | Existing tokens are partial; audit's numeric suggestions are not automatically approved. |
| Consistent money/currency/signs across all surfaces | Financial formatting and domain models | Not closed (U3/U4). |
| Loading, empty, error, partial-data states | Each screen UI test and visual review | Not closed (U4). |
| Accessibility: contrast, TalkBack, 48dp targets, larger fonts | Compose semantics and device acceptance | Real-device review remains pending. |
| Clear labels, time range, chart units and gross-vs-net semantics | Design review and tests | Not closed (U3/U4). |
| Architecture: single DB ownership, smaller files, domain isolation | Incremental architecture PRs | Not closed (A1, A2, D1). |
| Security: minimize transmitted data, stable time, safe backup, bounded dependencies | Threat model, contracts, release gates | Mixed; see S1–S7. |
| AI working protocol: evidence before confidence, current-branch checks, smallest scope, test negative cases, no unauthorized remote writes | `AGENTS.md`, `CONTRIBUTING.md` | Instruction rules exist; compliance must be verified **per PR**, not declared complete once. |

## Recommended sequence and ownership

1. **Continue independently in other chat:** R1 and its unfinished source-evidence and import reconciliation work. Do **not** infer success from #474/#475 overview screens or from green CI without a Redmi acceptance journey.
2. **Immediate independent hardening:** R2 supported-ABI decision and R3 carefully tested shrink release. These may involve signing/release paths, so coordinate with the release owner and get explicit change approval before code edits.
3. **Independent security follow-ups:** S4 design, S5 Worker deployment confirmation, S1 device timing tests. Keep S3 backup password work paused until James resumes it.
4. **Correctness/UI maintenance:** U3 shared totals, U1/U4 progressive UI cleanup, A1 decomposition, D1/D2 safeguards; each in its own small PR.
5. **Later:** S2, S6, S7, A2 after threat-model and dependency discussions.

## Update contract for every audit-related PR

Record: **ID · branch · base SHA · changed files · tests (pass/fail/skipped) · PR · CI run/head SHA · merged SHA · device/deployment evidence · residual risk**. Change the finding status only after checking those fields. If a PR is open, say **NOT MERGED**; if Worker code merged but was not deployed, say **NOT DEPLOYED**. Never describe a docs-only PR as an Android APK fix.
