# Payroll journal C1/D1 implementation plan

Execution: superpowers:executing-plans inline, whole-bundle review. Spec: pesatrack-payroll-journal-design.md. User authorizes autonomous combined tasks and explicitly prohibits instrumentation.

- [x] Confirm PR343 current-head CI, merge with no active main and record resulting main SHA. Start next branch at that main; do not merge next branch until previous main finished.
- [x] Write PayrollJournalPolicyTest and PayslipFieldReviewTest: strict money, gross70000/net52000, employee sum18000, employer exclusion, explicit principal2600/interest400, historical labels, malformed/overflow/missing/ambiguous PDF fields and no deposit authority. Observe red before policies.
- [x] Implement pure PayrollJournalPolicy: immutable model, bounded positive typed lines, current user-reviewed source, checked integer totals, normalized employer/month duplicate key and canonical content fingerprint. Do not assume interest split or account balance.
- [x] Implement generic anchored payslip label review: only one explicit amount per line, no fuzzy match/tax computation, conflict flags, source labels and page numbers, manual fields editable; PDF suggestions never invoke writes.
- [x] Add additive PayrollJournalSchema39 two tables and trigger; call from fresh/upgrade schema; update backup34–38 missing-table compatibility without replacing supplied values.
- [x] Implement PayrollJournalStore: CanonicalStoreLock, atomic save/revision edit/remove/read, unique employer-month records, no canonical ledger/account/PayCycle writes. Cross-check policy on loaded records; source labels retained.
- [x] Add SalaryScreen Payroll details sheet/list with manual fields and typed line controls, explicit reported-only Save/Edit/Delete; optional API35 PDF reader only pre-fills review fields. No mandatory payroll documents.
- [x] Add exact SQLite DDL development-lab constraints/migration/replay/rollback tests plus Android repository/populated migration/encrypted backup source regressions. Compile Android tests only.
- [x] Update current-state/tracker/readme, run combined unit/lint/APK/Android-source compile plus Python lab, request fresh source review; batch corrections and freeze.
- [ ] Create one reviewed PR with exact current-head checks/APK. Ensure previous main complete before merge; verify next main/APK; update issue322. No instrumentation/workflow/signing edits.

Acceptance limits: C1 is journal and labelled summaries, not full settlement/evidence registry/remittance accounting. D1 is strict selectable text suggestions with manual correction, not arbitrary PDFs/imageOCR/API24 extraction. Actual migration/restore/UI/service/device execution is explicitly unverified under the pause. Test confidence alone is not physical device acceptance.

Final frozen-source local validation:851 JVM cases0 failures/errors/skips, lint/APK/Android test Kotlin compilation successful. Python37/37; independent source review approved. No instrumentation or workflow/signing changes. Exact PR/main delivery evidence will be maintained in master issue322.
