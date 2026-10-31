# Tracker review safety follow-up

## Intent

Close two P2 review findings on the already-merged Command Center tracker (#514)
without modifying Android application code, financial records, release signing,
or the optional Project synchronization default (off).

## Behavior and constraints

- A single explicitly supported human task state is canonical regardless of
  letter case, so `Task status: paused` has the same effect as `Paused`.
  Conflicting distinct states remain unknown, never guessed.
- A truncated closed-PR history is incomplete evidence: an older merged PR
  might exist without a reciprocal `PR: #...` line in its Issue. In this case
  a read-only report may still show a warning, but the optional Project mirror
  must not overwrite an existing status from that partial snapshot.
- Keep bounded GitHub API requests and existing opt-in safeguards unchanged.

## Approaches

1. **Chosen: conservative guard at the data boundary.** Canonicalize the
   recognized human state and mark capped closed history as incomplete. Simple,
   testable, fail-closed, though large history may block Project sync until
   linkage coverage or collection is improved separately.
2. Resolve all old PR links automatically. More complete reporting, but
   requires unbounded historical discovery and is not safe within this fix.
3. Ignore the cap and trust visible PRs. Fast, but could downgrade a merged
   task; rejected.

## Acceptance

Synthetic regression tests must fail on the pre-fix code and pass afterward.
The full lightweight tracker suite and local secret scan must pass. A future
Project credential/board preview and activation remain owner-controlled.
