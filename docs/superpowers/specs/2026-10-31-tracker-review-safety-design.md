# Tracker review safety follow-up

## Intent

Close two P2 review findings on the already-merged Command Center tracker (#514)
without modifying Android application code, financial records, release signing,
or the optional Project synchronization default (off).

## Behavior and constraints

- A single explicitly supported human task state is canonical regardless of
  letter case, so `Task status: paused` has the same effect as `Paused`.
  Conflicting distinct states remain unknown, never guessed.
- A capped closed-PR history is incomplete *historical* evidence: an older
  merged PR might exist without a reciprocal `PR: #...` line in its Issue.
  The optional Project mirror must preserve higher-confidence existing status
  evidence when the capped history would otherwise downgrade it, while safe
  unrelated updates continue. Skipped task numbers appear in workflow logs.
- Keep bounded GitHub API requests and existing opt-in safeguards unchanged.

## Approaches

1. **Chosen (refined after owner approval): per-task evidence guard.**
   Canonicalize recognized human states. Preserve stronger existing Project
   evidence when closed PR history is capped, but allow unrelated progress
   updates and explicit human pauses. This keeps the already-active Project
   mirror useful without letting absent historical PRs erase verified work.
2. Resolve all old PR links automatically. More complete reporting, but
   requires unbounded historical discovery and is not safe within this fix.
3. Ignore the cap and trust visible PRs. Fast, but could downgrade a merged
   task; rejected.
4. Block all Project writes whenever old PR history is capped. Safe against
   downgrades, but would stop the already-working mirror in a mature repository;
   rejected after observing the live 31 October 2026 workflow.

## Acceptance

Synthetic regression tests must fail on the pre-fix code and pass afterward.
The full lightweight tracker suite and local secret scan must pass. A future
Project credential/board preview and activation remain owner-controlled.
