# Paycheck Analysis / PDF import reliability (11 October 2026)

Scope: one combined Android-native feature branch off main f20e461e64cc. User requested implementation, one reviewed CI build where practical, and **no instrumentation**.

## Problem and root-cause evidence
- The Activity Import control opens **StatementImportSheet**, which feeds every selected PDF into PdfStatementReview. That parser requires M-Pesa statement KES metadata and receipt rows, so a payslip triggers the misleading "KES currency could not be established from statement metadata" error.
- PdfImportState cancelled on Lifecycle.Event.ON_STOP even when the document picker temporarily stopped the Activity; state and extraction could disappear.
- Existing payroll journal review was nested in an ephemeral Salary bottom sheet and printed full raw extracted PDF pages, including unrelated staff metadata.
- Android API35 native selectable-text extraction already existed; file size was limited to 2 MB; current schema 40 payroll journal already isolates employer/month/gross/net/deductions/contributions from cash.

## Implementation
- Add persistent navigation destination **Paycheck Analysis** from Activity, Salary and Reports & Insights, reusing PayrollJournalStore and its existing user confirmation, editing and deletion. No cash ledger or schema changes.
- Keep selected-document state through the Android picker. Cancel only when the owning screen is truly disposed.
- Classify payslips before M-Pesa parsing; show a redirect button on incorrect statement selection. Never display raw selected statement/payslip text in UI.
- On the dedicated page, show edited reviewable employer, payroll month, gross, net, deduction and employer-contribution lines, alongside the latest/monthly saved summary and *reported* SACCO deductions. A payroll record never adds income or proves a contribution was remitted.
- Extend bounded payslip recognition with basic pay, house allowance, total cash earnings and taxable-pay labels as **preview-only earnings suggestions**. Do not store raw text, staff numbers, staff names, emails or addresses. If KES is absent but no foreign currency was detected, allow a preview with an explicit currency-check warning; the user still must explicitly review before saving.
- Increase PDF byte limit only to 3 MiB, retaining the 40-page and 100,000-character caps and API35 native reader. CSV remains at its current 2 MiB limit. Scanned OCR, arbitrary bank PDF statement layouts and API24–34 PDF extraction remain unsupported and do not trigger false transactions.
- Reuse the existing exact/unique M-Pesa statement candidate and CSV reconciliation flow. Uncertain rows remain unsupported; no bypass of account selection, duplicate review, date window or user confirmation.

## Verification gates
1. JVM regression: classifier redirects payslip with unrelated staff metadata and missing statement currency; true M-Pesa statement remains statement; irrelevant employment letter stays unknown.
2. JVM regression: payroll-only fields, Basic Pay/House Allowance/Taxable Pay, PAYE/NSSF/SHIF/SACCO, no identifying employee metadata, unknown KES requires review, foreign currency fails closed, multi-column ambiguous totals remain manual.
3. Boundary regression: PDF 3 MiB passes, 3 MiB+1 fails, bounded page/text limits still fail, temporary source-copy cancellation still erases partial file.
4. CI: unit tests, lint, Android-test source compilation, debug APK and offline financial regression suite. One PR workflow on the final combined head. **Do not run instrumentation, do not dispatch a duplicate workflow**.
5. User's Redmi document-picker lifecycle, password-PDF and actual unsupported payroll layout acceptance remain unverified until a voluntary device check.

## Deferred deliberately (no guessed financial claims)
- Persisting detailed basic-pay/allowance sublines across restarts requires a separately validated additive payroll schema/backup change. They are available as review-only suggestions here; the existing journal stores gross/net and deduction/contribution lines.
- Payroll-to-cash settlement, split salary receipts and remittance/reconciled SACCO balances remain Phase C, not implemented here.
- Arbitrary CSV conversion, OCR and pre-Android15 PDF extraction need separate review of dependencies/format compatibility.
