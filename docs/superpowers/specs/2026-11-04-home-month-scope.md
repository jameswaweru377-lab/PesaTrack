# Home monthly-activity placement (approved 4 November 2026)

The Home screen should get to the user's available money without repeating the
bottom navigation's Home destination as an "Overview" heading. Move the month
picker from the separate top row into the existing white financial-summary card,
beside a compact "Monthly activity" label and just above Money in / Expenses.

Keep the current confirmed-cycle available amount independent of which
historical month the user chooses. Keep selected-month Money in, Expenses, net
recorded flow, review entries and latest transactions period-scoped, without
recalculating those amounts or changing ledger classifications. Make the flow
caption name the selected month instead of always saying "this month". Account
balances and upcoming items remain current/future-facing.

Preserve the approved PesaTrack green/black brand, five-tab bottom navigation,
existing cards/rows, real-source data and month chooser. No new permissions,
dependencies, persistence, financial calculations, or instrumentation tests.

Approaches considered: deleting the month picker would obscure history; a
top-level month picker implies it changes all Home sections. The selected
approach keeps the control beside the monthly figures it actually filters.
