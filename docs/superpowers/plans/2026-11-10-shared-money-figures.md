# Shared Money Figures Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove salary-cycle dependence from current available money and use the same trusted account balances across Home, Activity and Plan.

**Architecture:** Reuse the existing `PaydaySnapshotStore` evidence and `PlanSpendableBalancePolicy`. Add a small shared balance policy for observed/manual eligibility; update screen adapters, not the canonical ledger. Historical Statements remain separate as-of evidence.

**Tech Stack:** Kotlin, Jetpack Compose, SQLite, JUnit JVM tests, Gradle 9.5, GitHub Android CI.

**Spec:** `docs/superpowers/specs/2026-11-10-shared-money-figures-design.md`

## Global Constraints

- Local-first: no financial data leaves the phone; no added cloud requests or permissions.
- No schema, migrations, statement assurance upgrades or source-to-ledger statement posting; N2 admission remains held.
- Keep existing Home/Activity/Plan/Accounts/More navigation and visual theme.
- Verified balances use `OBSERVED` or `USER_ENTERED`; `RECONSTRUCTED`, unknown, duplicate-conflicting and overflowing evidence never create confirmed totals.
- Current account totals use owned enabled non-cash spendable accounts; protected savings separately.
- Use integer minor units, no floating-point currency; no salary-based balance fallback.
- Tests are pure JVM where possible; no emulator or instrumentation requested.

## Review Focus

- Same provider balance imported twice must not double the total: Task 1 duplicate test.
- One known and one unknown owned spendable account must keep the total unavailable: Task 1 unknown test.
- Reconstructed balance must not appear as confirmed spendable money in any screen: Tasks 1 and 3 tests.
- No salary cycle but direct bank/wallet balances and unpaid bills must still show available money: Task 2 regression.
- Scheduled bill already paid must not reduce available money again: Task 2 regression.

---

### Task 1: Shared verified-balance rules

**Files:**
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/VerifiedAccountBalancePolicy.kt`
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/ui/screens/VerifiedAccountBalancePolicyTest.kt`

**Interfaces:**
- Consumes: `List<PaydayAccountSnapshot>`, existing quality/spendability enums and `CheckedMoney`.
- Produces: `VerifiedAccountBalancePolicy.trustedAmount(snapshot): Long?`, `.spendableTotal(snapshots): Long?`, and `.groupTotal(accountIds, snapshots): Long?`.

- [x] Added synthetic regressions for two accounts, protected savings, reconstructed/missing/stale/duplicate-conflicting amounts and incomplete groups.
- [x] Established CI RED on three targeted original failing cases (run 42870074542); local Gradle distribution unavailable.
- [x] Implemented shared source policy with exact confirmed evidence and dedup rules, no transaction writes.
- [ ] Re-run the focused suite, then the applicable broader JVM suite; require green exact-head CI.
- [ ] Commit `test/feat(balance): centralize verified account balance policy`.

### Task 2: Home current availability independent of salary

**Files:**
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/HomeSummaryScope.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/HomeScreen.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/BrownHomeComponents.kt` (label only)
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/PlanCalendarJourneyComponents.kt` (main Available display and separate balance caption)
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/PlanScreen.kt` (retain distinct pay-cycle breakdown label)
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/ui/screens/HomeSummaryScopeTest.kt`

**Interfaces:**
- Consumes: Task 1 verified balance policy, existing `PlanSpendableBalancePolicy.calculate(...)`, current tracked accounts, confirmed IDs, current balance snapshots, scheduled unpaid bills.
- Produces: Home hero's existing `currentAvailableMinor` value from current account evidence minus unpaid bills, no `confirmedCycle` gate; Plan main Available uses the already-calculated `spendable` instead of salary-cycle `available`, with separate current account total and truthful after-bills captions.

- [x] Added regression for known current accounts with no salary cycle, preserving historical month net flow.
- [x] Observed RED in exact-head GitHub CI.
- [x] Replaced salary-cycle `currentAvailableMinor` dependency with the shared Plan spendable calculation; use existing bill total helper and account snapshots. Show `data.spendable` in Plan detail's main card, `data.totalAccounts` as the separate current account total (including protected savings); reserve `data.available` for pay-cycle details. Preserved current layouts.
- [ ] Run focused and JVM suites, then exact-head CI.
- [ ] Commit `fix(home): derive available money from balances and unpaid bills`.

### Task 3: Reuse eligibility across Activity, Plan and Home accounts

**Files:**
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/ActivityBalancePolicy.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/PlanAccountBalanceReader.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/BrownHomeComponents.kt`
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/ui/screens/ActivityBalancePolicyTest.kt`
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/domain/rules/PlanSpendableBalancePolicyTest.kt`

**Interfaces:**
- Consumes: Task 1 policy and existing `PlanSpendableAccount`/`PaydayAccountSnapshot`.
- Produces: same direct-observed/manual eligibility everywhere; no cash added by Plan when Activity excludes it, no reconstructed amount in current available, unknown remains unknown.

- [x] Added parity tests for Activity/Plan, reconstructed balances, unknown/duplicate evidence and protected savings.
- [x] Observed RED for reconstructed Home group before implementation in GitHub CI.
- [x] Delegated Activity and Home group totals to Task 1; map Plan account values through Task 1; aligned current account selection to verified non-cash accounts.
- [ ] Run focused and broader JVM suites, require exact-head Android CI.
- [ ] Commit `fix(finance): align verified balance eligibility across screens`.

### Task 4: Documentation and final verification

**Files:**
- Add: this architecture spec and plan, explicitly stating remaining phone/statement gaps

**Interfaces:** No new runtime interfaces.

- [ ] Update only claims established by code and executed tests; name unverified Redmi balance behavior.
- [ ] Run `git diff --check`, financial synthetic tests, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug` in CI.
- [ ] Verify exact remote PR head and summarize remaining N2/device gates.
- [ ] Commit `docs: explain shared spendable balance authority and verification limits`.
