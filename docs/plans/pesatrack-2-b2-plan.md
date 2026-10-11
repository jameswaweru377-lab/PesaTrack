# PesaTrack Phase B2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans (recommended for the cohesive native execution) or superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Give Activity a conservative, persistent bank-to-M-Pesa transfer review based on explicit ownership and correlated transactions, while explaining the actual statement-import support accurately.

**Architecture:** Add ownership-consent provenance and pair-review records around the existing SQLite ledger. A pure policy identifies eligible unique pairs; an atomic adapter revalidates snapshots and changes canonical classifications only after explicit confirmation. Identity hints strengthen review evidence and never authorize ownership or cash writes.

**Tech Stack:** Kotlin, SQLiteOpenHelper, Jetpack Compose, JUnit, Android instrumentation, existing encrypted backup and Android CI. No new runtime dependencies.

**Spec:** [Approved B2 design](pesatrack-2-b2-design.md).

## Global constraints

- One isolated branch, one reviewable PR; batch related edits before PR CI.
- Local-first Android; minimum API 24; synthetic financial fixtures only.
- Two distinct explicitly confirmed owned accounts and correlating transactions are mandatory.
- Exactly three distinct ordered normalized names are strong identity evidence. Two matching names always require explicit confirmation.
- No automatic name-based classification in this slice. Explicit pair confirmation remains required even for a strong identity match.
- Preserve user corrections, amounts, fees, debts, salary, references, source rows and historical balances.
- Legacy enabled/USER_OWNED/confirmed_account_id values cannot backfill explicit ownership consent.
- PDF parsing, scanned-document OCR and password support are deferred. Payslips cannot prove deposits.
- Run user-requested instrumentation through the existing manual workflow; do not modify CI triggers or signing.

## Review focus

1. Two separate same-amount credits sharing a weak reference must never become one transfer; candidate selection must require unique pairing and retain both when ambiguous.
2. A legacy account marked USER_OWNED automatically must remain unverified for B2 until a new explicit confirmation.
3. Rejecting an identity must suppress identity-assisted pair review after replay; confirming identity must not confirm an account or a transaction pair.
4. Disabling or rejecting ownership while a review card is open must cause confirmation to fail without ledger changes.
5. Restoring an older encrypted backup must produce empty consent/pair tables and unchanged old cash totals.

## Execution and baseline

Native execution is recommended: these tasks share persistence interfaces and one financial write boundary. Use one final independent whole-branch review after unit, migration, restore and requested instrumentation checks.

The current local checkout now exists at `work/PesaTrack`, clean at `d5e90b0153d77f62d8e549e199e74abf8adc37a1`. The earlier audit's clone failure is superseded by the completed checkout. Authenticated push and the local Android toolchain still need verification. The GitHub connector is available for repository mutations if CLI authentication is unavailable.

- [ ] Refresh main SHA, open PRs, Actions and all nested AGENTS.md before editing. Confirm no competing schema/feature branch appeared.
- [ ] Create `feature/pesatrack2-owned-transfer-review-b2` from that exact main. Do not modify main or dependency PR branches.
- [ ] Read `superpowers:executing-plans`, test-driven-development and its good-test guidance before implementation.
- [ ] Check JDK17, Android SDK36, Gradle wrapper and emulator availability. If local execution is unavailable, report it precisely and use CI without claiming local tests passed.

## Task 1: Ordered identity evidence

**Files:** Modify `domain/identity/SelfNameEvidencePolicy.kt`, `domain/identity/IdentityGraphPolicy.kt`; tests in `domain/identity/SelfNameEvidencePolicyTest.kt` and `IdentityGraphPolicyTest.kt` under the existing main/test source roots.

**Interfaces:** Preserve `SelfNameEvidencePolicy.compare(String?, String?): SelfNameEvidenceLevel`. Preserve the existing phone-normalization signature; accept complete Kenyan 01/07 national and +254 forms consistently.

