# PesaTrack Current State

_Last refreshed: 18 October 2026_

This file is the short engineering source of truth. Historical audits, design explorations and `PROJECT_CONTEXT.md` are useful background, but they must not override current code or this snapshot when they conflict.

## Product status

PesaTrack is feature-complete enough for private/internal beta use, but it is still in a release-hardening phase before external/public distribution.

The current product includes the canonical ledger, provider-neutral financial interpretation, transfers/refunds/debts, salary cycles, budgets, saving goals, bills, reports, app lock, encrypted backup/restore, historical SMS import, notification capture, Data Health and Message Lab.

## Onboarding

- SMS setup offers one- and two-completed-month presets (each also includes this month to date) and an inclusive custom from/to date range. Custom selection is not capped at three months; both boundaries are enforced by the local SMS query. SMS consent shows the chosen range and separates the later notification-access setting. Full message text remains on device; optional online business-name lookup remains separate.

## Data and accounting

- Redmi acceptance remains blocked at page 1, receipt row 1 (safe screenshot
  evidence after #381). The production failure path now preserves whitelisted
  check labels and column roles from geometry through the isolated service to
  the import screen. This distinguishes reference/time, status, description,
  split/invalid amount and column-ownership refusals without exposing cell
  contents. Document-wide reconciliation failures do not fabricate an affected
  page. Acceptance rules and ledger writes are unchanged. The actual failing
  check still requires a new private-device attempt; no extraction fix is
  claimed from the screenshot alone.

- The table-first PDFBox bridge now uses the shared V2 ordered table model,
  retaining transient cell rectangles and physical column order. Shared
  semantic header discovery replaces its private header search; generic V2
  region discovery supports bounded borderless/date-anchored synthetic tables,
  repeated headers and wrapped descriptions. Production PDFBox sends versioned
  structured cells rather than formatted transaction prose through the isolated
  service. Review rechecks receipt groups, exact money, counts and observed
  summary/opening/closing declarations after transfer. Customer metadata stays
  isolated. Native/production CSV/payslip/SMS and atomic ledger persistence are
  preserved. Generic bank discovery and the existing V2 CSV model are read-only;
  neither establishes ownership or asset/liability interpretation. Actual
  statement-period date-range validation, arbitrary footer/continuation layouts,
  provider acceptance and real-device financial validation remain release gates.
  See docs/plans/table-first-statement-import.md.

- PDFBox-Android is an independent, opt-in on-device statement review path in
  the existing isolated PDF service (#375–#377), with further production
  evidence hardening in #380. The bridge now requires physical semantic money
  headings, complete receipt/timestamp ownership, checked credit/debit cells,
  and every receipt-group balance transition. Recognized summary tables and
  labelled totals/opening/closing balances are audited across the entire
  document, including footer declarations. Original money rectangles remain
  transient local evidence; six proof-count integers disclose which boundary
  and summary checks remain unverified. The decorative detailed-section label
  may be absent only on the fully proved PDFBox route; title, period and money
  headings remain required. Existing native/CSV/payslip/SMS behavior and atomic
  import persistence remain. Import still uses the existing NEEDS_REVIEW
  classification flow; automatic transfer, fee, reversal and loan semantics
  are not completed by PDF extraction. Synthetic 26-page/881-row reconstruction
  is regression evidence, not confirmation of the private Redmi statement.
  PDFBox stays opt-in; arbitrary bank/OCR support, dependency maintenance and
  actual device memory/latency and financial acceptance remain outstanding.
  See docs/plans/pdfbox-production-evidence-gates.md and
  docs/plans/pdfbox-dependency-review.md.

- A1 authority gating now protects manual entries, received salary and selected CSV statement adapters; existing canonical duplicate/replay checks still decide insertion. Expected salary and payslip information do not authorize cash.
- Manual salary requires a current enabled receiving account. Disabling an account does not remove existing salary history.
- Import statement offers All statement dates or a custom From/To calendar range for CSV and supported M-Pesa PDF transaction candidates. Both endpoints are included; reversed/future dates and empty matches are explained. Preview excludes outside rows before duplicate checks, and file/account/date snapshots are revalidated before existing atomic imports. Source dates/amounts/references and dedup identity are preserved. Raw PDF text is not date-filtered; reviewed parsed rows use the same range/dedup pipeline. No month-count cap, schema change or instrumentation run.
- Import statement and Payroll details offer local PDF extraction on Android15/API35+, with a transient masked password retry and specific safe failure codes. Native extraction runs in a private isolated process without ledger/backup initialization. One bounded M-Pesa text-column profile offers editable transactions with explicit wallet/field confirmation, date filtering and existing atomic dedup. Unsupported/partial layouts offer no transaction candidates. Payslip suggestions include explicitly labelled employer/month and NSSF/NHIF/other known amounts, including adjacent label/value text cells. API24–34 keep CSV/manual entry; scanned OCR, arbitrary bank layouts and unsupported encryption remain unavailable. Passwords are not persisted or logged; no decrypted PDF is written. Limits: 2 MB, 40 pages, 100,000 text characters; no new library, permission or document upload.
- Document-preview service/lifecycle and physical-device behavior are not verified while instrumentation remains paused. See docs/plans/pesatrack-document-review-design.md.
- Paycheck Analysis now has a dedicated Activity/Salary/Insights route with reviewed payroll summaries.
- Payroll outflow/rounding follow-up: the interpreter recognizes punctuated statutory abbreviations, student-loan/rounding labels and an explicit Outflows section, while a separate Calculations section never creates deductions. A reported deductions subtotal may exclude a separately labelled one-cent carry-forward; strict gross/net journal reconciliation still applies. Long M-Pesa PDFs now have 8 MiB, 160 pages and 500,000 characters with ordered 12k-character isolated-service messages, 90s timeout and 5,000 reviewed transaction candidates; CSV retains a 2 MiB file cap. Physical-device PDF layout support remains unverified. See docs/plans/payroll-outflows-and-large-pdf-import.md.
- Payslip field reconciliation extends on-device semantic parsing across commonly labelled earnings, net pay, metadata, explicit deductions sections and unfamiliar safe deduction labels. Unambiguous Total Cash Earnings can suggest gross cash pay; taxable pay remains separate. The review screen auto-prefills new payroll drafts, flags incomplete deduction arithmetic and never creates a synthetic balancing deduction. Only explicit user-reviewed payroll journal fields persist; source text and personal identifiers stay discarded. See docs/plans/paycheck-semantic-reconciliation.md.
 PDF selection no longer cancels on document-picker ON_STOP. Statement PDFs are classified before M-Pesa parsing so detected payslips route to Paycheck Analysis rather than yielding statement-currency errors; full raw PDF text is no longer rendered. Native PDF size cap is 3 MiB with the existing 40-page/100,000-character/API35 caps. Earnings breakdown suggestions (basic pay, housing allowance, cash/taxable totals) are preview-only, not yet persisted. Physical-device reliability is still unverified under the instrumentation pause. See docs/plans/paycheck-analysis-pdf-import-fix.md.

- Schema: **v40**
- Phase B evidence observations/links are additive and local-only. Manual and user-selected statement rows can persist immutable hashed provenance linked to an existing canonical transaction by strict account/amount/direction/time and reference rules. Ambiguous rows remain pending and do not alter income, spending, balance, or debts. Existing SMS/notification capture still owns canonical posting; comprehensive cross-source and historical backfill remains pending. Instrumentation is still paused. See docs/plans/pesatrack-2-phase-b-evidence-journal.md.
- `PesaTrackDatabase.get(...)` is the process-scoped database owner.
- Canonical financial-message ingestion is transaction-wrapped.
- Replay/idempotency and rollback are covered by Android tests.
- User-authoritative corrections must not be silently overwritten by replay.
- Unknown/ambiguous evidence must remain uncertain rather than being guessed.

## Payroll journal

- Salary overview now offers separate Payroll details / payslip review. User-reviewed employer/month gross/net, typed employee deductions and employer contributions live in a journal; no cash, account balance, debt or pay-cycle writes.
- Checked KES arithmetic; exact replay returns the existing employer/month record; conflicting replay requires explicit edit. Full stored snapshots protect edits/deletion, including after restored revisions. Historical source labels survive.
- Optional API35 PDF suggestions use exact labelled single amounts with aliases/conflicts left manual; field correction/review is mandatory. No arbitrary table/OCR/protected/API24 support. No raw document retained/uploaded.
- Additive schema39 and encrypted34–38 restore compatibility. Journal semantics are validated before restore staging/deletion. JVM and synthetic SQLite tests are provided; device migration/restore/UI execution is unverified under the instrumentation pause.
- Settlement links, multiple payouts/payslip aggregation and remittance/contribution balances remain planned. Reported net payable is never added to cash totals.

## Offline financial laboratory

- Python is development-only: fixed invented fixtures and read-only comparison
  with actual Kotlin outputs; no device SMS or ledger import, network/model
  integration, or Python runtime shipped in the APK.
- Four contract groups cover sender/account ownership evidence, configured
  transfers, debt interpretation histories and remembered merchant identities.
  29 authored scenarios at eight monetary scales produce 232 regression cases;
  unchanged name-only cases are repeats, not independent message formats.
- Configured notification transfers require a unique sender-derived source and
  unique whole destination identifier. Body bank names and ambiguous accounts
  do not establish an own transfer. Existing shared-reference mirror matching
  remains available when complementary observations establish both sides.
- Explicit remembered merchant corrections normalize ordinary Unicode spaces,
  retaining exact-name and phone safeguards. Unknown merchant names do not
  automatically imply groceries or consent.
- Fresh-database Android histories verify lender separation, observed versus
  estimated outstanding debt, repayment replay and remembered-category consent.
  These do not diagnose a particular installed user's debt or repair previous
  account-review decisions. Voice entry is outside this change.
- Release readiness still requires current-head Android CI and physical-device
  acceptance with invented data. See `tools/python_lab/README.md` for commands,
  acceptance criteria and coverage limits.

## Remote enrichment privacy

- Raw SMS, amounts, balances, account numbers and transaction codes must never be sent to the enrichment Worker.
- Online merchant/entity lookup is **off by default**.
- A user can enable or disable online lookup from Settings.
- Only a descriptor that passes `EntityDescriptorPrivacyPolicy` may be sent.
- The Android client supplies a random install-scoped UUID solely for fair rate-limit partitioning; it is not authentication and contains no financial/device identity.
- The Worker applies both per-install/anonymous and edge-wide rate limits and fails closed if limiter bindings are unavailable.

## CI and APK publication

The Android CI workflow has three responsibilities:

1. `build`: Worker validation, unit tests, lint, debug APK assembly, then upload the APK as a workflow artifact.
2. `instrumentation`: run Android instrumentation only when explicitly requested through a manual workflow run with `run_instrumentation` enabled (default off).
3. `publish`: after successful build checks on a push to `main`, replace the rolling development `ci-apk` asset without requiring instrumentation. Manual review/build runs retain artifacts but cannot replace the rolling release.

PR updates and merges run build checks automatically. Marking a PR ready does not start another run. Android 7 security and API compatibility emulator suites are manual review tools; the former weekly compatibility schedule is disabled. Manual signed releases also default to build-only validation. Emulator validation is opt-in through run_instrumentation; when explicitly enabled, it must pass before signing. Agents must not start instrumentation based on an older plan without a current user request.

The build/test jobs require only read access to repository contents. Release write permission is scoped to the publish job.

## Tests

Current repository coverage includes JVM tests plus Android instrumentation. Important existing test areas include:
- message relevance/extraction;
- corpus health;
- duplicate/mirror and replay behavior;
- debt and salary logic;
- migration;
- canonical ingestion atomicity;
- connected UI journeys;
- security backup/restore integration.

The next test expansion should add broader migration fixtures, performance benchmarks and repeatable physical-device acceptance. API-level emulator coverage remains available through explicit manual reviews.

## Repository and release hardening

- Android CI third-party actions are pinned to immutable commit SHAs.
- CI/deployment use an exact Wrangler version.
- Dependabot watches Gradle and GitHub Actions dependencies.
- `SECURITY.md`, `CONTRIBUTING.md` and `CODEOWNERS` define review/privacy expectations.
- The repository contains a privacy-policy source and a Play release checklist.
- SMS-permission surfaces provide an explicit disclosure before requesting historical SMS access.
- Release builds accept explicit version code/name inputs and a manual signed AAB/APK workflow is defined; signing material remains outside the repository and the first signed run is still a release gate.
- Historical SMS import reports scan/match/insert progress, supports cooperative cancellation, and retains a short-lived checkpoint so interrupted imports can resume by SMS date/id without rescanning the completed portion. Activity review/duplicate work is now bounded to unresolved/recent rows, duplicate matching stops at the policy time window, and month/account lookups use v33 ledger indexes. Physical-device large-ledger measurement still remains.

## Release status

Not yet public-store ready. Remaining release blockers include:
- protect `main` with required CI checks;
- clean stale branches;
- configure signing secrets, prove the signed release workflow end-to-end, and commit the Gradle wrapper;
- publish the privacy policy at a stable HTTPS URL and complete Google Play READ_SMS / Financial features declarations;
- Redmi/Xiaomi real-history acceptance;
- performance hardening for long imports/large ledgers;
- architecture decomposition of the largest stores/screens.

## Architecture rule for next work

Do not start another broad feature or visual redesign phase before hardening/decomposition. Preserve the current Home / Activity / Plan / Accounts / More product hierarchy and keep parser expansion evidence-driven.

## Owed to me history review

### Coordinated smart notifications (PR #378)

The existing opt-in Activity reminder from #360 now shares one worker, posted notification and daily budget with material account review, new They Owe Me payment review and occasional discovery. All phone categories are opt-in; existing Activity preferences and its seven-day unchanged-evidence pause are preserved. Quiet hours, quiet summaries, snooze, persisted dismissal, foreground/import suppression, current-evidence deep links and ledger-driven cancellation prevent routine nagging. Discovery defaults to an in-app Accounts card and stops after discovery/use/dismissal.

Payment prompts require a unique locally recorded phone, lending history predating a new live notification, and the existing journal's valid-source checks. Historical SMS/catch-up and statements never produce retrospective debt prompts. Loan/repayment actions open the existing explicit confirmation dialog; Gift/Other dismiss the suggestion without changing money records. Failed/interrupted SMS imports remain suppressed until a successful consented import. No new financial schema, parser, workflow, permission, telemetry, signing or statement changes. Exact CI and APK evidence are tracked in [PR #378](https://github.com/jameswaweru377-lab/PesaTrack/pull/378); instrumentation and physical-device banner/navigation acceptance remain paused and unverified.

Personal receivables use a separate local journal. Find in transactions searches imported M-Pesa and bank history by name or phone. Loans and possible repayments require explicit user confirmation; gifts/not-loan decisions are remembered per person. Bulk selection includes only eligible displayed outgoing rows and all selected records are previewed before an atomic save. Shared bills preserve the user's remaining expense category and fees; original cash movements are never duplicated. Activity exposes Owed to me/Repayments filters and debt labels. Schema 37 adds personal_receivable_review_exclusions; encrypted backups from schema 34–36 are bridged with missing tables empty.

Implementation verification is tracked in the history-review PR. Android instrumentation and device acceptance remain unrun unless explicitly requested. Build/lint/unit evidence does not prove physical-device migration or user journeys.

### Shared-bill refinement (implementation verification pending)

Activity now separates Money lent (full-payment recipient prefill) from Shared bill (blank person and amount, explicit existing-person/local-record selection). Shared bill retains the source category and requires a positive share strictly below the payment in both UI and atomic save. Detail and person history show the person, category, original bill and remaining own share; fees remain separate. Existing category allocations retain edit/remove access, but the new Split payment action is replaced by Shared bill. Existing partial loans are not guessed to be shared bills.

Explicit shared intent uses the existing journal event_key namespace shared:UUID; kind remains LENT, schema stays 37 and generic encrypted backup preserves the key. No previous journal rows are rewritten. Source snapshots are revalidated before entry save. No new permissions, telemetry or remote financial processing. Instrumentation remains off; new integration/journey fixtures may be compiled without being executed.

## B2 ownership and Activity review

Explicit ownership consent is separate from identity and account discovery. Legacy accounts require an explicit transfer-matching choice; optional profile name and full phone number are only hints. Activity offers user-reviewed bank-to-M-Pesa pairs only with two confirmed owned accounts, complementary transactions, a transaction-code cross-reference, identical recorded movement timestamps and uniqueness. Ordered three-name evidence is strong, two names require review, and this implementation confirms neither automatically. Confirmation preserves both source observations, fees, native observed balances and authoritative corrections; stale/ambiguous pairs cannot apply.

Schema v38 adds consent and pair decisions without backfill or ledger rewrites. Older supported encrypted v34–v37 backups migrate with empty consent. CSV import remains supported; PDF extraction, OCR and encrypted PDF import remain future phases. See the implementation tracker for exact validation and remaining acceptance gates.

B2 correction safety: either confirmed entry offers Undo transfer match. The saved pre-confirm projection restores both rows atomically, marks the match rejected, and removes only balance anchors copied by that match. Individual financial edits/deletion are guarded until undo; salary-cycle IDs remain rebuildable derived grouping. Added source/mirror mutation, rule/debt/exclusion, interrupted undo, encrypted restore, unrelated balance-anchor, replay and real detail-screen cancel/undo regressions. Review approved conditional on final-head CI; execution remains pending.

B2 review update: the user reinstated the instrumentation pause. Automatic B2 PR emulator execution has been removed; manual review remains opt-in only after a new explicit request. Final-head emulator and physical-device acceptance remain incomplete, independently of build/unit/lint success.
