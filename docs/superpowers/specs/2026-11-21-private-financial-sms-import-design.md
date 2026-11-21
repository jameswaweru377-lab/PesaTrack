# Private, fast financial SMS import

## Intent

PesaTrack must not request personal SMS **body** text while importing financial history. It must remain fully offline, preserve every existing ledger record and all historical deduplication/checkpoint semantics, and reduce repeated import work. The current Android SMS permission still grants broad inbox access; PesaTrack limits what its own code requests.

## Approach selected by owner

**Strict sender-first, with explicit local approval for unknown financial senders.** Other considered approaches were generic text-first scanning (better discovery, unacceptable access to personal text) and selection of accounts before scanning (too restrictive for automatic account discovery). The owner approved strict sender-first on 21 November 2026 and asked for immediate implementation.

## Behavior

1. Read inbox metadata (`_id`, `address`, `date`) first, without the message body.
2. Match addresses against a local offline financial sender directory, including the existing 38-bank directory, supported mobile-money and lender sender names, and any sender the user explicitly approves. Require exact normalized sender names or the existing bank directory's bounded suffix rules, never partial-name/phone similarity. Unknown numeric shortcodes are not trusted automatically.
3. Only for trusted sender IDs, fetch bodies with their exact SMS row IDs **and sender addresses** bound in bounded queries. Reject Unicode/confusable sender addresses rather than silently deleting ambiguous characters. Discard non-transactional/marketing text through the existing financial-message interpretation rules. Legitimate balance/loan observations remain separate from ledger transactions.
4. Keep current chronological ordering, progress, cancellation, replay identity, atomic financial writes, and restart checkpoints. No new permissions, network calls, ledger schema, telemetry, or backups changes.
5. Apply the same sender-first rule to other SMS inbox readers invoked by the app, not only initial onboarding.
6. Keep full history available by default, process the existing date range, and avoid onboarding's second **unconditional** full SMS import. Replay the trusted-sender range if the first pass found pending unowned financial events: bank payments and fees cannot be stored until accounts are confirmed. Preserve ledger idempotence.
7. Make explicit additional sender approval available within the existing import settings. Approval/revocation changes the sender policy revision, invalidates old checkpoints and cancels an ongoing scan; coordinate body reads with revocation. Do not add another mandatory onboarding screen or silently approve a sender based on text content.

## Verification

Use synthetic sender cases: M-PESA, Safaricom, Equity, all shipped message-pack aliases, generic banks, supported loans, personal phone numbers, confusable Unicode, spoof-like suffixes, unknown shortcodes, and explicit sender approvals. Confirm source metadata projections exclude `BODY`, body queries bind approved IDs and addresses, policy revisions invalidate stale checkpoints and onboarding only replays when unowned events need it. JVM/unit, Python source contracts, lint and debug APK checks where available; no instrumentation or emulator execution.

## Constraints and risks

- Financial provider legal-name directories (CBK, SASRA) do not themselves prove the provider's operational SMS sender ID. New aliases require verified evidence or explicit local approval; do not load all names into a trusted sender allowlist.
- Strict privacy can miss a previously unidentified genuine financial shortcode. Keep unknown senders unread; the user may approve its sender name explicitly.
- This cannot narrow Android's system-wide `READ_SMS` grant, only the data PesaTrack requests from the provider. Real-phone performance remains unverified without a device benchmark.