- [ ] Write failing `reorderedThreeNamesAreNotStrong`: James Mwangi Waweru versus Waweru James Mwangi must not return EXACT_THREE_NAMES. Assert ordinary case/whitespace variants do return EXACT_THREE_NAMES, and James Waweru returns TWO_OF_THREE_REVIEW.
- [ ] Add `invalidTokensAreNotDiscardedIntoAnExactMatch`: extra numeric, one-letter and punctuation-bearing unsupported tokens cannot disappear to manufacture a valid three-token match. Cover repeated, blank, two-token profile, substring and extra-name inputs. Use Locale.ROOT normalization and a documented deterministic supported alphabet; unsupported names remain reviewable through ordinary identity review rather than being guessed.
- [ ] Add complete 01/07/+254 phone equivalence and masked/suffix/malformed-phone rejection tests.
- [ ] Run the affected JVM tests and capture the expected assertion failures before replacing set-equality logic with ordered token comparison. Two-of-three remains review-only, including reordered partial overlap.
- [ ] Run the affected suites after implementation; verify no identity policy invokes ledger writes.

## Task 2: Ownership provenance, schema and restore

**Files:** Create `data/local/AccountOwnershipConsentStore.kt`; modify `PesaTrackDatabase.kt`, `FinancialUnderstandingStore.kt`, `security/PesaBackupManager.kt`; ownership UI in `MoneySetupScreen.kt` and `FinancialInstitutionsScreen.kt`. Add `AccountOwnershipConsentStoreTest.kt` and `B2OwnershipMigrationTest.kt` under androidTest; extend `BackupRecoveryIntegrationTest.kt`.

**Interfaces:** `recordConsent(accountId: String, owned: Boolean): Long` returns the monotonically increasing revision. `confirmedRevisions(): Map<String, Long>` returns only explicitly confirmed, currently enabled accounts. Internal helpers take the caller's SQLiteDatabase so provenance and the existing ownership update can share one transaction.

**Schema:** Recheck the version immediately before implementation; v38 if main still uses v37. Create `account_ownership_consents(account_id TEXT PRIMARY KEY, decision TEXT NOT NULL CHECK(decision IN ('CONFIRMED','REJECTED')), revision INTEGER NOT NULL CHECK(revision > 0), updated_at_epoch_ms INTEGER NOT NULL)`. Validate account existence in the write boundary. Do not rely on globally enabled foreign-key enforcement without inspecting the current database contract. Create the pair table in Task 3 in the same migration. No backfill.

- [ ] Write `legacyEnabledAndAutomaticallyOwnedAccountsHaveNoB2Consent`: seed v37 money accounts and automatically populated financial entities, upgrade, assert consent table is empty and existing rows/totals are unchanged.
- [ ] Write explicit confirmation/rejection/reconfirmation tests: record revisions 1/2/3; confirmedRevisions excludes rejected or disabled accounts. A name/phone identity confirmation cannot create account consent.
- [ ] Wire only explicit `confirmEntityOwned`, `setInstitutionOwned`, `confirmProviderOwned` and manual Add my account UI actions to consent writes. Audit their callers first: no automatic/replay path may obtain provenance through those methods. `AccountStore.save/findOrCreate` alone must not write consent.
- [ ] Wire explicit Not mine to rejection. Add an existing-account confirmation control so upgraded users can supply missing consent; an informational unverified state must not hide their history or balances.
- [ ] In migration tests compare populated transactions, salary, debt, user edits, accounts and totals before/after; ensure schema version assertions in existing migration suites are updated intentionally.
- [ ] Extend backup compatibility from supported v34-v37 to v38 by introducing missing tables as empty only when appropriate. Validate unexpected tables in older payloads. Test v34, v36, v37 and v38 restore, preserving current rollback/recovery validation.
- [ ] Run ownership/migration/restore instrumentation and existing migration/security suites. Failure leaves B2 unavailable and blocks merge.

## Task 3: Conservative pair policy and persistent review

**Files:** Create `domain/evidence/OwnedTransferReviewPolicy.kt`, `data/local/OwnedTransferReviewStore.kt`; modify `TransactionStore.kt` only for a narrow atomic classification helper. Tests: `OwnedTransferReviewPolicyTest.kt` (JVM), `OwnedTransferReviewStoreTest.kt` (Android).

