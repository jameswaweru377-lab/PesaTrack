# Bank Statement Account Identity UX Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let Equity users correct a missing full account number from Statements without weakening bank PDF admission.

**Architecture:** A small pure identity/wording policy protects the Kotlin Compose UI from mismatched validation. The import screen updates only the selected account’s local identifier and refreshes its account snapshot; existing bank admission remains authoritative.

**Tech Stack:** Kotlin, Jetpack Compose, existing SQLite `AccountStore`, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-10-30-bank-statement-account-identity-ux-design.md`

## Global Constraints
- No schema, migration, signing, workflow, provider parser or financial semantics changes.
- No private account identifiers in error text or logs.
- Existing M-Pesa/CSV and verified bank archive behavior unchanged.

## Review Focus
- Empty/last-four-digit account: PDF action blocked with actionable explanation.
- Full number including leading zeros: normalized correctly and saved locally.
- Non-digit and masked copy: rejected before writing.
- Wrong full number: backend still blocks as `WRONG_ACCOUNT`, shown as clear guidance.
- Account switches: edits cannot target a previously selected account.

---

### Task 1: Identity validation, error copy and inline correction

**Files:**
- Create `app/src/main/java/com/kedevjimmy/pesatrack/domain/statements/BankStatementAccountIdentityUiPolicy.kt`
- Create `app/src/test/java/com/kedevjimmy/pesatrack/domain/statements/BankStatementAccountIdentityUiPolicyTest.kt`
- Modify `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementImportScreens.kt`
- Modify `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/FinancialInstitutionsScreen.kt` (clarify existing optional last-four-digit guidance)
- Modify `CURRENT_STATE.md` (brief handoff)

**Interfaces:** `normalizeFullNumber(input: String?): String?`, `hasFullNumber(input: String?): Boolean`, `safeBlockedMessage(reason: StatementAdmissionResult.Reason): String`.

- [ ] Write failing JVM cases for complete, partial, masked, malformed, formatted, leading-zero numbers and actionable error messages.
- [ ] Run focused test to confirm expected failure (or report unavailable Android SDK).
- [ ] Add minimal domain helper and conditional bank account editor/button with no API, ledger or schema changes.
- [ ] Run focused tests, then Android CI unit tests/lint/debug APK verification on PR.
- [ ] Review changed files for private data exposure and other screen effects; merge after passing checks per user's standing instruction.
