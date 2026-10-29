# Phase 3B — Statement overlap coverage Implementation Plan

> **For agentic workers:** Required sub-skill: execute this plan with tests before production code. Steps are checklist items.

**Goal:** Establish source-neutral, read-only, account/month statement coverage that identifies proven repeated cash legs, distinct observations and unresolved overlaps before future all-source Statements and cross-product Insights reporting.

**Architecture:** Reuse Phase 3A `StatementEvidenceCorrelationPolicy.Observation` and `StatementEvidenceReadModel` to read separately stored M-Pesa and bank archives. A pure bounded policy groups statement observations per confirmed account, currency and Nairobi month; exact corroborated receipts permit repeat suppression, while missing/contradictory evidence cannot authorize consolidated totals. Keep the Activity ledger, imported archives, category corrections and approved UI untouched; show evidence status only within the existing Statement coverage view.

**Tech stack:** Kotlin/JVM and Android Jetpack Compose; existing stores, no new tables, permissions or dependencies.

**Spec:** `docs/superpowers/plans/2026-10-29-statements-phase3.md` (Phase 3B and product boundary).

## Global constraints

- Keep Activity the canonical daily ledger and Statements immutable independent evidence. Never post statement rows to Activity as a side effect.
- Compare cash legs per confirmed account and currency; never merge bank-to-wallet legs into one account's cash flow, or infer a salary/purchase/loan from narration.
- Only a strong, normalized exact reference plus same timestamp, direction, amount and account is safe for de-duplicating across documents; same receipt can represent multiple fees/legs.
- A missing reference, conflicting candidate or duplicate identity inside the same document yields **needs review**, not automatic suppression.
- A stored document whose whole-file totals were not independently verified can still supply evidence, but cannot authorize safe combined cash figures.
- A calendar month with imported rows is observed coverage, never guaranteed complete coverage; an absent month is unknown, not zero.
- Source totals never become authoritative combined spending/income; potential overlap leaves optional combined cash figures unavailable.
- No private transaction data, passwords, account numbers or personal fixtures in repository or test logs.

## Review focus

- Three PDFs repeat one receipt: one economic cash leg, two corroborating copies; stable regardless of import order.
- Two same-reference same-time legs with different amounts can be principal and fee; do not collapse them.
- Missing Equity reference with same-amount/time across PDFs: unresolved, not duplicate.
- Identical hash re-imports: one saved source; duplicate leg in one document: flag, not delete.
- Two owned accounts with opposite cash directions: independent account evidence; transfer classification belongs to separate proof layer.

---

### Task 1: Pure overlap policy and invented regression cases

**Files:**
- Create `app/src/main/java/com/kedevjimmy/pesatrack/domain/statements/StatementSourceCoveragePolicy.kt`
- Create `app/src/test/java/com/kedevjimmy/pesatrack/domain/statements/StatementSourceCoveragePolicyTest.kt`

**Interfaces:** `StatementSourceCoveragePolicy.analyze(observations: List<StatementEvidenceCorrelationPolicy.Observation>): List<MonthCoverage>`; each `MonthCoverage` includes account, currency, Nairobi month key, source count, observed row count, remaining observed leg representatives after confirmed repeats, exact repeated copies, unresolved candidate count, deterministic status and nullable safe observed money in/out totals.

- [ ] Write invented-data tests for exact two-/three-file repeats, partial complementary statements, unreferenced Equity-like collisions, inconsistent references, duplicate receipt/amount/time in one file, fee/principal separation, two accounts, currencies, reordered inputs and rejected Activity rows.
- [ ] Run focused JVM test and confirm that the missing policy fails compilation (if Android SDK/dependencies are available); otherwise record the local limitation and use the official PR CI for compilation/tests.
- [ ] Implement bounded deterministic grouping and conservative evidence checks; never mutate an observation or the ledger.
- [ ] Run focused JVM test; then the appropriate Android unit/lint/assemble CI suite.

### Task 2: Wire the read-only archive adapter and scoped coverage presentation

**Files:**
- Modify `app/src/main/java/com/kedevjimmy/pesatrack/data/local/StatementEvidenceReadModel.kt`
- Modify `app/src/main/java/com/kedevjimmy/pesatrack/domain/statements/StatementEvidenceCorrelationPolicy.kt` (additive source-verification evidence bit)
- Modify `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementIntelligenceDashboard.kt`
- Modify `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementDeepDivePages.kt`
- Modify `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementBankOverviewPage.kt`

**Interfaces:** The read-model `Snapshot` exposes the policy's per-month `coverage` without changing `correlation`. The M-Pesa **Statement coverage** and Equity bank source views receive selected account/year coverage and present verified-repeat counts and needs-review notices; existing one-source-per-month chart totals remain untouched.

- [ ] Add a focused test demonstrating adapter contract behavior if a pure extracted mapper is needed; all financial matching is tested in Task 1.
- [ ] Compute coverage from existing confirmed statement observations only, with existing 20,000-observation and time-window bounds; do not read or mutate Activity.
- [ ] Load coverage asynchronously when the Coverage page is opened; keep errors nonfatal and show no fabricated combined totals.
- [ ] Verify Kotlin compilation, existing UI tests and CI; review diff for changes in navigation, chart totals, statement import, signing and schema (none intended).

### Task 3: Verification and handoff

- [ ] Verify only the planned files changed and no financial source material was introduced.
- [ ] Open one PR against current `main`; wait for build/lint/unit CI success and inspect failures if any.
- [ ] Merge only after verified green checks under the user's standing workflow instruction, then verify merged commit and rolling APK publication separately.
- [ ] Explain limits clearly: bank reference gaps and cross-product ownership/categories remain Phase 3C/Insights work; synthetic tests are not a phone-verified financial audit.
