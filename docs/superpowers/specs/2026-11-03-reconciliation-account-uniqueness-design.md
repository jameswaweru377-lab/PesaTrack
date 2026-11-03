# Fail-closed account uniqueness in transfer review

## Intent

An account identifier is evidence of one financial account, not merely a label. If two separately configured owned accounts carry the same complete identifier, a shared receipt must not establish a transfer between them or between either duplicate and a third account. A mistaken match would make the review misleading even though it does not currently post to Activity.

## Scope

- Harden the existing **non-posting Statements transfer-review candidates**; no new matching or automatic financial posting.
- Preserve all source observations and existing user decisions. Ambiguous ownership identities withhold affected candidates until the user resolves account identities.
- Treat two complete bank identifiers as identical when their digits match after permitted punctuation removal. Normalize Kenyan wallet local/international phone aliases using the existing phone policy. Compare identifiers only within the same account type; do not infer bank/provider identity from a name.
- Missing, masked or malformed identifiers cannot create a new positive identifier match. Existing reference-based review for otherwise explicitly owned accounts remains unchanged.
- No database/schema, backup, UI design, remote service, financial totals or release workflow changes.
- Full posting/assurance changes remain held by N2 #537; emulator/instrumentation are excluded by current owner instruction.

## Approaches

1. **Selected:** Exclude duplicate full account identities from the set of accounts allowed to produce transfer candidates. Conservative, read-only, and compatible with existing ownership revision checks.
2. Merge duplicate account records automatically. Rejected: would mutate user data and could incorrectly combine distinct bank accounts with coinciding local numbers.
3. Leave duplicate accounts eligible but display a warning. Rejected: still allows the underlying review to claim a false two-account transfer.

## Proof

A shared reference between two account records with the same normalized full bank identity must produce no transfer pair. Two wallet records using different Kenyan phone presentations must likewise be withheld. Valid distinct account IDs and identifiers continue producing the existing review candidate.
