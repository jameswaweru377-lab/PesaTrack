# Private Financial SMS Import Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent importing or reading private SMS bodies and improve repeated/history import efficiency without changing financial truth.

**Architecture:** A pure sender policy controls an Android SMS inbox reader that fetches metadata first and only loads bodies by trusted row IDs in bounded batches. Historical import and the other on-demand SMS readers consume this one reader; onboarding stops its second full scan.

**Tech Stack:** Kotlin/JVM, Android `ContentResolver` SMS provider, existing SharedPreferences, SQLite importer, existing Compose theme, Python source contracts.

**Spec:** `docs/superpowers/specs/2026-11-21-private-financial-sms-import-design.md`

## Global Constraints

- Offline-only; no new permissions, network calls, dependencies or ledger schema.
- Preserve progress/cancel/resume ordering and transaction accounting.
- No instrumentation/emulator execution. Use synthetic test data.
- Do not change signed release or workflow configuration.

## Review Focus

1. Spoofed sender `EQUITY-HELP` must not be trusted merely for containing `EQUITY` — Task 1 test.
2. Numbered personal sender `0712340000` must never have its body retrieved — Task 2 contract.
3. A trusted sender with a promotional offer must not become a transaction — Task 1/2 existing relevance tests.
4. Cancellation between metadata batches must not skip unprocessed financial records on resume — Task 2 checkpoint test.
5. The same message cannot be inserted twice after onboarding account confirmation — Task 3 contract.

---

### Task 1: Offline sender decision and approvals

**Files:** Create `domain/ingestion/FinancialSmsSenderPolicy.kt`; `data/local/FinancialSmsSenderApprovalStore.kt`; test `app/src/test/.../domain/ingestion/FinancialSmsSenderPolicyTest.kt`.

**Interfaces:** `FinancialSmsSenderPolicy.isTrusted(sender: String?, additional: Set<String> = emptySet()): Boolean`; store `listApproved()/approve()/remove()` for explicit local approvals.

- [ ] Test known banks/M-PESA/loan senders, personal numbers, spoof suffixes, unknown shortcodes, and explicit approval; observe red.
- [ ] Implement exact/bounded sender policy with current bank registry and existing known financial products; run focused tests and contracts.
- [ ] Keep the offline directory authoritative only for sender screening, not transaction/account ownership.

### Task 2: Metadata-only inbox and bounded body fetch

**Files:** Create `ingestion/SelectiveSmsInboxReader.kt`; modify `ingestion/HistoricalSmsImporter.kt`; `ingestion/SalarySmsCandidateFinder.kt`; `ingestion/TransactionMessageEvidenceReader.kt`; tests `tools/test_private_sms_import_contract.py`.

**Interfaces:** reader takes date/ID query and a sender predicate, reports header-total progress and supplies one row at a time in stable order; returns body only for trusted sender IDs.

- [ ] Write source-contract tests showing `BODY` is never selected in metadata and trusted IDs bound every body query; observe red.
- [ ] Implement bounded metadata/body batches in reader; keep required consent and scan-order checkpoint semantics.
- [ ] Migrate import, salary candidate and original-evidence reads; run focused tests, source contracts and negative cases.

### Task 3: Avoid repeat import and expose sender approval

**Files:** Modify `ui/screens/SmartOnboardingSetupScreen.kt`; `ui/screens/HistoricalImportScreen.kt`; create `ui/screens/FinancialSmsSenderApprovalSection.kt`; update `README.md`, `CURRENT_STATE.md`, `docs/PRIVACY_POLICY.md` and test source contracts.

- [ ] Write a contract that fails if onboarding performs the same full-range SMS import both for discovery and again for account selection.
- [ ] Remove redundant second inbox import; use existing identity/transfer reconciliation without losing canonical records.
- [ ] Add an existing-import-settings approval affordance for explicit sender names (no additional onboarding screen).
- [ ] Run source contracts, JVM tests/lint/build via available toolchain or GitHub CI, and review diff for changed money meaning.

### Task 4: Verify and hand off

- [ ] Run `python3 -m unittest tools.test_private_sms_import_contract` and the relevant source/financial contract tests.
- [ ] Run `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` where Gradle is available, otherwise GitHub Android CI after pushing a review branch.
- [ ] Inspect `git diff --check`, changed files, branch head; report exact evidence, unverified device performance and privacy limitations. Do not run instrumentation.
