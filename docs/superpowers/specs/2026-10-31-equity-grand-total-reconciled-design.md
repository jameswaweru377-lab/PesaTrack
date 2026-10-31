# Equity Grand-Total Reconciled Statements — Design

**Owner direction (31 October 2026):** Make the historical Equity PDF useful for financial summaries when its original Credit, Debit, closing balance, and printed grand Total reconcile. Preserve rigorous source checks, local privacy and truthful labels.

## Intent

The historical 14-page Equity format has explicit Credit (Money In), Debit (Money Out), Balance and a final printed Total. The existing extractor validates 200 rows, 199 adjacent running balances and the three grand figures, but assigns `GRAND_TOTAL_AND_CHAIN_ONLY` because no independent printed opening balance or per-page subtotals appear. It already saves the reconciled incoming/outgoing amounts and source rows in a separate SQLite archive. Today's screen browses rows but has no meaningful source-specific report.

## Chosen approach

**Grand-total reconciled statement report, not a fabricated FULL proof.** Use the already admitted limited-archive records to show a per-document Money In, Money Out and net-movement summary, plus monthly subtotals from that document's dated rows. Call this a *Grand-total reconciled* statement. Explicitly explain that the printed opening balance is unavailable; a calculated opening cannot substitute for independent source evidence. Do not promote the archive to `FULL` or claim independent opening/page proof.

The alternatives considered were (1) require a different bank PDF with independently printed opening/page totals, which would preserve strict FULL but leave this historical file without a summary, and (2) simply relabel the same evidence as FULL, which would be misleading. Both are rejected for this scope.

## Behaviour

- The Equity account's saved-statement list shows individual reconciled source records and source-specific gross Money In/Out. Do not sum distinct imported PDFs on the account landing: overlapping files could duplicate observations.
- Opening a saved source loads its original immutable legs and independently recomputes per-source row count, Money In and Money Out; it shows totals and monthly breakdown only when these match the persisted source totals exactly. Any mismatch, overflow or row read failure gives a visible error, never an invented number.
- Month assignment uses `Africa/Nairobi` and integer minor units. Monthly subtotals sum exactly to the source's grand totals; net movement is credits minus debits, **not** profit, salary or spending classification.
- The source's original transaction rows remain browseable. Descriptions do not automatically become a counterparty, category, own-transfer or salary determination.
- Import screens label the existing strict `GRAND_TOTAL_AND_CHAIN_ONLY` admission as *Grand-total reconciled* and state what did and did not reconcile. The physical SQLite assurance and archive separation remain unchanged.
- Existing saved historical Equity documents become readable under the new UI without deletion, reimport or migration; no database tables, migrations, backup format, permissions, privacy policy, network, analytics, signing or release workflows change.
- The existing global verified-cash-flow summary, `StatementBankReportReader`, cross-account/duplicate policies, and canonical Activity ledger remain untouched. Global Statements account visibility is already being changed in the independently owned draft PR #502; do not overwrite or merge its branch from this feature.

## Acceptance and negatives

1. Synthetic single-source credit/debit rows produce exact per-month and grand Money In/Out, net and row counts; opposite sides of a transfer remain gross cash observations, not income/spending.
2. A source with incorrect row count or saved grand total, nonpositive timestamps, zero movements, or arithmetic overflow produces no displayable cash summary.
3. Month boundaries use Nairobi local time; multiple source PDFs never silently aggregate or duplicate their source totals.
4. Import verification continues to reject changed financial cells, missing/unaccounted rows, account mismatches, altered printed grand totals and broken balance chains. Existing M-Pesa imports and fully verified bank reports remain unchanged.
5. The UI clearly differentiates *Grand-total reconciled* from *fully verified opening balance* and shows a failure state if rows cannot be reconciled after loading. Kotlin JVM tests and compiled Android UI tests must pass CI; emulator/physical Redmi acceptance remains a separate explicit gate.

## Scope and coordination

This is a read-only reporting/presentation upgrade of already reconciled Equity evidence, not a universal bank parser or a change to the definition of the canonical ledger. Keep code changes to one small domain projection, the saved-bank source screens, targeted import wording, tests and current-state notes. Recheck current `main` and open Statements PRs before PR creation and merge.