**Interfaces:** Define immutable `TransferReviewObservation` containing stable transaction ID, account ID, amountMinor, currency, event epoch, direction, protected state, references and counterparty hints. `OwnedTransferReviewPolicy.candidates(observations, confirmedRevisions, profileName, profilePhone)` returns `TransferReviewCandidate` with two observation snapshots, two ownership revisions, identity evidence and a stable pair key. `OwnedTransferReviewStore.pendingCandidates(limit: Int = 20)` returns bounded cards; `decide(candidate, decision)` returns APPLIED, STALE or REJECTED without throwing partial ledger changes.

**Pair schema:** `owned_transfer_review_decisions(pair_key TEXT PRIMARY KEY, source_transaction_id INTEGER NOT NULL, destination_transaction_id INTEGER NOT NULL, source_snapshot_hash TEXT NOT NULL, destination_snapshot_hash TEXT NOT NULL, source_ownership_revision INTEGER NOT NULL, destination_ownership_revision INTEGER NOT NULL, decision TEXT NOT NULL CHECK(decision IN ('CONFIRMED','REJECTED','DEFERRED')), updated_at_epoch_ms INTEGER NOT NULL)`. Use sorted stable transaction IDs in the key and direction-aware fields in the snapshot. No raw SMS/document blobs. Include this table in Task 2's migration/restore tests.

- [ ] Write positive candidate test: confirmed bank expense and confirmed wallet income, KES 10,000 minor units, same nonblank independent reference, times within 48 hours, ordered three-name match. Result is a review candidate, not an authorized mutation.
- [ ] Test missing consent, same account, nonpositive amount, unequal amount/currency, time beyond 48 hours, missing/conflicting references, protected USER_CLASSIFIED/EXCLUDED/rule-based rows, reversal/refund/debt/receivable/salary settlement relationships and unsupported source claims. These must not produce a pair.
- [ ] Test ambiguous twins and overlapping candidates using all eligible rows in the bounded period, including rows without an identity hint: every transaction must have exactly one compatible counterpart. Never select the nearest row merely to break a tie.
- [ ] Test names and complete phone hints independently; two-name identity remains explicitly review-only. Existing rejected identity decisions suppress identity-assisted candidates. No new automatic classifier is called.
- [ ] Build deterministic snapshot hashes from all facts used for eligibility, including references, source provenance, review state and ownership revisions. Rejected pairs stay suppressed on replay. Deferred pairs stay suppressed until relevant facts change. Confirmation cannot be inferred from a persisted name decision.
- [ ] Write stale-card tests: change amount, direction, reference, user correction, account enablement or consent after preview. decide returns STALE; snapshots of ledger and decisions remain unchanged.
- [ ] Implement confirmation under CanonicalStoreLock plus one SQLite transaction. Reload both rows and consent, recompute uniqueness and eligibility, and reject changed snapshots. Preserve both source rows and references: use the existing canonical ledger semantics to classify one canonical source-to-destination TRANSFER and mark the corroborating incoming observation as EXCLUDED only after proving it is the same movement. Do not delete either row or alter fees/amounts. Set canonical state to USER_CLASSIFIED so replay cannot overwrite it. Persist the pair decision atomically.
- [ ] Before writing this helper, inspect all existing mirror/exclusion and reporting behavior. If a protected relationship or separate-cash interpretation prevents safe reuse, leave the candidate pending and do not substitute `resolvePossibleDuplicate` or broad reconciliation calls.
- [ ] Test rollback on invalid persistence, idempotent repeated confirmation, confirmation replay, ownership reversal, source deletion and encrypted restore. Assert zero additional income/spending for the confirmed pair, preserved fee cost and correct source/destination balance movement. Confirm unrelated twins retain their original totals.
- [ ] Rebuild pay-cycle projections and emit LedgerUpdateBus only after a successful commit. Do not create a second ledger or general evidence-import engine in this slice.

## Task 4: Activity review and import explanations

**Files:** Create `ui/screens/OwnedTransferReviewComponents.kt`; modify `ActivityScreen.kt`, `IdentityReviewScreen.kt`, `IdentityGraphStore.kt`, `StatementImportScreens.kt`. Add `OwnedTransferReviewJourneyTest.kt` and relevant identity-review instrumentation.

**Interfaces:** Review cards consume TransferReviewCandidate, call store.decide asynchronously, reload after APPLIED/STALE, and prevent duplicate submissions. No changes to the five-tab navigation.

