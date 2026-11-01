# Audit remediation rollout design

Date: 1 November 2026. Starting main SHA: `37743e30c0a543c7f6942c53f55bdb2ef16eb775`.

## Intent

Resolve the verified audit findings without losing PesaTrack's trusted local ledger, changing the user's financial history by accident, weakening privacy, or replacing a newer change with an older branch. Every batch is independently testable and reviewable. The external release remains blocked until its evidence gates genuinely pass.

## Approaches

- **One combined remediation PR:** fewer branches, but unacceptable coupled financial, privacy, release, and migration risk; reject.
- **Parallel PRs against different main commits:** faster apparent progress, but conflicts with ongoing Equity, biometric, and instrumentation work; reject for shared data/release files.
- **Recommended and approved direction: sequential scoped PRs:** current main for each, tests first when behavior changes, CI proof, no merge or deployment before its gate; accept the extra review time.

## Invariants and non-goals

- No sync, cloud AI, smart rule, new remote financial content or permission expansion. Existing optional Worker behavior must not receive additional data classes.
- Android minimum API **24**, app ID `com.kedevjimmy.pesatrack`, same signing identity, non-downgrade version code, and no deletion of installed user data.
- Income, expenses, refunds, reversals, fees, debt, and own-account transfers retain distinct integer-minor-unit accounting. A missing category is safer than a fabricated category.
- Existing encrypted backups and older PBKDF2 parameters remain restorable. Do not alter a saved schema silently.
- Existing approved screens remain visually unchanged unless their specific audit finding requires it.
- No new SDK/dependency without size, license, and migration review; no private financial test fixtures.
- Do not rebase, amend, merge, or edit another chat's open PR or branch.

## Sequencing and specific choices

| Batch order | ID(s) | Deliverable and gate |
|---|---|---|
| 1 | **R1** | Repeatable signed-SHA physical-device acceptance sheet with explicit PASS/FAIL/NOT RUN. Documentation and process only; cannot itself certify the phone. |
| 2 | **R2** | Document current arm64-only install limit and preserve Python 3.13; decide any ARMv7 build only after actual install/device figures. No speculative 32-bit change. |
| 3 | **T1** | Reuse/coordinate the active instrumentation PR #505, then introduce nightly and exact-release-SHA emulator gate, keeping ordinary PR checks short. |
| 4 | **N3** | Restrict manual release signing to approved refs, independent protected environment and owner-confirmed offline keystore recovery. No new key. |
| 5 | **R3** | Release R8/shrinking, targeted keep rules, measured APK delta and minified PDF/backup/lock tests; physical phone retest before public tag. |
| 6 | **N2** | Feature-admission rule: no new statement features until R1 and T1 evidence is green. |
| 7 | **S5** | Shared Kotlin/JavaScript privacy regression corpus, fail closed on ambiguous person-like descriptors, preserve PAY_BILL/BUY_GOODS opt-in only. |
| 8 | **S4** | Robust abuse budgets and rate keys, rotating UUID and carrier-NAT regression tests; deployment separately reviewed. |
| 9 | **T3** | Android fault-injection test then isolate post-commit diagnostics; committed ledger result never reported as failed. |
| 10 | **N1** | Type-first reversal/fee semantics, never bulk recategorize existing ledger rows; preserve 600-case exam and require money parity. |
| 11 | **S3** | Stronger new-backup passphrase policy and benchmarked PBKDF2 cost, with 180k restore and API24 tests. |
| 12 | **X1** | Correct historical Python, schema 40, wrapper and signed release wording; include same-change docs in all earlier PRs. |
| 13 | **S2** | First threat model SQLCipher versus disclosed app-private SQLite. Keep current storage until a separately approved atomic, recoverable migration proves necessary. |
| 14 | **S6** | Contain normal-process PDF parsing, retain offline operation and full-document reconciliation; do not overlap ABI/Python upgrade. |
| 15 | **A2** | Move BillCadence enum to domain without changing persisted names. |
| 16 | **T4** | Debug StrictMode, main-thread I/O dispatch, timed global lock with synthetic large-history measurement. |
| 17 | **T2** | Incrementally extract ingestion stages and small result types behind unchanged public contract. |
| 18 | **A1** | Small, behavior-neutral file extractions protected by actually run DB, money and UI tests. |
| 19 | **T5** | Lifecycle state holders one screen at a time, starting read-only and ending with complex mutation screens. |

R1, N1, R3 and the T1 release gate are external release blockers. No code/doc PR may claim a physical-device PASS based solely on emulator or unit evidence. R2 also remains an explicit supported-device decision before broad distribution.

## Dependency constraints

T1 precedes T3, T2 and A1 risky changes. N1 classification must not overlap T2, A1 or T4 concurrency changes. S3 and S2 remain separate. S6 and R2 parser/ABI changes remain separate. R3 must not overlap refactoring or Python dependency changes. S4 and S5 get separately reviewed PRs, then one explicitly approved Worker deployment. N3 signing protection precedes signed public release. R1 physical acceptance is repeated for the actual final minified/signed release candidate.

## Review, rollback and completion

Every batch starts on fresh main and records a unique PR, task ID and owner. Failing regression first for behavior changes, then a focused green test, broader JVM/lint/APK and relevant emulator evidence. The rollback is the previous compatible signed build or a single PR revert **only when that does not strand migrated data**; backup/schema/Keystore changes require explicit recovery tests before deployment. Update relevant docs in the same PR.

The owner may authorize the recommended code direction, but **cannot delegate evidence that has not occurred**: real-device testing, offline keystore possession, GitHub environment settings, 32-bit install telemetry and measured low-end KDF latency must be recorded when available. Missing evidence keeps the associated release gate open, not silently skipped.
