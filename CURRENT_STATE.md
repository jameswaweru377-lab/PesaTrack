# PesaTrack Current State

_Last refreshed: 1 November 2026_

_Merged-branch cleanup (PR #552; 1 November 2026):_
A separate monthly workflow proposes deletion only for unprotected, non-default
branches whose current tip exactly matches a same-repository PR merged into the
current default branch and which have no open same-repository PR. Manual dispatch
previews by default; only the repository owner on the default branch can request
manual apply. The apply job alone receives `contents: write` and rechecks each
branch immediately before deleting its ref. No live branch has been deleted yet;
the first preview and owner-authorized cleanup remain pending merge and green
exact-head CI. Each run caps work at 170 candidates, paces rechecks, and reports
any excess as deferred to stay within the documented GitHub API request budget.
This automation does not infer active ChatGPT chats or verify Android-device
acceptance.

_A2 bill-cadence domain dependency (1 November 2026):_
The voice-planning domain parser now uses a domain-owned `BillCadence` enum;
the scheduling layer keeps a Kotlin source-compatible alias for existing
callers. Saved schedule preferences still use `MONTHLY` and `WEEKLY` names.
This is a code-boundary change only: no new schedule behavior, screen,
database version, permission or network flow. A pure domain compatibility
regression is checked by PR #546 Android CI; local Gradle tests could not
resolve the Android Gradle plugin offline. No physical-device PASS is claimed.

_Temporary unlocked-screen capture support (1 November 2026; CI and Redmi acceptance pending):_
The owner requested Android screenshots and screen recordings for private UI review.
An enabled app lock no longer blocks capture while the app is unlocked and in
the foreground. The PIN/biometric lock screen, an app-lock-protected background
window and the backup-recovery blocker retain `FLAG_SECURE`. App-lock credentials,
biometric checks, backup/restore, financial data and permissions are unchanged.
The full-screen Statement Import, Smart Notice and receivables-review dialogs
update their own window protection when app-lock-protected PesaTrack pauses;
the sensitive backup/restore dialog remains screenshot-protected throughout.
Screenshots may reveal real financial information; redact before sharing.
This temporary relaxation needs real-phone acceptance and can be reverted after
UI review.

_Statement account visibility and supported-reader routing (verification pending):_
The account inventory includes saved limited sources and accounts with reports
in other years; year-scoped totals remain separate. Bank PDF identification
tries existing supported local readers before the read-only unfamiliar preview.
A supported candidate still requires account admission, explicit bank
confirmation and the normal source review before saving. Incomplete documents
are never admitted by the fallback. This change requires CI and real-device
acceptance; graphical issuer evidence is not authentication.

_Money until next salary (Task 3; draft PR #507, Android CI and device acceptance pending):_
Plan has a separate local-only forecast card, using either a **user-confirmed
next payday and expected amount** or the existing **estimated usual salary
window**, clearly labelled. Future salary is never added to current cash.
A projected low–high pre-payday remainder is offered only with fresh and
complete spendable account evidence (or an explicit user-confirmed total),
three fully reviewed completed salary cycles, trusted variable expenditure
and fees, and confirmed bills and commitments. Active loan/Fuliza repayments
require an explicit user-confirmed amount due before payday; uncertain
amounts/dates, missing balances and thin history stop the numerical forecast.
Manual amounts expire after 24 hours, after a salary-cycle change or after
ledger changes. The approved Plan design, original Safe today calculation,
salary receipts, categorization, statements, debt ledger, database schema,
permissions and network behavior are not changed. The expected amount is
informational only, not cash. Synthetic JVM policy tests, Android CI and
Redmi acceptance must be reported separately; this is not yet verified on
a phone.

_Targeted salary accuracy audit (31 October 2026):_
The Salary setup page defaults to day 24–28, which may wrongly send genuine
29th–1st receipts into pay-cycle review for users with a later pay window;
the owner may adjust this in Salary Source, and changing the default or
classification policy needs separate consent and tests. A December salary
paid around the 21st is likewise outside the usual window and correctly
needs explicit user review under existing policy. Multiple enabled salary
rules are accepted by `PayCycleStore` while the Salary screen displays only
the newest rule; this requires a separate authoritative-source review.
Existing Safe today is derived from tracked pay-cycle funds and average cycle
length, not a directly observed multi-account available balance, so it must
not be advertised as a verified wallet amount. Debt balances alone lack a
next repayment date and instalment amount. These existing calculations are
left unchanged while their compatibility is evaluated separately.

_Recurring-pattern v2 domain proposal (31 October 2026; draft PR, device acceptance pending):_
An opt-in, pure Kotlin `RecurringPatternPolicy.detectV2` evaluates local
observations for weekly, fortnightly, fixed day of month (±2 days), month-end
and approximately four-month termly patterns. It tolerates one missed cadence
with enough other evidence, keeps the existing 35% amount guard and HIGH/MEDIUM
confidence tiers, anchors the next date to the calendar, and includes an
explainable reason for every candidate. Invented JVM fixtures cover leap years,
skipped events, inadequate history, volatile amounts and irregular shop visits.
This code is **not connected to live suggestions**: the current bill setup
cannot accurately represent fortnightly or termly commitments and would
otherwise risk displaying them as monthly. The existing detector, bill setup,
transaction ledger and categorization remain unchanged. Connecting v2 to the
app requires separately approved schedule/UI handling and device verification;
this change adds no permission, telemetry or network access. See
`docs/superpowers/specs/2026-10-31-recurring-pattern-v2-design.md`.


_Default offline categorization exam (31 October 2026; baseline verified in JVM CI):_
A fixed 600-case invented Kenyan corpus now scores the existing built-in
recognizer and extracted unchanged TransactionStore fallback, without user
SmartRules or cached remote hints. The baseline records correct, wrong,
explicit Needs Review and uncategorized-without-review separately, overall
and across ten transaction kinds. The initial frozen snapshot is 85 correct,
20 wrong, 135 needs review and 360 uncategorized-without-review; Android CI
run 41177373457 confirmed the frozen baseline with 1,316 JVM tests, zero
failures and one skipped test. The JVM exam rejects corpus drift and a
wrong-answer rate above the frozen ceiling of 20/600. No categorization logic,
ledger, permission, network, schema or UI behaviour was changed. See
`docs/CATEGORIZATION_EXAM_BASELINE.md` for the exact scope and limitations.

_Premium Profile and internal pages (31 October 2026; feature branch, CI/device checks pending):_
The six approved Profile design boards are being translated into the existing
Compose routes: identity and local photo details, Better money habits,
app-lock/security, reminders, Appearance/privacy, help/troubleshooting and
About. Better money habits reads the currently loaded, same-revision Home
snapshot and uses the existing local financial-signal evaluator; absent or
stale evidence shows an explicit insufficient-information state, never an
invented positive result. Personal details now retain the form after a
successful local save and show a brief confirmation; failures keep the edits.
Existing encrypted backup/restore, app-lock internals, ledger decisions and
four-tab navigation are unchanged. Phone-only readability, Android settings
actions and CI results remain to be verified; no new import-notification
service or global motion toggle is claimed.

_Profile visual-accuracy follow-up (31 October 2026; awaiting CI and device review):_
The initial six-board Profile PR #503 merged, but a phone review found important
visual mismatches. This separate correction restores the illustrated violet
identity/security banners, green leaf and status chip on Better money habits,
green/coral health-state cards, tinted grouped rows, dedicated notification
and privacy hierarchy, four-tab Appearance preview, guided help steps, the
centered About identity, and an inline green Profile saved confirmation.
It retains the same local health evaluator and the original PIN, biometrics,
backup, settings and reminder stores. The design-only mockup's import-update
toggle and whole-app reduce-motion toggle are not represented as working
toggles until their app-wide behavior is implemented and verified.

_Native unfamiliar-statement preview Slice B1 (31 October 2026; draft PR #495, CI/device acceptance pending):_
The isolated Android 35+ PDF text service is wired to a **read-only** unfamiliar
bank PDF preview. A selected bank account can inspect all pages with complete
native coordinate evidence; missing pages/positions, scanned documents and
unreconciled movements are refused without saving. A known Equity layout error
can enter this preview only for an explicit, typed layout failure. Issuer,
account, password and financial-proof refusals cannot be downgraded. The
provisional preview is bound to the selected account/file and has **no Save
button**. A separate pure admission policy requires independent issuer,
full-account, currency, complete-region and printed-total proof, then reuses
the existing bank admission gate. No new issuer has that proof yet, so this
draft does **not** automatically archive unfamiliar banks, classify money,
alter Activity, add OCR, expand device support or change backup schemas.
No CI, device or first-bank pilot success is claimed by this document.

_Universal statement discovery Slice A (31 October 2026; isolated PR, CI/device acceptance pending):_
A new pure Kotlin, read-only probe can provisionally reconstruct unfamiliar
selectable-text tables with either explicit Signed Amount and Balance columns
or the existing independently headed money layouts. It checks bounded complete
page sequences, physical column ownership and the entire adjacent running-
balance chain before returning *untrusted candidate evidence*. Unsupported
layouts, unreadable pages, unverified movements and incomplete files never
produce an importable record. A separate typed fallback rule permits a
read-only probe only after an explicit unsupported-layout result, not after
issuer, account, password or financial-verification refusals. Slice A alone did
not connect to the live picker or prove issuer, account ownership or complete
document totals. It does not persist Statements, alter Activity,
import arbitrary banks, learn formats, perform OCR on scans or send financial
data to the cloud. Invented synthetic unit tests cover multi-page layouts and
fail-closed cases; end-to-end and physical Redmi acceptance remain future work.

_Limited bank source visibility follow-up (31 October 2026; CI/device retest pending):_
After a historical Equity PDF passed local review, the save action closed
the import sheet but the Statements landing screen indexed only fully verified
bank periods. A saved limited-assurance source therefore looked absent.
The Statements index now includes those accounts as explicitly limited
source records without adding their amounts to verified cash flow, Activity,
or unified coverage. Saving a bank PDF also returns to its account's saved
source view; limited-only banks have a dedicated read-only document list.
This is a presentation/navigation fix, not a ledger or backup schema change.
The reported Redmi save/return journey still requires a fresh device retest.

_Historical Equity PDF reader (31 October 2026; CI and device acceptance pending):_
The graphical-logo, borderless Equity statement uses a separate narrowly
recognized offline Python extraction profile. The private 14-page sample
produces 200 dated rows; 199 adjacent running-balance checks and the printed
grand credit/debit/closing totals reconcile. It does not print an independent
opening balance or per-page totals, so it **cannot** receive the fully verified
bank-evidence status. With explicit user review it can be saved to an additive
version-6 *limited-assurance* archive, isolated from verified bank reports,
Activity, inferred categories and automatic matching. Encrypted backup/restore
includes that separate evidence; older version-3/4/5 statement payloads retain
their respective schemas. The original private PDF stays out of source control
and CI, which use invented synthetic fixtures. Python contract tests pass
locally; Kotlin CI, device migration, backup restoration and Android acceptance
are not yet claimed at this point.

_Equity grand-total reconciled source reports (31 October 2026; Android CI and device checks pending):_
An admitted historical Equity PDF already stores its printed-grand-total-checked
Money In/Out and immutable cash rows. The limited source view now offers
**per-document** Money In, Money Out, net movement and Nairobi-calendar-month
breakdowns, after rechecking row count, source identities, row order and sums
against the saved source totals in both document lists and detail views.
A corrupt or incomplete saved row set withholds cash amounts.
Both limited-only and mixed FULL/limited account views show these individual
source records. Different imported documents are never combined into account-wide
figures, because their date coverage could overlap. These are gross cash
movements, not categorized income or spending. The saved format still lacks an
independently printed opening balance and per-page totals and is **not**
promoted to fully verified bank evidence, canonical Activity, verified reports
or automatic matching. The separate archive, local safeguards, database schema
and backup format are unchanged. Kotlin Android CI, emulator and physical Redmi
acceptance are not yet claimed by this note.

_Profile refinement (30 October 2026; branch review, CI and device checks pending):_
The approved Profile hierarchy retains the purple hero and local-only identity while
removing duplicate financial mini-stats, Financial overview and More settings.
Personal information now offers a bounded local photo-picker/crop/replace/remove
flow with emoji fallback; names and optional emails keep their existing local
preferences. Account security exposes PIN management, an opt-out for biometric
unlock and an explicit 0/30/60-second background lock interval, preserving the
30-second default and protection for locked/background screens. Reminders retain their existing
stores with higher-contrast accessible switches. Help now offers searchable
topics and read-only permission checks alongside the existing diagnostics route.
Pure Kotlin crop/timing smoke checks pass; Android CI and manual photo/lock/large-
text validation are not yet claimed. No statement, budget, backup or ledger logic
was changed in this refinement.

_Conditional PDF password prompt (30 October 2026; pending CI/device verification):_
Statements Import now first attempts local M-Pesa or bank PDF verification without
an opening password. Only an explicitly recognized PDFMiner password failure
opens the existing masked password prompt, which permits
retry after an incorrect entry; unprotected files skip the prompt. Both
extractors classify the underlying typed PDFMiner error even when wrapped by
pdfplumber, without revealing source exception text. Original account-ownership,
whole-statement reconciliation and user-review gates remain unchanged; no
password is stored or sent online. The synthetic Python tests cover real
encrypted and unencrypted PDFs, but live Redmi phone acceptance is pending.

_Bank PDF account setup UX (30 October 2026; pending PR CI/device verification):_
Statement Import now identifies a missing/incomplete bank account number before PDF
selection and lets the user provide the complete number to their existing selected
local owned account. Stable account-identity refusal codes have private-data-safe,
actionable messages; the wider institution-picker copy distinguishes optional
transfer hints from the full number required for verified bank PDFs. The
document-side admission rule, source ownership checks, archived Statements,
Activity transactions and database schema are unchanged. Phone acceptance is
pending; no private account values are retained in repository fixtures.

_Statements ↔ Activity evidence milestone (30 October 2026; pending PR CI/device verification):_
The existing imported-statement coverage, cash overview and canonical Activity
ledger remain independent. A new opt-in read-only correlation snapshot can now
compare strictly original SMS/notification cash observations against independently
archived M-Pesa and bank statement legs. It groups verified copies of the same
account cash leg before proposing an exact-reference, opposite-leg own-account
**review candidate**, not a confirmed transfer classification. Conflicting
same-reference amounts in independent PDFs, protected Activity evidence,
missing bank references and unverified source totals veto those proposals;
none authorizes an Activity mutation.
Statement-import and manual ledger entries cannot circularly corroborate their
own PDF. This does not add a UI review action, persist links, synchronize bank
categories or build final combined Insights totals. Real-device acceptance and
Android CI verification must be recorded separately.

_Targeted security update (29 October 2026, pending CI and device verification):_
App-lock background grace and PIN lockout now use boot-scoped monotonic elapsed time,
not the user-adjustable wall clock. A prior-boot background marker requires unlock;
a prior-boot or legacy wall-clock PIN lockout restarts with a bounded 60-second delay
when repeated failures were recorded. PIN hashing, backup format, permissions and
financial data are unchanged. The focused JVM timing-policy tests pass locally;
Android build and physical-device behavior remain unverified for this change.

_Audit S5 descriptor privacy hardening (29 October 2026, separate review branch):_
The Android and Worker sanitation policies now distinguish 10-character
transaction references containing digits from ordinary 10-letter business
words, fail closed for any digit or non-ASCII character before normalization,
and accept only explicitly recognized public labels or narrowly business-
typed suffixes. Unknown single-word and ambiguous generic names are rejected;
a business suffix still cannot conclusively prove an organization is public.
One synthetic
corpus is exercised by Worker and Android JVM tests; this does not grant new
online channels or permissions, alter ledger data, or change the deployed
Worker until separate deployment approval. Worker rate-limit finding S4 and
its shared-mobile-IP tradeoff remain unresolved. Android CI and physical
device results must be recorded before accepting this branch.

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
- Unmerged work on `codex/backend-accuracy-repairs-20261020` adds an explicit route from a fully verified M-Pesa PDF to the existing Activity review/import flow. Informational rows stay in the statement archive; cash movements remain unclassified and subject to account/date selection, duplicate/possible-match review and the existing atomic write. On 20 October, the local Python parser passed the private sample; however, Android PDFBox on `Codex_Pixel_6_API_37` stayed on “Checking your document and transactions…” for over ten minutes of process CPU, with no ANR or review result. It was stopped before confirmation; Statement Activity remained empty. No financial values or identifiers were retained. This is a reproduced device-emulator latency failure, and the PDF-to-Activity route has not passed an Android review journey.
- Statement duplicate preview now uses a pre-indexed history window keyed by source reference and by account/amount/direction/local day; ambiguous equal-amount/day rows are still only possible matches. Bill-payment matching also avoids using a single unclassified payment to satisfy multiple bill schedules. A side-by-side `.local` debug package on `Codex_Pixel_6_API_37` scanned 31 SMS in the selected 30-day range; Activity showed 19 transactions after the first import and the same count after reimport. Synthetic M-Pesa examples appeared, including one uncategorized inflow; a KCB credit appeared as source-unknown and needs review because no KCB account was set up. The I&M synthetic message was not found in the October Activity list and remains unresolved. Full JVM tests pass for the current worktree; Android test sources compile. Physical Redmi acceptance remains unverified.
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

### PDF latency and I&M message investigation

- The Statements M-Pesa PDF screen uses the bundled offline `pdfplumber` parser, separate from the API 35 isolated PDF text reader. The physical-row path was repeatedly cropping the full page for every cell. It now assigns page characters to the physical grid once, retains the existing source-cell ownership and reconciliation checks, and falls back to the crop path for unusual layouts. A local host run using the supplied statement completed with the same full-verification result in 7.7 seconds versus 49.6 seconds before the change. A 120-second page-checked budget now fails closed with a fixed timeout reason; no records are saved by parsing.
- The synthetic I&M message matched the bundled I&M payment template and extracted all four expected fields. It remained pending because the test profile has no user-owned I&M account. No canonical Activity transaction was created, preserving account ownership and ledger safety. No parser change was indicated.
- A rebuilt APK could not be produced in this shell: Gradle fails before task configuration with a JDK loopback/Unix-domain socket error on both installed JDKs. The optimization is locally tested, but it has not yet been installed or verified on the emulator. The existing `.local` app was reopened; its installed code predates this optimization.
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
3. `publish`: after successful build checks on a push to `main`, build a release-signed APK with the existing protected signing secrets and replace the `ci-apk` attachments with only `PesaTrack-Full.apk` and `PesaTrack-Update.apk`. These are identical signed APKs; signing/version compatibility checks fail closed. Missing secrets block publication. Manual review/build runs retain artifacts but cannot replace the rolling release. Physical-phone upgrade compatibility remains unverified.

R1 external-release device acceptance is **NOT RUN** for the current signed
candidate. `docs/acceptance/R1_DEVICE_ACCEPTANCE_TEMPLATE.md` now defines
repeatable M-Pesa PDF, limited Equity PDF, backup/restore, app-lock and signed
upgrade checks with explicit PASS/FAIL/NOT RUN outcomes. This is an acceptance
**plan**, not Android instrumentation or actual Redmi device evidence; the
rolling `ci-apk` channel remains a development preview.

N2 statement-feature admission is **ON HOLD** until R1 has actual signed-SHA
physical-phone PASS evidence and T1 has executed exact-candidate-SHA Android
persistence PASS evidence (migrations, backup, ingestion atomicity, replay,
debt and money aggregates). Existing supported import correctness and privacy
fixes can still proceed with their own tests and reviews; new sources,
assurance promotion and automatic statement-derived financial behavior cannot
be declared ready. The hold is a review policy, not an automated PR check.
See `docs/acceptance/N2_STATEMENT_FEATURE_GATE.md` and tracker issue #537.

PR updates and merges run build checks automatically. Marking a PR ready does not start another run. Android 7 security and API compatibility emulator suites are manual review tools; the former weekly compatibility schedule is disabled. Manual signed releases also default to build-only validation. Emulator validation is opt-in through run_instrumentation; when explicitly enabled, it must pass before signing. Agents must not start instrumentation based on an older plan without a current user request.

The build/test jobs require only read access to repository contents. Release write permission is scoped to the publish job.

## Phase 3C — Statement category boundary (29 October 2026)

M-Pesa Statements continue using the existing **exact receipt, account, amount
and bounded date** verification for approved Activity category projection. An
explicit one-off Statement correction has precedence; the reverse
`Remember in Activity + Statements` action requires its own verified receipt
and user consent. Bank statements such as Equity do **not** acquire these links
from matching names, amounts or dates alone.

The Equity/bank document view supports reversible **source-row-only** labels
in the existing `statement_category_labels_v2` table (expense labels for debits,
income labels for credits). They are annotations, not proven financial event
types. Bank PDF legs, cash totals, Activity, debts, source overlap and other
imported files remain unchanged. Missing bank references mean a full automatic
bank-to-Activity category bridge is deliberately **not yet available**. The
independent cross-product Insights report remains a future phase. See
`docs/superpowers/plans/2026-10-29-statements-phase3c-category-bridge.md`.

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
- Release builds accept explicit version code/name inputs; a protected release identity and signed rolling APK already exist. Signing continuity and actual physical-phone upgrade acceptance remain release gates, independently of a successful workflow.
- Historical SMS import reports scan/match/insert progress, supports cooperative cancellation, and retains a short-lived checkpoint so interrupted imports can resume by SMS date/id without rescanning the completed portion. Activity review/duplicate work is now bounded to unresolved/recent rows, duplicate matching stops at the policy time window, and month/account lookups use v33 ledger indexes. Physical-device large-ledger measurement still remains.

## Release status

Not yet public-store ready. Remaining release blockers include:
- protect `main` with required CI checks;
- clean stale branches;
- verify signing-secret environment protection and independently recoverable offline signing-key custody; the Gradle wrapper and rolling signed APK already exist;
- obtain an actual PASS on the final-candidate-SHA R1 physical-device acceptance sheet and exact-SHA emulator persistence checks;
- publish the privacy policy at a stable HTTPS URL and complete Google Play READ_SMS / Financial features declarations;
- Redmi/Xiaomi real-history acceptance;
- performance hardening for long imports/large ledgers;
- architecture decomposition of the largest stores/screens.

## Architecture rule for next work

Improve architecture incrementally during approved feature and fix work: extract focused responsibilities only in related code, preserve financial behavior and tests, and avoid whole-app rewrites. Preserve the four visible bottom tabs Home / Activity / Plan / More (Accounts remains an internal destination), and keep parser expansion evidence-driven.

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
