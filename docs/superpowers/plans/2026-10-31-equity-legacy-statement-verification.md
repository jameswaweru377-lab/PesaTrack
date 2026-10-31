# Older Equity statement verification implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Safely read the older 14-page, borderless Equity statement without misrepresenting its missing opening balance and page-by-page totals as fully verified.

**Architecture:** Keep the existing strictly verified ruled-table extractor unchanged. Detect one narrowly proven legacy Equity layout through on-device PDF geometry and its account/period/column evidence, verify every financial row, adjacent balance and the double-drawn printed grand Total, and explicitly carry a lesser assurance level to archive/UI/report consumers. Fail closed for any unknown, incomplete or contradictory PDF.

**Tech Stack:** Python pdfplumber/Chaquopy, Kotlin, SQLite, Compose, synthetic ReportLab PDF tests.

**Spec:** The owner-approved 30 October older-Equity-PDF handling request; this plan captures the recovered 14-page layout evidence without storing the private file.

## Global constraints

- No source PDF, personal name, account, reference or balance enters GitHub, CI fixtures, logs or third-party services.
- Existing independently verified Equity PDF import and M-Pesa imports must remain unchanged.
- Imported bank documents never mutate Activity, canonical account balances or debt.
- The private file has a printed grand Total (all three values match the reconstructed rows), but no printed opening balance or per-page totals. Any lower-assurance import must never be represented as complete verified coverage or mutate Activity.
- Require exact full-account match and explicit review before saving; duplicate imports stay idempotent.
- Android CI unit tests, lint and debug APK must pass before merge; real phone acceptance remains a separate gate.

## Review focus

1. Graphic-logo header without extractable issuer label: safe legacy layout detection versus unrelated PDF rejection.
2. One omitted or repeated financial row: exact cell/row inventory must fail, including across page boundaries.
3. Changed amount, cash direction or balance: complete adjacent chain must fail without inventing opening balance.
4. Different selected account or non-Equity provider: refuse before persistence.
5. Missing per-page totals and opening balance: cannot silently acquire the same coverage claim as the fully proven layout.

---

### Task 1: Synthetic PDF evidence adapter

**Files:**
- Create: `app/src/main/python/equity_legacy_extractor.py`
- Modify: `app/src/main/python/bank_table_extractor.py`
- Test: `tools/python_lab/test_equity_legacy_extractor.py`

- [ ] RED: Write invented graphical-logo, borderless multipage PDF test; assert all rows, their dates/references/amounts/balances and `balance_chain_only` status.
- [ ] RED: Add negative cases for missing/duplicate rows, identity changes, dates/columns, currency, overlaid text, and adjacent balance errors.
- [ ] Run focused Python tests to see the expected failures.
- [ ] Implement bounded layout-specific geometry adapter with no path/network/file writes and safe error codes.
- [ ] Run focused Python and full Python laboratory suites.

### Task 2: Kotlin admission and archive contract

**Files:**
- Modify: `StatementBankAdmission.kt`, `OfflineBankTableReader.kt`, `StatementSourceProfileRegistry.kt`
- Modify: `StatementBankArchiveStore.kt`, `StatementArchiveDb.kt`
- Test: `StatementBankAdmissionTest.kt`, Android archive migration/integration tests.

- [ ] RED: Prove missing per-page totals/oldest opening cannot pass the existing full path but may enter the separate grand-total-plus-chain path with exact owner/coverage proof.
- [ ] RED: Prove v5-to-v6 migration keeps existing source rows/category labels and legacy batches, while the new flag remains false.
- [ ] Implement explicit provenance flags and lossless archive migration, replay validation and idempotent save.
- [ ] Run available local JVM tests, then CI for Android-only verification.

### Task 3: Consumer isolation and user-visible assurance

**Files:**
- Modify: `StatementBankReportReader.kt`, `StatementEvidenceReadModel.kt`, `StatementImportScreens.kt`, bank source view.
- Test: focused report/coverage and UI policy tests.

- [ ] RED: Prove chain-only rows cannot contribute to verified report totals or correlation links.
- [ ] Implement clear `Limited assurance` label, separate saved-source row access, and specific failure messages.
- [ ] Run full Python, Kotlin CI unit/lint/APK checks, review privacy/backup/replay impacts and update `CURRENT_STATE.md`.
- [ ] After current-head green CI and compatibility check, merge if safe; otherwise report the exact gate without changing `main`.