- [ ] Write UI tests for source/destination/date/amount/reason display and explicit Confirm transfer / Reject match / Not now actions. Two-name cases require tapping Confirm; displaying the card or pressing identity Yes cannot post a transfer.
- [ ] Explain identity confirmation separately: Yes, this is me / No, not me. Display prompt.reason so strong versus partial evidence is visible rather than hidden by generic copy.
- [ ] Give name and phone review the same current account/transaction safety checks. Do not rely solely on stale identity_observations; join current transaction facts and the new consent records. Existing ordinary identity-label personalization may remain available, but must not imply a proven transfer.
- [ ] Test saving/error/stale states, UI refresh, rejection after restart, large-font layout and no account creation from identity controls.
- [ ] Improve CSV instructions with supported columns, signs, dates, UTF-8 and limits. Clearly state: PDF import is not available yet; text-based, scanned and password-protected PDFs need separate support. Payslip net pay is not proof of cash received.
- [ ] Preserve current CSV duplicate preview, unchecked possible matches and review-required import. No PDF picker option or parser dependency.

## Task 5: Combined validation, review, tracker and merge

**Files:** Update `docs/plans/pesatrack-2-implementation-tracker.md`, `CURRENT_STATE.md`, and add `docs/plans/pesatrack-b2-acceptance.md`. Carry the approved spec and this plan into repository documentation on the same branch after plan review.

- [ ] Update stale top-level tracker headings and master issue #322. Mark B2 pending validation, explicitly describe pair consent/review versus existing identity evidence, and retain C/D/E/release gaps. Record PDF research as a deferred adapter with API24 compatibility work.
- [ ] Run `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin --stacktrace` plus existing synthetic financial/Python contracts. Capture failures by name; do not equate source compilation with emulator execution.
- [ ] Run user-requested `:app:connectedDebugAndroidTest` through the existing Android CI manual review input if local emulator execution is unavailable. Verify actual successful execution of new ownership/pair/migration/restore/UI suites and existing financial regression suites, not a skipped job.
- [ ] Review the full branch for invariant violations, privacy, consent provenance, restore shape and history correctness. Obtain an independent whole-branch code review using the approved execution method. Resolve findings before merge.
- [ ] Create one PR after batching product changes and tests. Attach its URL to this chat. Verify the exact reviewed head SHA, every required CI result and the debug APK artifact. Further CI cycles are allowed only to resolve failures or review changes.
- [ ] Merge with the exact-head guard after green checks and review, as already authorized by the user. Verify separate main CI, rolling APK provenance and artifact metadata. Record physical Redmi acceptance as unverified unless actually performed.
- [ ] Post final evidence to issue #322 and link the tracker. Report exactly what the app contains, what remains planned and any acceptance gates not met.

## Plan self-review

- Scope matches the approved ownership/correlation design and intentionally defers PDF dependencies and general payroll/evidence-import schemas.
- Ownership and pair tables share one additive migration; restore compatibility and consent provenance are covered before ledger wiring.
- Names and phones can only strengthen candidate review. No plan step permits transfer classification from names alone.
- Atomic confirmation includes source snapshots, uniqueness, revisions and protected relationships. One source row can be excluded only as a proven mirror, never merely because amounts/names overlap.
- Every review-focus risk has an explicit JVM or instrumentation assertion. Physical device acceptance remains separately reported.
- Pending prerequisite: human review of this plan and execution-method selection under the installed writing-plans workflow. No implementation changes have been made.

## Execution rulings and review corrections
- User authorized direct native execution and one combined workflow without further gate questions.
- Reviewed transactions must have identical recorded movement timestamps. The original bounded-time proposal cannot safely preserve staggered settlements in the current single-timestamp canonical projection; such observations remain independent for a future settlement-aware phase.
- Generic shared references are insufficient. A primary transaction code must be observed on the complementary side; distinct bank/wallet primary codes are allowed with that cross-reference.
- Native balance evidence is copied atomically to account_balance_observations before exclusion, retaining raw transaction balance fields.
- The explicitly requested B2 emulator tests run alongside build in the same PR workflow using a narrowly scoped branch condition, rather than a second manual workflow.
