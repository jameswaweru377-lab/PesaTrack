# PesaTrack Remaining Accuracy Work

> **Execution:** Implement inline on the existing `codex/backend-accuracy-repairs-20261020` branch; preserve its current worktree changes.

**Goal:** Close the remaining safe accuracy and reliability gaps without changing the canonical-ledger authority or treating uncertain statement rows as confirmed money.

**Architecture:** Reuse the existing statement review, `EverydayMoneyStore` import/duplicate pipeline, bill schedules, and schema-v40 evidence journal. PDF cash rows may reach Activity only as explicitly selected `NEEDS_REVIEW` candidates; zero/informational rows stay in Statement Activity. Exact evidence links remain authoritative, and ambiguous matches remain pending.

**Tech Stack:** Kotlin, Android/Jetpack Compose, SQLite, Python/pdfplumber, existing JVM/Python tests.

**Spec:** User-approved local-first architecture in the current request and `docs/PRODUCT_CHARTER.md`; PDF acceptance limits in `docs/plans/pdfbox-production-evidence-gates.md`.

## Global Constraints

- Keep analysis and raw document processing on-device.
- Do not add cloud sync, OCR, a second accounting engine, or a new financial dependency.
- Preserve explicit account ownership, user review, duplicate/replay behavior, and atomic canonical writes.
- Never promote uncertain statement movements to trusted balances or income/expense totals.
- Keep PDFs/passwords/source content out of logs and remote services.
- Do not merge or publish until exact-head checks pass; device-only acceptance must be reported separately.

## Review Focus

- Same receipt code but differing account, direction, amount, or date must remain a distinct candidate.
- Same-day equal amounts without a receipt match must remain a possible match, never auto-merge.
- Informational/zero PDF rows must never become ledger transactions.
- A repeated payment must not satisfy more than one explicit bill.
- Unsupported or partially extracted PDF layouts must save nothing.

---

### Task 1: Preserve and verify existing local fixes

**Files:** Existing worktree changes in bill matching, statement landing/review, PDF extraction, and institution presentation; corresponding unit and Android-test sources.

- [x] Run focused existing regressions and inspect failures before further edits.
- [x] Repair only demonstrated defects; retain all unrelated edits already in the checkout.

### Task 2: Make PDF-to-Activity import explicit and review-gated

**Files:** `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementImportScreens.kt`, statement import policy/tests, and relevant UI tests.

- [x] Add failing tests for mapping verified nonzero M-Pesa rows to signed `StatementRow`s; exclude zero/informational rows.
- [x] Verify the tests fail for the intended behavior.
- [x] Add an explicit route from verified PDF review to the existing Activity import preview.
- [x] Reuse existing duplicate, possible-match, selection, date, account-snapshot, and atomic import gates. Keep every imported row in `NEEDS_REVIEW`.
- [x] Verify positive, duplicate, ambiguous-match, and zero-row cases.

### Task 3: Bound statement preview work for large histories

**Files:** `EverydayMoneyStore.kt`, a focused pure matching policy if needed, and statement import tests.

- [x] Pin down current matching behavior for exact reference, same-day amount match, and unrelated transaction.
- [x] Replace per-row full-history scans with indexed lookups while preserving exact-match semantics.
- [x] Verify large synthetic previews produce identical decisions and bounded work.

### Task 4: Finish bill payment matching safety

**Files:** Existing `BillPaymentMatchingPolicy.kt`, `BillSchedules.kt`, and planning tests.

- [x] Verify explicit rule IDs remain authoritative and an unclassified transaction can match only one bill.
- [x] Verify overlapping bill rules and amount-only similarity do not double-count.

### Task 5: PDF extraction diagnostics and performance evidence

**Files:** `mpesa_pdf_engine.py`, `tools/python_lab/test_mpesa_pdfplumber_android.py`, and PDF acceptance notes.

- [x] Verify the current duplicate-cell-extraction optimization against synthetic valid and invalid layouts.
- [x] Capture elapsed time for a fixed invented multi-page fixture; do not set a success threshold from a single host run.
- [x] Retry the supplied PDF locally and retain only its safe pass status and elapsed time; do not output statement values or identifiers.
- [x] Leave provider/device acceptance open: the local Python result does not validate Android/PDFBox or device behavior.

### Task 6: Build, review, and document

- [x] Run focused Kotlin and Python regressions, then required project unit, lint, Android-test compilation, and APK checks.
- [x] Review the full diff for financial writes, privacy leaks, and unrelated UI changes.
- [x] Update `CURRENT_STATE.md` with unmerged-code versus device-acceptance status.
- [x] Report remaining external gates honestly; physical-device proof is absent.
