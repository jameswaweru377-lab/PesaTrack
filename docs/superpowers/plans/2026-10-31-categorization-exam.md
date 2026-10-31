# Categorization Exam Implementation Plan

**Goal:** Freeze a reproducible, invented 600-case baseline of PesaTrack's default offline category decisions without changing categorization behavior.

**Architecture:** Reuse the production `KenyanTransactionRecognizer` on the JVM. Extract only the existing `TransactionStore` fallback decisions into a small pure policy that production and the exam both invoke; preserve the existing priority of user rules, built-in recognition, cached hints and fallback. Exclude user-specific rules and cached remote hints from the exam, and report that limitation explicitly.

**Tech stack:** Kotlin, JUnit 4, existing Gson test dependency, Gradle JVM unit tests.

**Spec:** Owner-approved Task 1 in the 31 October 2026 accuracy audit follow-up and its Phase 1 verification report.

## Global constraints

- At least 500 invented Kenyan cases, including paybills, tills, person-to-person, Fuliza, fees, reversals, airtime, salary, rent and school fees.
- No real names, phone/account numbers or actual financial messages; no network, telemetry, new permissions or dependencies.
- No changes to categorization semantics, UI, data schema or transaction counting. Do not add functionality to `TransactionStore.kt`, only extract the existing fallback.
- Track correct, wrong, needs review and uncategorized-without-review separately, overall and by transaction type.
- A frozen baseline guards against increases in the wrong-answer rate on the same cases, while the report retains all four outcomes.
- Every new or modified function is bounded; no new file exceeds 800 lines and no new function exceeds 80 lines.
- Update `CURRENT_STATE.md` in the same PR. No merge.

## Review focus

1. A reversal with an existing reference must retain the current neutral automatic result.
2. A reversal without a reference must retain the current neutral needs-review result.
3. Unrecognized person-to-person/paybill/till transactions must not be falsely scored as explicitly sent to review.
4. A phrase such as 'airtime' in a reversal may reveal a wrong automatic answer and must not be silently omitted.
5. A changed fixture denominator or duplicate fixture identifier must fail the test instead of weakening the wrong-rate gate.

## Steps

### Task 1: Labelled invented case corpus and scoring contract

**Files:** Add `app/src/test/resources/categorization-exam-v1.tsv`; add `app/src/test/java/com/kedevjimmy/pesatrack/domain/rules/CategorizationExamTest.kt`; add `app/src/test/resources/categorization-exam-baseline-v1.json`.

- [ ] Write JVM test loading invented rows, validating unique IDs, required type coverage, expected direction/category and exactly four outcomes.
- [ ] Confirm red: the existing fallback is not yet exposed as a pure JVM decision, so the test cannot compile until extraction.
- [ ] Add the minimal pure fallback extraction in Task 2.
- [ ] Run test and record overall/per-type counts; freeze the measured wrong count and corpus digest in JSON.
- [ ] Rerun test so any future increase in wrong count with the unchanged denominator fails CI.

### Task 2: Behavior-preserving fallback extraction

**Files:** Add `app/src/main/java/com/kedevjimmy/pesatrack/domain/rules/DefaultCategoryFallbackPolicy.kt`; extract the existing `when(parsed.type)` fallback from `app/src/main/java/com/kedevjimmy/pesatrack/data/local/TransactionStore.kt`; add `app/src/test/java/com/kedevjimmy/pesatrack/domain/rules/DefaultCategoryFallbackPolicyTest.kt`.

- [ ] Write direct tests for every current `MpesaTransactionType`, both reversal-reference states and auto-vs-review outcomes.
- [ ] Confirm red because the pure policy does not exist yet.
- [ ] Move the fallback's exact decision table into the pure policy; call it from the unchanged production ordering after cached hints.
- [ ] Rerun focused JVM tests; do not change any case's expected outcome to conceal a regression.

### Task 3: Documentation and full verification

**Files:** Modify `CURRENT_STATE.md`; optionally add a short human-readable baseline under `docs/` if CI output is hard to locate.

- [ ] Record scope, counts, denominators, limitations, and that user-specific rules and cached remote suggestions are deliberately excluded.
- [ ] Verify the test fixture and baseline cannot contain real personal financial data.
- [ ] Run JVM tests, lint and debug build in CI if local Android tooling cannot resolve; record exact run SHA/counts.
- [ ] Review the diff for scope creep, push one PR to `main`, and leave it unmerged.
