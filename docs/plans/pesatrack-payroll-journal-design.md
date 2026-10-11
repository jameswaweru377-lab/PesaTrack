# Payroll review and journal design — C1/D1

User authorization: proceed through pending blueprint work, combine safe related tasks, no instrumentation. This is the next cohesive package after PR343/main are green. Do not start another main workflow while the prior one is active.

Goal: a separate local payroll journal with manual gross/net/deduction/contribution review, optional conservative selectable-PDF suggestions, and explicit reported—not-received labels. User review is required before saving. No cash linking or transaction writes in C1.

Architecture: additive schema39 payroll records and ordered lines, atomic journal-only save/edit/delete with stale revision checks; pure integer-minor-unit arithmetic/types. Backup34–38 compatibility adds empty journal tables only and never overwrites existing supplied tables. Existing cash/account/debt history and balances are untouched. A payroll record is not a receipt.

Record: UUID, revision, employer (bounded), ISO payroll month, KES gross and net payable, reviewed source MANUAL or PDF_TEXT, ordered positive line amounts with employee/employer role and explicit semantic type. One record per normalized employer/payroll month prevents repeated imports from inflating summaries; corrections edit the existing record. Multiple payslips in the same month are deferred rather than silently summed. No deduction-derived asset, pension, SACCO or loan balance.

Validation: employee deduction sum must equal gross minus net; employer contributions never reduce net. Checked arithmetic, strict decimal currency input, bounded lines, unique line IDs/order, no negative/zero/overflow amounts, no guessed statutory tax rates. Unknown deductions remain explicitly unclassified. Reported amounts and no matched receipt are separate concepts; no outstanding-cash metric until multiple-settlement links exist.

UI: Payroll details in existing Salary overview, independent of salary source setup. List journal records; open manual form or optional PDF text review; field correction and explicit Save payroll details; visible not a cash receipt disclaimer, reported earnings/withholding/contributions separated; Edit/Delete revise only journal. Existing Import statement remains unchanged.

PDF: reuse bounded isolated API35 extractor; strict complete labelled lines with a single KES amount only. Generic exact label registry (gross/net/PAYE/health/NSSF/pension/SACCO shares/deposit/insurance/loan principal/interest/employer contributions). Ambiguous SACCO/loan, duplicate/conflicting fields, multicolumn/multiple money values and blank pages require manual review. Never infer employer/month or deposit; unsupported layouts editable manually; no OCR or API24 library. No document retention or upload.

Tests: synthetic JVM arithmetic/type/bounds/conflict/parser/replay contracts and exact SQL SQLite lab migration/constraints/rollback/replay; Android journal transaction/migration/backup regression source compilation only. Whole bundle unit/lint/APK build, source review, one PR CI; merge/main sequential. Device migration/service/UI acceptance explicitly unverified because instrumentation is paused.

Future: full evidence observation registry, receipt settlement linking/multiple payouts, parser profile corpus/OCR/API24, remittance proof and contribution analytics. Do not claim full C/D completion.
