# Complete owned account identity before transfer review

Date: 2026-11-08
Owner: #601 (current financial-integrity chat)
Scope: legacy transfer review and N2 read-only reconciliation suggestions

## Problem

`StatementOwnedAccountUniquenessPolicy.unambiguous` checks full bank account
numbers (8–16 digits) and Kenyan wallet numbers and eliminates duplicate configured
aliases. However, an account with a missing, masked, malformed, or partial
identifier is skipped while collecting identities, then incorrectly returned
in the final allowed-account map. A shared receipt reference can therefore
make a pair look eligible for transfer review without a complete owned account
identity. This is a review-only false positive, but could mislead user decisions.

Exact reimports are already blocked by the statement archives' content/account
fingerprints, and overlapping monthly history selects one source per month.
No new change to import persistence or report cash calculations is justified
for this step.

## Design

- Treat each owned account's complete validated full identifier as a
  prerequisite for inclusion, not merely as a duplicate-detection input.
- Keep only IDs with successfully validated identities AND without another
  owned record with the same normalized identity and account type.
- Apply this in the existing shared identity policy; both legacy review
  suggestions and N2 proof already call it. Do not duplicate checks in callers.
- Preserve valid unique bank/wallet accounts, raw statement evidence, review
  decisions, the canonical Activity ledger, exact import idempotence and all
  financial totals.
- Keep N2 source-to-ledger admission on HOLD; review-only candidates never
  authorize posting.

## Acceptance

Synthetic JVM regressions demonstrate RED on the old behavior, GREEN after
the one-policy fix. Exercise null, empty, masked, partial BANK identity,
malformed MOBILE_WALLET identity, and retain valid unique-account behavior.
The existing Android CI unit/lint/debug APK workflow must pass at the final
PR head. Skip emulator and instrumentation runs. Physical Redmi verification
remains James's acceptance step.
