# Transfer reconciliation

The Statements match-review entry now opens a transfer workspace. It connects evidence of movements between explicitly owned bank and mobile-wallet accounts. The existing match and duplicate review remains available through **Other matches and duplicate review**.

This is a non-posting reconciliation layer: confirming a pair saves review metadata. It does not delete a transaction, change a category, create a missing movement, repair a balance, or change income, expense, budget or report totals. Integration with economic totals requires a separate accounting change and independent verification.

## What establishes a candidate

1. A stable shared reference, opposite movements, matching integer principal and currency, and separate explicitly owned accounts; or
2. A full account identifier in explicitly directional bank narration, with a compatible opposite movement in that owned account.

For example, the supported grammar can interpret `Transfer to account 009876543210` or `Transfer from M-PESA 0712345678`. These are synthetic examples, not evidence of any particular bank's real output. Generic mentions, names, paybill names, partial numbers and masked identifiers do not establish an account route. Kenyan wallet number aliases are normalized only for wallet accounts. Duplicate configured identifiers remain ambiguous. Narration formats outside this bounded grammar remain unresolved.

Time limits the candidate search to 48 hours; it never proves a transfer. The year view loads surrounding context to detect cross-year counterparts and competing observations. A different amount is not automatically explained away as a fee. Currency conversion and multi-stage transfers are not inferred.

## Preserving accounting meaning

The original bank debit and wallet credit remain separate account movements. Strict existing same-leg evidence may group several observations of one movement for review, without deleting any record. Repeated equal amounts with more than one plausible counterpart remain ambiguous.

Existing protected Activity roles and explicit Activity user decisions are withheld. Persisted statement category choices are preserved. Explicit loan, repayment, savings, Fuliza, fee, reversal, refund, pending and failure narration is withheld from ordinary transfer pairing. These checks are conservative protection, not a complete loan, fee or reversal accounting engine.

Verified bank source identity must match the currently configured account. Limited-assurance sources are not promoted. Older Activity and M-Pesa records may lack the original account identifier, so every new transfer confirmation requires an explicit check of both original account identities and the relationship.

## Review states

- **Needs review:** supporting reference or account-route evidence found; no confirmation saved.
- **Confirmed by you:** account attestation and relationship confirmation saved for this exact evidence.
- **Rejected by you:** the proposed pair was rejected. The original movements remain intact.
- **Counterpart unresolved:** a narration identifies another configured owned account, but no unique eligible counterpart is linked. This does not indicate a failed payment or a missing bank balance.
- **Ambiguous or withheld:** conflicting candidates, incomplete evidence or protected semantics prevent confirmation.

Confirm, reject and undo only affect review metadata. All screens explicitly state that fees have not been reconciled and financial totals remain unchanged. Statements and Activity evidence can have different coverage; an empty candidate list is not a proof of no transfers.

## Persistence and safeguards

The layer uses the existing `statement_match_decisions_v1` table with a distinct versioned fingerprint namespace. No database version or backup format changes are needed. Fingerprints bind the observation identities, account identities and ownership revisions, amounts, currency, times, references, source verification and available narration. Changed evidence requires fresh review.

The writer re-reads evidence inside the canonical lock and an archive transaction. Stale expected decisions cannot overwrite newer choices. Both the new and existing review writers check for conflicting confirmed use of an observation. Financial tables are not written. Undo removes only this layer's selected metadata entry.

All interpretation and calculations run locally. Source narration is not included in diagnostic `toString()` output or uploaded. The UI uses account labels rather than printing account identifiers.

## Verification and limits

The domain tests cover matching without shared references, shared-reference pairs, ambiguity, duplicate source observations, money extremes, currencies, time windows, ownership revision changes, narration changes, protected semantics and full identifier boundaries. Host Android database tests use Robolectric and synthetic disposable databases to exercise persistence, stale choices, undo, cross-review conflicts and cross-year cases. They belong to the existing `testDebugUnitTest` CI task; no instrumentation workflow is enabled by this change.

Representative local commands:

```text
./gradlew :app:testDebugUnitTest --tests '*TransferReconciliation*' --tests '*TransferDecisionPolicyTest' --tests '*Statement*' --tests '*Own*Transfer*'
./gradlew :app:assembleDebug :app:lintDebug
```

These tests do not establish real-statement accuracy or device acceptance. This implementation does not add new SMS route parsers, support every bank narration, reconcile fees or loans, recover absent records, or modify existing report definitions. No real financial data is needed to run its tests.
