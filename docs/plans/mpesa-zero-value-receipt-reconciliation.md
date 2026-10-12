# M-Pesa zero-value receipt import — 12 October 2026

## Observed device failure
After PR #356/#357, a user attempting to import a Safaricom PDF statement received:
> A transaction has no single nonzero wallet movement; review the original statement.
The document opened, but no import candidates were prepared.

## Confirmed parser failure
`PdfStatementReview.read()` treated any parsed zero signed movement as an immediate fatal failure, before applying running-balance reconciliation. The error is emitted when a compact two-money-cell row has an actual numeric `0.00` movement, or when a three-cell row contains an explicit zero paid-in and zero withdrawn value. The screenshot/source PDF does not prove which individual receipt triggered it.

## Safe implementation
- Treat an *explicit numeric* zero-value receipt as a nonposting informational statement row (not a cash expense, income or transfer).
- Keep that receipt in the **full ordered running-balance chain** for both pages and receipt references; do not silently skip balance evidence, break the chain or introduce cash deltas.
- Require at least one adjacent row to verify a zero-value record. For compact rows, retain prior Safaricom structure and direction checks and require at least one explicitly signed debit. Reconcile **every adjacent receipt**, not just the rows that remain after dropping informational entries.
- Reject ambiguous entries with missing numeric amount cells (e.g. bare `-`), duplicate references, missing balance proof, wrong balances or unsupported zero-only imports.
- Exclude verified zero-value receipts from the import candidates and summarize how many were excluded. Do not persist source descriptions or zero-value financial transactions.
- Maintain the established local PDF extraction cap, currency warning, date validation, account selection, transaction duplicate review, confirmation, no partial import, and no instrumentation.

## Synthetic regression coverage
Tests exercise two pages with a verified zero-value record between deposit/debit rows, changing balances, missing money fields, duplicate zero receipts, three-column explicit zero cells, dash-only unknown cells, zero-only statements and single unproven zero receipt. No real statement, identifying name, phone number, reference code or PDF is copied to the test suite.

## Limitations
Build and synthetic tests do not establish actual Safaricom PDF layout behavior on this user's phone. The user should try the rebuilt APK with the selected M-Pesa wallet and compare receipt counts, signed amounts and balances against the source before confirming import. For unsupported native PDF text order, remain fail-closed rather than guessing.

## Follow-up: split PDF text-cell errors (12 October)
The latest reported phone error is `A receipt row or wrapped description could not be positioned safely`. The prior line normalizer rejected standalone money-cell lines appearing after the Completed status, a normal possibility when Android exposes a PDF table as text fragments. It now reconstructs exactly two or three terminal money/placeholder cells from multiline rows, allows alphabetic wrapped descriptions after those cells, and safely ignores an exact repeated table header. It does **not** accept numeric fragments embedded in wrapped descriptions, interleaved money/words/money, unknown transaction status, extra money columns or incomplete receipt rows. The UI error now states page number and receipt row ordinal only, never private receipts, phone numbers or amounts. Synthetic regression cases reproduce the split-line layouts and ambiguous cases without a source statement.

**Device verification is still required**: neither a green CI build nor these synthetic fixtures proves the exact text geometry in the user's Safaricom PDF. The parser should refuse unsupported row shapes rather than fabricate credits, expenses or partial cash records.
