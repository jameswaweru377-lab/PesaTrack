# Payroll Outflows and longer PDF statement verification — 11 October 2026

**User findings:** A redacted three-column payslip includes Inflows, Outflows and Calculations. Payroll import skipped abbreviated PAYE/NSSF I/II/AHL/SHIF, a student-loan line and 0.01 rounding adjustment, preventing journal reconciliation. M-Pesa PDF statement exceeded existing 100,000-character reader limit. Preserve local-first privacy: no payslip image or real financial figures added to the repository.

## Payroll safeguards
- Outflows is an employee deduction section; Calculations is a separate explanatory column. The latter may repeat computed PAYE and show signed tax relief values; it must never create extra journal deductions.
- Recognize punctuated provider-neutral abbreviations: P.A.Y.E., N.S.S.F. Tier I/II, A.H.L., S.H.I.F., H.E.L.B., and Rounding Cfwd (KES). Preserve source labels and NSSF tier separation.
- H.E.L.B. is a reviewed **Other** deduction: the payslip alone does not disclose principal vs interest or a full loan balance.
- Total Deductions can exclude a separately printed rounding carry-forward. The parser recognizes a separately labelled positive rounding amount (when present) that may explain the gross-to-net penny difference. Neither the reported subtotal nor supporting calculations are used as extra deductions. Existing strict gross = net + employee deduction lines rule remains in force.
- Unknown labels still need explicit review; ambiguous totals fail closed. No bank/cash, loan-balance, or SACCO remittance posting; no schema changes.

## PDF safety/performance
- Previous limits: 3 MiB file; 40 pages; 100,000 extracted characters; 45s; max 2,000 parsed rows.
- New bounded import limits: **8 MiB; 160 pages; 500,000 characters; 90s; 5,000 rows**. CSV remains 2 MiB, but uses the same reviewed row maximum. This is not an unlimited one-year guarantee.
- Native isolated PDF reading (Android API35+) sends each page in <=12,000-character messages to avoid Binder's single-message-size limit. An ordered, capped assembler rejects missing, reordered, excess-page or oversized chunks. No page truncation, document upload, raw-text logs, or persisted decrypted text. The existing temporary encrypted/selected PDF copy is deleted.
- Existing supported M-Pesa PDF layout metadata and row-consistency validation remain required; extending size limits does **not** imply universal PDF statement support. Scanned/image PDFs still need separate on-device OCR.

## Verification
- Invented three-column payslip: distinguish outflow deductions vs computation relief, total + rounding vs net; check gross/net journal validation.
- Native PDF policy boundaries, 500k-character chunk assembly/order/size and incomplete transfer failures; older PDF/CSV, duplicate, date, account and financial contract regressions.
- CI unit tests, lint, Android instrumentation **compilation only**, debug APK and Python financial synthetic evidence. Instrumentation is paused by user request.
- Test the actual M-Pesa PDF and redacted payslip on device after build; the exact source text order from Android PdfRenderer may still differ. If layout unsupported, report a bounded reason, never partially post transactions.
