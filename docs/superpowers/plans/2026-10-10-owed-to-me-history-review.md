# Owed to me History Review Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Let users find a person's imported transactions, confirm loans and repayments individually or in batches, and see accurate debt labels and totals.
**Architecture:** Extend the existing personal receivable journal. A pure matching/validation policy feeds a local repository and a separate Compose review flow; original cash transactions remain authoritative.
**Tech Stack:** Kotlin, Android SQLite, Jetpack Compose, JUnit, existing GitHub Android CI.
**Spec:** docs/superpowers/specs/2026-10-10-owed-to-me-history-design.md (approved 10 October 2026).

## Global Constraints

- No new permissions, analytics, network processing, or raw-SMS upload.
- Matching is a suggestion; all financial links need user confirmation.
- Shared bills retain the original expense category for the user's remaining share; fees remain spending.
- Use integer minor units and checked arithmetic.
- Build, unit tests, lint and debug APK assembly required; instrumentation off unless explicitly requested.
- Isolate changes from open PRs #312 and #289. Do not merge the new PR without task-specific authorization.

## Review Focus

- Two people share a name: show uncertainty; do not merge them.
- Kenyan phone variants or conflicting phones: normalize valid equivalents, block conflicting bulk selection.
- Source changes after preview: revalidate under the ledger lock; reject the whole stale batch.
- Incoming gift or receipt predates loan: never silently reduce outstanding debt.
- Restored review decisions: preserve gift exclusions and avoid duplicate debt links.

## File Structure

New domain files under app/src/main/java/com/kedevjimmy/pesatrack/domain/receivables/: ReceivableHistoryPolicy.kt and ReceivableBatchPolicy.kt.
New repository: app/src/main/java/com/kedevjimmy/pesatrack/data/local/ReceivableHistoryRepository.kt.
New UI: app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/ReceivableHistoryReview.kt and ReceivableActivityPresentation.kt.
Extend PersonalReceivableStore.kt, PesaTrackDatabase.kt, PesaBackupManager.kt, PersonalReceivablesScreen.kt, ActivityScreen.kt and TransactionDetailScreen.kt only at their integration points.
Tests mirror the new domain files under app/src/test/java; Android migration/store fixtures extend existing integration files.

### Task 1: Matching and financial batch policy

**Interfaces:** ReceivableHistoryPolicy.match(name: String, phone: String?, counterparty: String?, counterpartyPhone: String?): HistoryMatch; HistoryMatch carries reason and bulkEligible. ReceivableBatchPolicy.validate(existing: List<DebtMovement>, selected: List<DebtMovement>): BatchTotals; DebtMovement carries transactionId, kind, amountMinor and occurredAt; BatchTotals carries lentMinor, repaidMinor and outstandingMinor.
- [ ] Write JVM tests for equivalent 07/+254 phones, normalized full names, partial search, same-name conflicting phones, and absent identity; assert conflicting matches are not bulk eligible.
- [ ] Write batch tests for KES 5,000 lent/KES 2,000 repaid/KES 3,000 outstanding; partial KES 800 share of KES 1,600 bill leaves fees outside principal; overpayment, duplicate IDs, repayment before loan and arithmetic overflow are rejected.
- [ ] Run targeted JVM tests in CI to observe failure before production policy is added.
- [ ] Implement pure policies, with deterministic chronology and checked arithmetic.
- [ ] Run targeted tests and full build checks; commit the verified policy.

### Task 2: Local history and atomic confirmation

**Interfaces:** ReceivableHistoryRepository.search(query: String, personId: String?, limit: Int, offset: Int): List<ReceivableHistoryCandidate>; candidates carry canonical row, match reason, eligibility/reason, existing link and review exclusion. PersonalReceivableStore.confirmHistory(personId: String?, name: String, phone: String?, selections: List<ReceivableHistorySelection>, exclusions: List<Long>): String returns the resolved person ID after atomic save. Selections carry transactionId, kind, amountMinor and the previewed source identity.
- [ ] Add synthetic tests for gifts, already linked/excluded/unresolved rows, self-transfers/mirrors, reversals/refunds, existing splits, stale preview and cancellation.
- [ ] Add schema 37 personal_receivable_review_exclusions table keyed by person ID and transaction ID; unique decisions survive replay.
- [ ] Implement bounded all-history canonical search independent of Activity month. Reuse imported counterparty evidence and existing transaction mapping; use bound SQL parameters and no raw-message network access.
- [ ] Implement confirmHistory under CanonicalStoreLock and one database transaction: load current sources, validate all selections, calculate chronological debt, then insert links/exclusions. On any error roll back everything. Keep record() behavior compatible.
- [ ] Add v36→37 migration and encrypted restore fixtures preserving existing people/events and review exclusions; repeat restore/confirmation without duplication.
- [ ] Verify JVM policies and CI build. Android fixtures are authored/compiled, not executed unless requested.

### Task 3: Review UI and person summaries

**Interfaces:** ReceivableHistoryReview(initialPersonId: String?, initialName: String, initialPhone: String?, onDismiss: () -> Unit, onSaved: (String) -> Unit). ReceivableActivityPresentation produces confirmed debt label/person/principal from existing links.
- [ ] Add presentation/state tests: default selection empty, Select all applies only to eligible displayed rows, gifts are excluded, repayment suggestions stay separate, partial amounts update preview.
- [ ] Implement search → loans/gifts → possible repayments → summary → confirm. Show match reason, counterparty, account, date and amount; paginate explicitly. Support individual edits and bulk loan selection, with final count/amount preview.
- [ ] Add Find in transactions to Owed to me and person details; offer history review after Activity loan confirmation. Reuse existing import route when search history is missing.
- [ ] Show lent/repaid/outstanding on person details and cards. Preserve unavailable balances and correction access. Provide usable loading, empty, retry and validation states; prevent repeated submissions.
- [ ] Add integration journey fixtures for individual confirmation, bulk loans, gift exclusion and repayment preview; compile through APK build.

### Task 4: Activity labels, verification and review

**Interfaces:** Confirmed loans display Debts → Owed to me; confirmed receipts display Debts → Repayment received. Activity Owed to me filter uses active journal links rather than rewritten expense categories.
- [ ] Add tests that shared bills keep the remaining personal expense category and full cash movement, while loan/repayment labels reflect confirmed principal only.
- [ ] Integrate presentation in Activity rows/details and debt filter; selecting loan classification opens confirmation.
- [ ] Update CURRENT_STATE.md and README.md schema/current flow documentation; retain instrumentation opt-in policy.
- [ ] Inspect whole diff for financial identity guessing, hidden bulk selections, UI thread database reads, duplicate journals and backup omissions.
- [ ] Run CI unit tests, lint, APK assembly, existing 232 financial contract cases and Worker checks; inspect actual logs and artifacts on the final SHA.
- [ ] Create and attach an isolated draft PR with results, authored-but-unrun Android tests, and Redmi acceptance checklist. Request an independent review when authorized by the review skill, resolve findings and rerun affected checks. Report remaining device-only validation.

## Execution

Recommended: Native execution in this session. The matching, atomic save and UI interfaces depend on each other closely; implementing them together avoids repeated context setup. An independent whole-branch review should check financial correctness before the work is presented as ready.
