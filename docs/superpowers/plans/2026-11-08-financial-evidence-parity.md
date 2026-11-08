# Financial Evidence Parity Implementation Plan

> **For agentic workers:** This owner-approved single-pass implementation follows existing JVM tests and exact-head Android CI; no emulator or instrumentation runs.

**Goal:** Keep existing Statements-to-Activity receipt/category evidence as cautious as the newer same-leg reconciliation flow, without changing any money.

**Architecture:** Preserve the legacy M-Pesa PDF's existing 10-character receipt admission contract (it is not the newer generic cross-source 8–48-character contract). Harden the bounded SQLite source reader to recognize persisted specialized cash links and require settled, account-identified evidence. Keep intentional user-confirmed receivable/investment projections available. All changes are read-only.

**Tech Stack:** Kotlin, SQLite, existing Robolectric/JUnit JVM suite.

**Spec:** Issue #601 and the existing N2 feature gate, both read in this task.

## Global constraints

- Preserve canonical transactions, amounts, category corrections, account balances, user decisions and source archives.
- No database schema, backup, app UI, permission, network, signing or workflow changes.
- No statement-derived posting/combined financial totals while N2 #537 remains on HOLD.
- No emulator/instrumentation; exact-head CI only after the combined implementation, as owner requested.

## Review focus

1. A legacy M-Pesa statement must retain its exact 10-character receipt boundary; widening would expand statement admission while N2 is on hold.
2. A manual, statement-derived or uncertain Activity row cannot masquerade as original independent SMS evidence.
3. An Activity record without an identified account must not supply remembered category evidence to a particular bank/wallet.
4. An inbound refund or debt event linked by a separate journal must not turn the original payment into ordinary category evidence.
5. A saved personal receivable or investment relationship must not be accidentally erased by the specialized cash protection.

## Task 1: Preserve receipt identity and source origin (P1)

**Files:** The existing legacy receipt/evidence policies and their JVM tests.

- [ ] Preserve the legacy M-Pesa exact 10-character receipt contract; do not widen supported statement source formats.
- [ ] Require original SMS/notification origin when pulling Activity receipt evidence.
- [ ] Verify identity, amount, account and time constraints still reject wrong candidates.

## Task 2: Guard persisted specialized cash roles (P1–P2)

**Files:** `data/local/StatementReceiptEvidenceReader.kt`, its new Robolectric test.

- [ ] Exercise debt repayment, inbound refund/reversal, fee/Fuliza metadata and removal of synthetic relationships.
- [ ] Read only SMS/notification origin records and apply relationship checks without writing financial tables.
- [ ] Preserve the explicitly supported receivable/investment category/amount bridge.

## Task 3: Fail closed on uncertain source identity (P2–P3)

**Files:** `domain/everyday/StatementReceiptEvidence.kt`, `StatementActivityEvidencePolicy.kt` and their existing tests.

- [ ] Require an actual matching account for category inheritance and reject missing or mismatched IDs.
- [ ] Reject invalid source times or pending review evidence; do not transform review uncertainty into confirmation.
- [ ] Update `CURRENT_STATE.md`; run diff check, security scan and single exact-head Android CI.
- [ ] Merge only with required checks green; confirm source and signed rolling APK; leave #601 open for N2/device-acceptance work.
