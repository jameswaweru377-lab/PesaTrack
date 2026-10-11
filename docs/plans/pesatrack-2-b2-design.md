# PesaTrack Phase B2: current audit and proposed implementation

Audit date: 11 October 2026. Repository: jameswaweru377-lab/PesaTrack.

## Verified baseline

- Main: d5e90b0153d77f62d8e549e199e74abf8adc37a1, merged PR #340.
- Main Android CI run 38090915307 completed successfully. This is baseline evidence, not evidence for a new change.
- Seven open PRs (#254-260) are dependency updates. No active feature PR was returned by the live open-PR query.
- Schema remains v37. Read AGENTS.md, README.md, CONTRIBUTING.md, CURRENT_STATE.md, PRODUCT_CHARTER, the implementation tracker and master issue #322 including its updates.
- A1 evidence authority and B1 matching policies exist but are not wired to a general evidence repository. A3 history selection and A4 onboarding work are merged. Manual received salary already has income/removal/reconciliation coverage; do not rewrite it based on the stale tracker heading.
- Profile name is used in Activity identity review. Existing Yes/No/Not now controls persist identity decisions, not transaction-pair transfer confirmation.
- SelfNameEvidencePolicy currently uses a set intersection: reordered three-name candidates count as exact. This does not meet the ordered-token decision in the current request.
- IdentityGraphStore name review requires two enabled accounts. Phone review does not have the same account-pair query safeguard. Neither check independently proves explicit ownership consent or a uniquely correlated pair of transactions.
- FinancialUnderstandingStore.observeEntity may set USER_OWNED and confirmed_account_id through automatic resolution. Those fields alone cannot establish explicit user confirmation. IdentityGraphStore also derives self-account nodes from enabled accounts.
- CSV import has preview, duplicate/possible-match flags and review-required rows. PDF statement extraction and payslip extraction are absent.
- Tracker top-level statuses are stale relative to appended delivery notes. Master issue body is also older than its comments.
- Local clone failed because this environment lacks authenticated Git access. The GitHub connector can read the repository. No local Android build, emulator test or APK validation was run in this audit.

## Recommended cohesive B2 design

Implement one branch and PR with identity matching, ownership provenance, pair-review persistence and import explanations. Batch edits before requesting one comprehensive PR CI run. Use the existing manual instrumentation workflow for the instrumentation tests requested by the user; do not add a new workflow.

### 1. Identity evidence

Normalize case and whitespace deterministically with Locale.ROOT. Reject unsupported/malformed tokens rather than deleting them until an apparent three-name match remains. Require exactly three distinct legal-name tokens in the configured hint. Strong evidence requires all three observed tokens in the same order. Two matching names remain review evidence and always need explicit confirmation. Reordered names never become strong evidence. Phone comparison uses complete normalized Kenyan numbers; masked or suffix-only values are insufficient. Neither name nor phone confers ownership.

### 2. Explicit ownership provenance

Add an additive, tested ownership-decision record keyed by stable account ID, with confirmed/rejected state and revision. Write it only through explicit onboarding/account actions. Automatic discovery must not create confirmation records. Legacy accounts without auditable consent remain unverified for this new automatic rule; do not backfill confirmation from enabled, USER_OWNED or confirmed_account_id. Preserve existing balances and history. Confirming a person's identity must not confirm any account.

### 3. Correlation and conservative review

Offer a bank-to-wallet pair only when two distinct enabled accounts have explicit ownership confirmation and two eligible transactions provide complementary cash directions, equal positive minor-unit amount, equal currency and bounded event time. Require strong independent reference correlation for any automatic pathway, and a unique pair. Conflicting references, repeated same-amount movements, fee-only rows, reversals, debts and user-protected corrections remain distinct or pending.

Three ordered names strengthen identity evidence; they never replace the transaction or ownership requirements. Start with explicit pair review until all persistence/replay gates are proven. Any automatic classification must use the existing canonical transfer write boundary and pass those same requirements. Do not add a second cash ledger or silently merge twins.

### 4. Review and persistence

Show source/destination, dates, amounts and the reason for the candidate. Use explicit Confirm transfer, Reject match and Not now controls, separate from Is this your name/number. Save rejection/defer decisions against the stable pair and relevant revisions. Revalidate ownership and transaction snapshots atomically before confirmation. Replay must preserve user corrections and rejection. Ownership reversal invalidates eligibility without recreating cash. Restore includes the new records and does not fabricate legacy consent.

### 5. Import explanations in the same branch

Explain supported CSV fields, preview and possible-match behavior. Explain that payslips are payroll evidence and do not prove a deposit. State that PDF import is not yet supported; distinguish born-digital text PDFs from scanned/image PDFs and encrypted/password-protected files. Do not add a file-picker option that implies unsupported extraction works.

## PDF research and separate future phase

Android PdfRenderer.Page.getTextContents is API 35. It cannot provide an API24-wide text extractor by itself. PdfBox-Android advertises API19+ support and Apache-2.0 licensing; its documented dependency is com.tom-roush:pdfbox-android:2.0.27.0. That is a candidate, not an approved dependency or evidence of safety/performance.

Keep PDF parser adoption outside the financial persistence change. Benchmark APK size, memory, malformed/protected-file behavior, cancellation and offline extraction on API24 before adoption. A future read-only adapter should produce bounded text and preview without ledger writes. Text extraction is not statement-layout understanding. Scanned PDFs need separate offline OCR, and empty extraction must be described as no usable text, not definitive proof of a scan. Encrypted files stay explicitly unsupported unless separately implemented and tested.

Sources:
- https://developer.android.com/reference/android/graphics/pdf/PdfRenderer.Page
- https://github.com/TomRoush/PdfBox-Android
- https://github.com/TomRoush/PdfBox-Android/blob/master/library/src/main/java/com/tom_roush/pdfbox/text/PDFTextStripper.java

## Acceptance and delivery gates

1. Synthetic JVM negatives: reordered/repeated/extra/malformed/blank names; two-name confirmation; complete versus masked phones; missing/reversed ownership; different currency/date/reference; ambiguous twins; payslip/planned claims; fees/debt/reversal and protected edits.
2. SQLite instrumentation: atomic pair decisions, interrupted writes, stale UI snapshots, rejection/defer replay, ownership reversal, repeat imports and no duplicate income/spending.
3. Populated v37 migration and encrypted restore: preserve accounts, salary, debts, user edits and cash totals; no inferred consent backfill; test source deletion and replay.
4. Compose instrumentation: clear confirm/reject/not-now actions, two-name confirmation, disabled stale candidates and error handling.
5. Required CI: testDebugUnitTest, lintDebug, assembleDebug, compileDebugAndroidTestKotlin and existing synthetic financial contracts. Run requested emulator instrumentation through the existing manual workflow. Verify exact head, logs and APK artifact. Physical Redmi validation remains an explicitly recorded external gate.
6. Review the complete diff, resolve financial correctness findings, merge only after green checks and review using the authorized scope, then independently verify main CI and corresponding APK.
7. Refresh tracker and issue #322 to distinguish merged app behavior, unwired contracts, deferred PDF/payroll work and unverified device acceptance.

## Alternatives

- Recommended: the ownership-provenance and pair-review design above. It directly satisfies the conservative financial constraints, but needs a tested migration.
- Smaller interim PR: ordered-name correction, symmetric phone/name review guards and accurate import explanations; retain identity-only review. It reduces risk but does not complete B2 pair persistence.
- Combining full PDF parsing, OCR and payroll with B2 is not recommended: unrelated dependencies and schemas would obscure the review and increase financial regression risk.

Status: audit and design only. No product files changed, no branch/PR created and no new build claimed. The next step is design review, then a detailed implementation plan and execution under the installed Superpowers workflow.
