# Paycheck field reconciliation — 11 October 2026

Base: eb58d181bffa (PR #348). User's screenshot shows selectable text extraction working but missing employer, pay month, gross field and deductions. Screenshot figures are not included in tests or source code.

## Observed root causes
- Payroll parsing recognized labelled Total Cash Earnings only as transient earnings, while gross auto-fill required a literal Gross Pay label.
- Employer and period recognition required a narrow single-line label format.
- A fixed list of deduction labels ignored other payroll deduction types and many local layouts; split native PDF text cells were only recognized for a small set of labels.
- User had to press Use extracted fields and review, and manual form fields appeared before the import action, contradicting the expected import-to-review flow.
- The accounting ledger separately requires gross minus net to equal explicitly reviewed employee deduction amounts. This policy must not be relaxed.

## Changes
- Source-independent field meanings: gross cash pay, total cash earnings, taxable pay, net pay, earnings breakdown, employee deductions, employer contributions, and total deductions.
- Gross auto-suggests from the unambiguous Total Cash Earnings label when no direct gross is present. **Taxable pay is never automatically used as cash gross.** Basic pay plus allowances is supporting evidence only, not assumed complete.
- Parse explicit single-line and paired employer/month metadata, labelled month/date formats and safely identifiable organization heading. Never use employee name, number, grade, account identifier or email as employer.
- Parse earnings and deduction sections using global aliases plus bounded, safely named unknown employee deduction labels as Other, always requiring user review. Recognize PAYE, NSSF tiers, health insurance, employer contributions, housing levy, SACCO, insurance, retirement and loan classes as payroll meanings, not employer-specific rules. Do not estimate statutory tax rates or remittance.
- Detect conflicting, repeated or multi-column/YTD evidence and NSSF aggregate/tier duplication; leave unresolved rather than summing guessed amounts.
- Auto-fill the form on successful import of a new payslip, allow corrections before saving, show unexplained gross-to-net difference and relevant warnings; require explicit review and matching deduction arithmetic. Edit-existing import still needs explicit apply. Prior payroll facts remain separate from canonical cash ledger.
- Keep all PDF text local and temporary and all tests synthetic. No external lookup by employer, no raw document uploads or personal-data logging, no schema/backup migration, no new permissions. Existing stored payroll journal remains the record authority.

## Deliberate limitations
- Actual employer detection cannot be guaranteed from a PDF with no unambiguous text employer; fallback remains manual. Scanned PDFs and Android pre-15 extraction are out of scope.
- Non-labelled numbers and rows where current vs YTD columns cannot be distinguished remain manual.
- Detailed earnings sublines and total deductions are review-only until a separate safely versioned payroll persistence extension. Gross/net and reviewed deduction/contribution lines continue to persist.
- No SMS/payroll settlement, taxes recomputed from rates, or claimed SACCO savings account balance.

## Verification
- Synthetic JVM payroll regressions (including user-observed layout pattern with invented amounts), existing global currency/ambiguity/privacy cases, lint, Android-test compilation, APK assembly, Python financial contracts.
- Instrumentation stays off. No manual duplicate GitHub workflow.
- Redmi real-device acceptance still needs user test after the verified APK.
