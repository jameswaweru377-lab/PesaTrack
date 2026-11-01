# Guarded ancestor-only branch cleanup

## Intent

Retire stale branch refs whose exact current tip is already an ancestor of the
current `main`, even when no merged PR has the same historical head. This is a
separate owner-triggered lane, not an expansion of monthly cleanup.

## Options considered

- Delete based on age or branch name: rejected; unique code can be lost.
- Treat a one-time compare result as deletion permission: rejected; branches
  can move between inventory and deletion.
- **Chosen:** opt-in ancestry mode, fresh compare both in preview and immediately
  before lease deletion, preserving the existing open-PR/protection/default
  checks, exact-SHA lease, bounded batch and post-delete confirmation.

## Boundaries

- The existing merged-PR lane and its command remain unchanged.
- The new exact owner command is `/pesatrack-cleanup-ancestors-apply-10` on
  recovery Issue #528; it must run only on the repository default branch.
- Compare immutable branch SHA against the current default SHA. Only `ahead`
  or `identical` with `behind_by == 0` in the branch-to-main comparison qualifies.
- Missing, malformed, ambiguous, paginated, rate-limited or failed API evidence
  stops safely. Open/unknown PR heads, protection and changed branch tips block.
- At most ten deletions per run. No Android, signing, financial or UI changes.
- Closing an unmerged PR does not by itself make its branch eligible.

## Acceptance

Focused tests show a true ancestor eligible, a diverged branch ineligible,
an open PR protected, and changed main/branch or ambiguous responses refusing
deletion. The workflow owner/Issue/command gate is tested. CI must pass before
merge; live deletion requires a separate successful post-merge owner command.
