# Salary and pay-cycle presentation

Salary has three short views: Overview, Add salary and Source. Setup stores the employer matcher once, using two account choices per row. The payday range remains editable without filling the overview with instructions. Recorded salary history initially shows three receipts, with more on request.

Receipt dates use the Material calendar picker. UTC picker dates are converted to local midnight before recording, preserving the ledger calendar day in Kenya and across other time zones. Future dates are unavailable. The source matcher, not the amount alone, recognizes future matching payment messages. An expected payday does not invent a receipt.

Removal requires confirmation. A purely manual receipt is excluded from totals; an SMS-backed receipt (including a manual entry later enriched with its payment code) remains ordinary income. Salary boundaries are rebuilt atomically with the correction. The saved employer matcher remains enabled. Explicit user corrections survive message replay, and removed receipts are hidden from optional salary suggestions. Explicitly adding the same removed manual receipt restores its row rather than creating a duplicate. A captured payment whose salary label was removed blocks a duplicate manual entry; it can be marked as salary again from Activity.

Activity puts the date range, spending, separate fees, income and transfers above compact transaction rows. Category labels are readable, with an optional explanation of cycle boundaries. Home puts the account overview before the spending breakdown.

Validation added: timezone/calendar day tests; database removal, replay, late SMS and re-entry tests; Compose calendar, account grid, manual entry and confirmation tests. Android CI runs the full existing suite, lint and emulator checks. These checks do not replace verification on a physical phone.
