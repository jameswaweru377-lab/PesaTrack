# PesaTrack Python Financial Intelligence Lab

This is an opt-in, offline, **development-only** Python experiment, deliberately separate from the production Kotlin Android application. It is not a financial-accounting authority or a replacement for Financial Intelligence V2, Message Packs, the existing SQLite ledger, or Android tests.

## Strict safety boundary

- No Android Gradle plugin downgrade, embedded Python runtime, APK dependency, Internet permission, manifest change, database migration, telemetry, or cloud service.
- No ability to access Android SMS, SQLite, real balances, accounts, transactions, or user corrections.
- Synthetic, invented, developer-authored fixtures only. Never place personal or exported SMS in this repository. No third-party processing is required.
- All proposed labels are non-authoritative and may explicitly abstain. No prediction writes ledger data.
- Existing Kotlin tests and device acceptance remain required before *any* Android integration.

## Five experiments

1. **Message interpretation research:** a provider-neutral nearest-example baseline that suggests a label or abstains. Similarity is **not** a confidence probability.
2. **Accounting QA:** synthetic validation of money, dates, duplicate IDs, transfers, refund provenance and no-movement evidence; safeguards against incorrect loan/transfer income totals.
3. **Insights:** category summaries and spending-change candidates calculated from intentionally structured synthetic events only.
4. **Learning evaluation:** reproducible leave-one-out measurement of correct, wrong and abstained predictions. No trained production ML model, dynamic personal learning or on-device inference is added.
5. **Planning forecasts:** an illustrative savings-goal estimate based on synthetic contributions, not an authoritative balance or a guaranteed outcome.

## Run it

From repository root with Python 3.11 or newer and no pip installs:

    python -m unittest discover -s tools/python_lab -p 'test_*.py' -v
    python -m tools.python_lab.financial_lab

The demonstration only loads the checked-in file at tools/python_lab/fixtures/synthetic_cases.json. There is intentionally no CLI argument that imports user SMS or data exports.

## Release gates before any future integration

- Create representative **sanitized** held-out fixtures reflecting actual provider-neutral grammar and transaction outcomes.
- Compare against the existing Kotlin engine's outputs. Measure false-positive money movements, transfers mistaken for income, duplicate bank/wallet observations, Fuliza refund and debt chains, M-Shwari savings and pay-cycle edge cases.
- Block a model if it invents a balance, creates an ungrounded financial event, overrides a user correction or raises accounting error rates.
- Assess real-device latency, memory, APK size, resource pressure and maintainability separately.
- A deliberate schema adapter, privacy review and Android tests would be required to make any future result available in the app.

## Scope of this stage

This is **a research and testing scaffold**, not five new production features. A first local evaluation may show low coverage or wrong labels; these are actionable experimental findings, not grounds for automatic deployment. Production Android and Worker code remain unchanged. The existing Android CI build job now runs lab tests and an opt-in Kotlin export/evaluation after the normal unit/lint/build checks. This costs additional CI time; no additional workflow jobs are introduced.

Reversibility: remove this folder, with no app database or APK changes.

## Read-only Kotlin comparison (next safe step)

The bridge uses **19 explicitly reviewed sanitized fixtures** from
`FinancialMessageFixtureCorpus`, with TST codes and SAMPLE counterparties.
It does not scan resource directories, accept arbitrary file arguments, read a
phone, import exported SMS, or access a ledger. The approved manifest contains
only these test messages. Extending it requires review of every new fixture.

`PythonFinancialLabExportTest` checks the manifest hashes against the Kotlin
corpus, runs the real interpreter and canonical mapping, asserts current expected
record kinds, and exports only IDs, text hashes, actual kinds and interpretation
flags. Ordinary unit tests validate the manifest but skip exporting unless
`PESATRACK_PYTHON_LAB_EXPORT=1`. Output is an ignored build artifact, never an
application asset. Python predicts from the reviewed manifest, not imported text.

From a **clean checkout**, with the repository's existing Gradle 9.5/JDK 17/Android
SDK toolchain (no plugin downgrade), run on a POSIX shell:

    python3 -m unittest discover -s tools/python_lab -p 'test_*.py' -v
    export PESATRACK_LAB_SOURCE_SHA="$(git rev-parse HEAD)"
    export PESATRACK_PYTHON_LAB_EXPORT=1
    rm -f app/build/python-lab/kotlin-output.json
    gradle :app:testDebugUnitTest --tests '*FinancialMessageInterpreterTest' --tests '*FinancialMessageFixtureCorpusTest' --tests '*PythonFinancialLabExportTest' --rerun-tasks
    python3 -m tools.python_lab.kotlin_evaluation

PowerShell: set the two environment variables via `$env:...`, using the current
`git rev-parse HEAD` value for the SHA, then run the same Python/Gradle commands.
Delete only the old generated export before rerunning. The SHA is an explicit
provenance assertion by the test runner, not a signature or proof of a clean tree.
CI derives it from the checked-out commit and regenerates the export.

### Acceptance criteria for the bridge

- Existing production Kotlin, Gradle versions, manifest, persistence and local-first
  behavior remain unchanged; existing Android checks still run.
- Kotlin validates exact fixture membership/hashes and actual kinds before export.
- Python rejects wrong schema/SHA/corpus, duplicate/missing/extra IDs, unreviewed
  hashes, unknown kinds, non-boolean flags and unexpected fields (including SMS).
- Test fixtures remain disjoint from the fixed synthetic training examples.
- Repeated evaluation is deterministic and read-only, with all 19 cases counted.
- CI runs the full Python suite, existing JVM/lint/APK checks and instrumentation,
  then exports actual Kotlin results and uploads a sanitized evidence artifact.
