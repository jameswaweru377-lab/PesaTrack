# Responsive Paycheck Analysis dashboard

## Reference and scope
Implements the user-selected visual concept from a screenshot shared in this conversation. Replaces the standalone `paycheck-analysis` screen's default long form with a premium dark-theme-compatible dashboard. The existing Activity/Salary/Reports navigation and original payroll journal import/edit/delete flows are retained. No backend, schema, cash-ledger, parser, network or permission changes.

## Visible layout
- Title and saved-month selector; user can choose an employer/month already stored.
- Four colored summary cards in a 2x2 grid on standard phones: gross, net, employee deductions, and separately reported employer contributions. Small/narrow screens use one column, wide tablets use four.
- Gross-versus-net trend with only saved values for the selected employer and up to six real periods, sorted in time. With fewer than two periods, show a request for another saved month, not a fabricated chart.
- Donut and exact-value deduction legend for PAYE, NSSF, health, and a grouped Other total; user can expand original deduction labels to see all classifications independently.
- Key insights based on the current saved payroll: largest deduction type and net-pay-to-gross ratio. Employer contributions are never double-counted.
- Prominent PDF import action, secondary manual entry, and a compact saved-record card, plus a View all list. Date selector and saved records provide direct paths to edit and verified deletion.
- Existing bottom navigation stays provided by AppScaffold; original PDF picker/password dialogs and user-reviewed arithmetic remain unchanged.

## Responsive and privacy behavior
- 320dp class widths: single-column summary.
- Most 360dp–730dp phones: 2x2 metrics, trend and donut vertically stacked, long currency figures on separate text lines.
- 730dp+ large foldables/tablets: trend beside donut.
- 760dp+ widths: four summary metrics horizontally.
- Saved record summary uses 2-column amounts on mobile; 4 columns on wide screens.
- Uses MaterialTheme surface/onSurface colors and violet/pink/teal/gold accents so dark mode follows the approved reference while light-mode remains legible.
- Imports and saves only data from existing PayrollJournalStore; no embedded sample graph points, hardcoded employer, static deductions or personal payslip content.
- Absence of a previous month never generates fictional growth claims. Payroll analysis does not record bank receipts or remitted SACCO balances.

## Verification
- Compile Android Compose source, run JVM unit tests and lint, synthetic Python financial regressions, generate debug APK in a single automatic PR workflow.
- Never run instrumentation; source compilation of Android tests is allowed.
- Visual user acceptance remains required on the Redmi and narrower/large devices. Review wrapping, small fonts, dark/light mode, empty state, single month, multiple employer/month, import, edit and delete.
