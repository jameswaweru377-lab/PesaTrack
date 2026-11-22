# Provider-neutral owned-transfer evidence — 22 November 2026

## Purpose

Identify completed transfers between the user's confirmed accounts without requiring repeat manual classification. This applies to any recognised Kenyan financial sender using supported financial-message shapes, not a hard-coded bank list.

## Safeguards and decisions

- A bank or wallet name in a message identifies a provider, **not** a specific owned account. Both endpoints must already be confirmed by the user; ambiguous same-provider account mappings remain unresolved.
- For bank-to-mobile SMS containing a recipient phone, compare the **full normalised phone** with the explicitly owned destination wallet identifier. Exact match permits automatic owned transfer when the source is confirmed and the message is completed. Different full phone means an external payment, never an own transfer. Missing/invalid number means unresolved unless independently corroborated.
- The saved full name supports reordered three names, two of three names and exact two-name profiles. It is corroborating evidence and a contradiction guard, never sole proof of ownership. Missing beneficiary names do not defeat exact account-identifier evidence.
- Use the existing transfer interpretation, account consent, transaction reference, duplicate/mirror and reconciliation pathways; preserve original references and protect fee, loan, refund, reversal, pending and failed roles.
- Imported statements may contribute **read-only** directed account-route evidence. Existing N2 statement-to-ledger posting hold remains in force; no statement row is inserted into Activity or silently changes totals.
- Do not read personal SMS, introduce provider-specific branches, permissions, cloud dependencies, schema changes, or changes to approved screens.
- Replay/import must be idempotent; transactions already decided by the user are not overwritten.

## Acceptance

Synthetic examples cover diverse institution titles, phone aliases, two-/three-name variants and shuffled names; positive bank-to-owned-wallet transfers, non-owned recipient payments, missing numbers, conflicting names, duplicate account identifiers, other-bank transfers, pending notices and separate transaction/M-Pesa references. Statement tests cover directed full wallet identities and reject masked, unmatched, ambiguous and protected rows.

Verify targeted JVM tests, Android CI build/lint/unit tests without instrumentation, and the PR before merging to `main`. Real-device correctness remains unverified until owner testing.