- Missing/invalid exports fail CI; **experimental label errors remain reported**
  and block the candidate, rather than breaking unrelated accounting tests.

### Meaning and limits of the report

The report separates correct compatible labels, wrong labels, abstentions and
unsupported cases. Coverage uses only comparable cases as its denominator.
Payment maps to expense; transfers allow transfer/savings-transfer compatibility
without verifying the savings subtype. Debt kinds remain comparable even when
persistence needs fallback. Auxiliary fees/balance/available-credit observations
are not additional principal movements. Standalone observations mean no movement
only if there is no legacy fallback. Mixed principal events remain unsupported.

**Receipt does not imply income.** Receipts (including the merchant-refund-shaped
receipt), cash movements, reversals and unresolved evidence remain unscored.
The report counts non-income cases suggested as income and no-movement cases
suggested as movement. These are label-level risk indicators, not ledger writes.
Any comparable incorrect suggestion sets `candidate_gate=blocked`; a report with
no errors still says `not_assessed_for_integration`, never production ready.

This step does **not** compare money amounts, invent balances, verify ownership,
deduplicate wallet/bank observations, validate Fuliza purchase/refund/repayment
chains, prove refund provenance, evaluate pay cycles, or replay user corrections.
Those need separate Kotlin/ledger acceptance work and representative held-out
fixtures before any integration. Do not tune on this evaluation corpus and then
claim held-out improvement.

Reversibility for the bridge: remove its Kotlin test class, lab folder and the
three added lab CI steps. No app database, runtime or APK dependency changes.
# Offline app direction (2026-10-09)

Python remains a development-only laboratory. Run `python3 -m tools.python_lab.synthetic_corpus` from the repository root before Kotlin unit tests to create 2,432 invented monetary variants of the 19 reviewed scenarios. CI requires this corpus with `PESATRACK_SYNTHETIC_CORPUS_REQUIRED=1`. The generator accepts no input arguments and writes only the ignored build directory. It preserves dates, identities and zero amounts; scaling uses decimal arithmetic. Kotlin checks authored record kinds and balances, monetary scaling and repeat interpretation. Existing Android ingestion tests plus 30 invented payment replays check actual transaction idempotency separately. Template variations do not establish new-bank coverage, economic income, automatic refund provenance or production readiness of the earlier experimental classifier.

Production changes use deterministic Kotlin calculations only: currency spacing, completed-month recorded spending comparisons and goal-linked historical savings scenarios. Existing local recurring/salary policies and explicit exact-identity category preferences are reused. No remote AI, model download, subscription, Python APK dependency, new permissions or database schema changes are added. Goal scenarios require three completed months and retain zero/withdrawal months. Their limits include missing records and withdrawals that were not linked to the goal. Budget and debt projections remain with the existing engines; this PR does not infer missing interest rates or promise repayment dates.

# Four offline financial contract groups

The development laboratory also checks 29 authored invented scenarios across
account ownership evidence, configured transfers, debt message histories, and
explicit merchant-category identity matching. Eight monetary scales produce
232 cases. Bank-name and merchant cases repeat unchanged across scales; these
are regression variations, not 232 independently researched message formats.

```sh
python3 -m tools.python_lab.financial_contracts generate
PESATRACK_SYNTHETIC_CORPUS_REQUIRED=1 PESATRACK_LAB_SOURCE_SHA="$(git rev-parse HEAD)" \
  gradle :app:testDebugUnitTest --tests '*FinancialContractLabTest'
PESATRACK_LAB_SOURCE_SHA="$(git rev-parse HEAD)" \
  python3 -m tools.python_lab.financial_contracts evaluate
```

Both commands use fixed repository/build paths. No arbitrary SMS import,
network, model, subscription or production Python runtime is added. Kotlin
independently reconstructs the inputs and expected monetary scales from the
reviewed fixture file, rejecting altered input, missing/duplicate IDs and
unsupported scales. The Python evaluator rejects stale checkout SHA,
fixture hash mismatch, incomplete histories and malformed envelopes. It compares
actual Kotlin decisions with authored answers and reports failures by group.
CI executes the contracts in the full JVM suite and retains synthetic diagnostic
output on failures while APK publication continues to require both successful
build and Android test jobs.

Acceptance criteria:

- A bank name in a body, unknown sender, ambiguous configured account, or a
  longer number containing an owned identifier must not prove an own transfer.
- Unique configured source/destination evidence still recognizes genuine
  transfers; an incoming bank receipt is not an outgoing bank-to-wallet transfer.
- Loan principal, repayment, outstanding balance and available credit remain
  separate fields tied to the right liability. Known M-Shwari and an invented
  unknown lender share financial grammar rather than needing a rule per amount.
- A remembered category applies to the same merchant despite ordinary Unicode
  spaces, while substrings and conflicting/missing phone identities stay separate.
  Remembered consent, one-off corrections and future application are tested in
  `RememberCategoryIntegrationTest` using a fresh invented-data database.
- `FinancialLabHistoryIntegrationTest` stores known and invented lender histories,
  checks KSh 2,500 minus KSh 500 leaves KSh 2,000, distinguishes observed versus
  estimated outstanding amounts, and replays the invented events without duplication.

Limits: the JSON comparison interprets individual steps without reconstructing
an installed user's ledger. Separate Android tests cover a few complete stored
histories; they do not diagnose a particular user's M-Shwari balance. These tests
do not repair previous account-review decisions, infer ownership from a name,
guess groceries for an unknown merchant, prove every bank wording, or add voice
entry. Existing shared-reference mirror-transfer integration tests remain in
the Android suite. Production interpretation and writes remain in Kotlin.

