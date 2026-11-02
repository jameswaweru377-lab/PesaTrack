# Cross-source totals consistency — non-posting preview design

## Goal

Make it possible to explain how one financial movement appears across an original SMS/Activity entry and one or more M-Pesa or bank statement observations, without double-counting or losing genuine payments.

## Current authority and release hold

The canonical Activity ledger remains authoritative for Home, Activity, Plan and Reports. Statements retain their independent per-account reconciled totals and source-assurance labels. User confirmations in the draft matching review are **metadata only**. They must not trigger ledger edits, source deletion, accounting suppression or balance changes.

`docs/acceptance/N2_STATEMENT_FEATURE_GATE.md` explicitly holds new combined statement-derived totals until R1 signed phone and T1 executed Android persistence gates pass and the owner approves resumed admission. This design does not lift that hold.

## Preview classification rules

| Evidence | Preview explanation | Financial posting |
|---|---|---|
| Independently verified statement leg matches one original Activity leg in a confirmed same-movement group | Two observations of one possible cash leg; Activity remains canonical | None |
| Opposite legs on separately confirmed owned accounts in a confirmed transfer group | Potential internal transfer; account balance movement is not earned income/spending | None |
| Exact code with contradictory amount, type or fee | Conflicting evidence requiring review; neither observation is discarded | None |
| Equal amount/time only, masked account number, ambiguous cluster, protected fee/debt/reversal or limited-assurance source | Unresolved; cannot suppress any canonical observation | None |
| One source is missing | Preserve current independent totals and show coverage gap, not a synthetic zero | None |

## Required tests before any accounting change

1. Synthetic triad: original wallet SMS + wallet statement + opposite bank statement must represent one wallet leg plus one opposite transfer leg, not three financial movements.
2. Repeated statement exports covering the same dates must not multiply a confirmed cash leg; conflicting reference/amount combinations stay unresolved.
3. Two genuine same-amount payments close in time must remain two movements absent uniquely corroborating evidence.
4. Fees, Fuliza, loans, repayments, refunds and reversals retain their protected accounting roles.
5. Category, account ownership, exclusions and user corrections must not be overwritten by a later import or replay.
6. Interrupted migration, encrypted backup/restore and old-schema upgrade preserve both original Activity rows and pending conflict evidence.
7. All displayed Money In/Out figures must either use one shared authoritative projection or carry distinct, truthful labels for different scopes.

## Acceptance and staged integration

First, test a read-only summary comparison with synthetic data and no SQL writes. Then verify one exact combined candidate through JVM checks, focused executed emulator persistence, full regression triage, and disposable-data signed physical-phone persistence. Only after the N2 owner-resume decision may a separate accounting-impact PR propose a canonical projection change. No draft confirmation alone proves a transfer or authorizes hiding a transaction.
