# Bank statement account-identity guidance — design

## Intent
An Equity user who has selected a confirmed bank account but has not entered its complete printed account number must be able to resolve the import blocker without leaving Statements. The current optional/last-four-digit guidance contradicts the strict bank archive admission boundary.

## Approaches
- **Selected: inline account-number setup in Statements.** Show a private edit field when the selected bank account has no complete numeric identifier, let the user save it to that same local account, then permit verified PDF review. This avoids a second navigation journey and has a reusable bank-facing contract.
- Redirect to My financial institutions: smaller UI change, but forces the user to abandon a PDF import journey and does not explain the requirement beforehand.
- Auto-adopt the number extracted from a PDF: rejected because the document is evidence, not user ownership confirmation.

## Contract
- Require 8–20 digits for a bank PDF account identifier; accept only whitespace/dashes as input formatting, preserve leading zeros, and never accept a masked/last-four-digit value.
- If absent/invalid, display “Complete bank account number required” beside the bank PDF action. Allow local correction on the selected owned account. Keep the number out of logs, error strings, remote requests and public UI labels.
- Make bank PDF selection unavailable until the selected bank account has a complete identifier; do not block M-Pesa PDF/CSV paths.
- On successful local save, use the updated account for subsequent PDF admission. On failure, explain without showing identifiers.
- Convert `UNKNOWN_ACCOUNT_IDENTITY` and `WRONG_ACCOUNT` admission reasons to understandable user messages; other block reasons remain unchanged.
- Never relax provider, ownership, currency, page/balance or full-number matching, never save unverified PDF rows, and never change Activity, Statements history, or schema.

## Verification
JVM tests for complete/missing/partial/masked/malformed/leading-zero identifiers and privacy-safe error wording; Android CI unit/lint/debug build; phone journey still requires real-device confirmation.
