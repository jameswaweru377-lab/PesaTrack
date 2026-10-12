# Safaricom statement shared-receipt hardening

## Evidence and privacy

A user-provided private multi-page M-Pesa PDF was inspected **locally** to understand general structural shapes. The document, password, original transaction values, names, account identifiers, phone numbers, and text were never committed or sent to GitHub. Synthetic test dates, receipts, amounts, masked identifiers and descriptions were invented independently. This plan intentionally contains no raw statement fixture or exported diagnostic dump.

The real document demonstrates:
- Transaction details may continue **below** the Completed status and currency cells, including a dash followed by a partially masked identifier.
- Distinct payment, charge, overdraft, wallet and savings-related movements can share the same receipt code.
- A receipt group can carry multiple intermediate balances; requiring every printed line to reconcile against its immediate neighbor is not a correct document-layout invariant.
- Balance transitions involving different account perspectives may remain unproven. Those must not be silently treated as verified wallet activity.
- The existing statement-entry fingerprint inherited a code-only global transaction identity and could collide across distinct statement legs.

## Implemented in this PR

1. Preserve bounded masked identifiers as description text when embedded in wrapped receipt descriptions. Unlabelled numeric monetary continuations and unknown statuses still reject the document.
2. Replace globally unique receipt-code requirements with **distinct account-leg evidence** requirements, rejecting truly identical leg rows.
3. Keep original receipt as immutable evidence reference; assign candidate statement postings an account/row-specific hash-derived canonical identity rather than a code-only fingerprint. A code-only SMS match is not proof that every leg with that reference is already imported; exact account/amount/direction/day evidence is required even for the existing preview duplicate shortcut.
4. Reconcile adjacent **receipt groups** using a consistent balance anchor selected from the candidate observations in each group. Reject noncontiguous codes, date disorder, arithmetic overflow, extreme layout ambiguity, or a low balance proof rate. Flag all unproven group boundaries even when the overall structure is credible.
5. Display disputed receipt groups in the normal statement review, initially **unselected**. User must inspect the signed amount and original source before choosing them. There is no automatic or silent repair of those discrepancies.
6. Preserve M-Pesa-only account confirmation, existing currency requirements, document size limits, atomic import/rollback, and local-first privacy. No new data schema, permissions, telemetry or external PDF processor.

## Physical table reconstruction

- Statement imports now request the already-available optional **local PDF coordinates**. Payslips and other readers retain the text-only default.
- The native isolated PDF service preserves bounded multiline Details blocks in the same ordered IPC stream instead of dropping every block containing a newline. Strict geometry length, rectangle and total-fragment limits still apply.
- For a positively recognized receipt/status block family, the new conservative adapter combines text from the **physical Details region** separately from the Completed / Paid In / Withdrawn / Balance region. Numeric account references and masked identifiers in Details cannot become extra monetary cells after flattened reading-order conversion.
- The adapter proves exact counts of receipt anchors and completed statuses and refuses missing fragments, cross-column boxes, unknown extra money tokens, invalid document boundaries and incomplete positional data. Only then does it pass reconstructed rows through the existing currency/receipt/group audit.
- If Android cannot provide trustworthy complete geometry, the old text parser remains as a strict fallback. No partial financial import or invented cell value is permitted. This fallback may still reject unsupported PDF structures; Android-device acceptance remains pending.

## Acceptance / limits

- Synthetic JVM cases cover masked descriptions, prohibited extra amounts, distinct/malformed shared receipt legs, deterministic fingerprint independence, group-level balance proof, date disorder and clearly inconsistent statements.
- Android CI must confirm unit tests, lint, test-source compilation, sanitized financial lab evaluations and APK build. No instrumentation until explicitly reauthorized.
- Real phone PDF extraction can use a different line/order model than the local PDF inspection. **A successful desktop analysis and passing synthetic tests do not prove on-device import.**
- The full universal-bank PDF geometry engine and automatic bank/loan/SMS semantics are still separate work (issue #366). Imported statement movements still need the existing manual financial classification workflow and cross-source reconciliation. Do not claim automatic transfer/loan classification from this patch.
- Do not copy original PDF or password into public repositories, CI logs, test fixtures or ticket attachments.
