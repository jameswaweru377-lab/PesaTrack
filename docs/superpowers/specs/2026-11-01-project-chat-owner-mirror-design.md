# Project chat-owner mirror: narrow design

## Intent

The existing PesaTrack Development Tracker Project shows a `Chat owner` text
field, but the event-driven GitHub Actions reconciler updates only its separate
`Tracker status` field. A canonical Issue can therefore claim `CHAT-AUDIT-R1`
while the Project owner cell stays blank. GitHub Issues remain authoritative;
the Project is a display, not a new ownership register.

## Approaches

1. **Manually maintain Project owners:** immediately useful for current cards,
   but new claims and handoffs drift. Done as a temporary one-time backfill.
2. **Add a separate webhook or scheduled bot:** needs another credential and
   service, making event ordering and permissions harder to audit.
3. **Extend the existing status mirror (selected):** reuse the trusted,
   event-driven workflow, verified Project identity, read-only reconciliation,
   item mapping and existing least-privilege Projects token. One extra opt-in
   variable guards owner-text writes until a live preview passes.

## Required behavior

- Use only the `owner` already extracted from each labeled canonical Issue.
- Reflect an explicit valid `CHAT-...` alias or `Unclaimed` without interpreting
  it as evidence of a currently running ChatGPT conversation.
- Validate the exact pre-existing `Chat owner` text field and Project identity.
  Never create fields, cards or Issues, and never touch human `Status`.
- Continue the existing bot-owned `Tracker status` synchronization when owner
  mirroring is disabled; an owner-only failure must never silently report
  successful owner sync.
- Do not write when snapshot evidence is incomplete, a relevant Issue card is
  duplicated, the owner is malformed, or the Project field is ambiguous.
  Missing cards are reported and never created; unrelated valid cards may sync.
- Preview owner changes in Actions summaries, without exposing private Issue
  content or changing other Project fields. Enable writes only after the
  explicit repository variable is set to `true`.

## Acceptance

Focused tests cover exact owner changes, no-op, unclaimed, missing/malformed
field, duplicate/foreign/archived Issue cards, GraphQL text mutation shape,
default disabled behavior, and the coexistence of normal status updates.
Android, money, signing, release, and real-device behavior remain untouched.
