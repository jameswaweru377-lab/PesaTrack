# Provider-neutral owned-transfer evidence implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reliably classify completed owned-account transfers across recognised Kenyan financial institutions using verified destination identity, while keeping statements non-posting.

**Architecture:** Extend existing generic transfer facts with recipient evidence and use confirmed money accounts at the ingestion boundary. Reuse current self-name policy, transaction insert/mirror safety, and read-only bank-statement route matcher; no provider-specific branch or database migration.

**Tech Stack:** Kotlin, JUnit/JVM tests, Android/SQLite existing stores, GitHub Android CI.

**Spec:** `docs/superpowers/specs/2026-11-22-provider-neutral-owned-transfer-evidence.md`

## Global Constraints

- No instrumentation or emulator runs; device validation belongs to the owner.
- No new financial sender reads, permissions, schema or network access.
- Do not auto-post bank statement rows to Activity under N2 hold.
- All fixtures use invented names, phones, references and amounts.
- CI test, lint and debug APK must succeed before merging; preserve source history.

## Review Focus

- An SMS to an unrelated wallet number must not become an owned transfer (Task 2).
- Two wallets or two same-provider banks must not resolve to the first account (Task 2).
- An SMS with no full recipient identifier must not auto-transfer from a provider name (Task 2).
- A transfer reference shared by duplicate source observations must not double-count money (Task 2).
- A bank statement mentioning only masked or partial identifiers must not become a proven route (Task 3).

---

### Task 1: Preserve recipient identity through generic transfer parsing

**Files:**
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/domain/interpretation/FinancialCandidateExtractor.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/domain/interpretation/FinancialRelationshipResolver.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/domain/interpretation/FinancialMessageModels.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/domain/interpretation/FinancialEventClassifier.kt`
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/domain/interpretation/GenericTransferMessageShapesTest.kt`

**Interfaces:** Propagate `recipientPhone: String?` and `recipientName: String?` from bank-to-wallet `FinancialActionCandidate` into `TransferEventFact`, without leaking recipient data into diagnostics or altering other event families.

- [ ] Add synthetic tests asserting recipient phone and name survive provider-neutral completed transfer interpretation; pending/failed notices remain non-posting.
- [ ] Run tests and confirm failure on missing evidence (`./gradlew :app:testDebugUnitTest --tests '*GenericTransferMessageShapesTest'` via CI if local wrapper unavailable).
- [ ] Implement only bounded generic extraction and field propagation.
- [ ] Run focused tests to verify passing, then commit.

### Task 2: Confirm the actual destination before automatic accounting

**Files:**
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/domain/identity/SelfNameEvidencePolicy.kt`
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/domain/rules/OwnedTransferRecipientPolicy.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ingestion/FinancialMessageIngestionCoordinator.kt` (extract guard rather than grow `ingest()`)
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/domain/identity/SelfNameEvidencePolicyTest.kt`
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/domain/rules/OwnedTransferRecipientPolicyTest.kt`

**Interfaces:** `OwnedTransferRecipientPolicy.assess(recipientPhone, recipientName, walletIdentifier, profileName)` returns an explicit owned/external/uncertain verdict. The ingestion coordinator uses verified account consent and unique mapping; external destination moves use the established payment path, uncertain evidence remains pending, exact owned destination uses the existing transfer path.

- [ ] Add RED cases for alternate name order, two-name profile, exact phone aliases, unrelated phone, conflicting name, missing number, ambiguous ownership and rule-protected cases.
- [ ] Confirm focused test failure before code changes.
- [ ] Implement pure recipient policy and integrate with the existing transfer/payments path without duplicating ledger writes.
- [ ] Verify tests, existing mirror/replay regressions and commit.

### Task 3: Improve safe statement-route evidence and finish

**Files:**
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/domain/statements/TransferRoutePolicy.kt`
- Test: existing `app/src/test/java/com/kedevjimmy/pesatrack/domain/statements/TransferRoutePolicyTest.kt` (or nearest existing policy suite)
- Update: `README.md`, `CURRENT_STATE.md`, `docs/TRANSFER_RECONCILIATION.md`

**Interfaces:** A completed, directional statement narration with an exact full owned wallet identifier may produce existing `Route`; it remains review-only and still requires unique opposite account evidence.

- [ ] Add RED positive multi-institution statement narrations and negative masked/ambiguous/protected cases.
- [ ] Confirm tests fail for absent route coverage.
- [ ] Extend bounded route grammar; ensure no change to statement posting or legacy decisions.
- [ ] Run full Android JVM tests, lint and debug APK through GitHub CI; compare diff, commit, open PR, confirm checks, merge to `main`, verify post-merge workflow.
