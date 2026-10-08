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

This is **a research and testing scaffold**, not five new production features. A first local evaluation may show low coverage or wrong labels; these are actionable experimental findings, not grounds for automatic deployment. This change does not modify existing Android, Worker, CI or APK production. No additional GitHub Actions jobs are introduced, avoiding extra Actions minutes while other repository work is active.

Reversibility: remove this folder, with no app database or APK changes.
