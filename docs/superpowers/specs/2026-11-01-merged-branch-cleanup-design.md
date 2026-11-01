# Merged Branch Cleanup Design

## Intent

PesaTrack has hundreds of remote branch refs. This workflow will automatically remove only refs whose current head is already represented by a pull request merged into `main`, reducing stale repository clutter without discarding unmerged work.

## Goals

- Inventory same-repository branch refs and pull-request heads using bounded pagination.
- Delete a branch only when its current SHA exactly matches the head SHA of a PR merged into `main`, no open same-repository PR uses that branch name, the branch is not protected, and it is not the default branch.
- Re-fetch the branch, PR evidence, open-PR state, default branch, and protection state immediately before deleting each ref. A mismatch or incomplete API result skips deletion.
- Run automatically monthly. A manual run previews by default; an apply run is restricted to the repository owner.
- Report totals and each deletion/skip reason in the Actions job summary. Do not write Issues or Projects.
- Keep the existing Development Tracker read-only and the Restricted Safe Auto-Merge workflow unchanged.

## Non-goals

- Never delete a branch with an open PR, a branch associated only with a closed-unmerged PR, or a branch with no matching merged PR head.
- Do not close, merge, rebase, force-push, rename, or edit any PR or Issue.
- Do not infer that an unmerged branch is abandoned from age, naming, inactivity, overlap, or author.
- Do not infer live ChatGPT/Codex activity or change task ownership.

## Approaches

1. **Add deletion to the existing tracker workflow.** Rejected because that workflow runs for PR and Issue events and is deliberately read-only; a write token there would widen exposure.
2. **Create a separate monthly cleanup workflow with a job-scoped `GITHUB_TOKEN`.** Selected. Only this workflow receives `contents: write` and `pull-requests: read`; it has no Issues, Project, Actions, or PR write permission. Schedule runs apply the strict merged-only predicate; manual runs preview unless the owner explicitly selects apply.
3. **Reuse the restricted-merge GitHub App token.** Rejected because branch cleanup is a separate authority and should not share the merge worker's credential.

## Safety and failure behavior

- Resolve the repository's current default branch; never hardcode a deletion target for it.
- Use the branch API's protection signal and fail closed if branch/protection data is missing or malformed.
- Match PR repository, `head.ref`, `head.sha`, `merged_at`, and `base.ref == default_branch`; match the current branch tip SHA exactly.
- Treat pagination caps, HTTP errors, timeouts, malformed rows, or a state change during recheck as a no-delete result for the affected run or branch.
- Delete only the exact `refs/heads/<encoded branch name>` ref; never force-delete. Record the branch name, SHA, and merged PR number, not tokens or response bodies.
- Serialize runs so two cleanup attempts cannot race.

## Acceptance

- Unit tests prove that merged exact-head branches qualify and that default, protected, open-PR, closed-unmerged, unmatched, head-moved, malformed, and incomplete-inventory cases never delete.
- Tests prove the state is rechecked before deletion and only the expected ref is sent to the delete endpoint.
- Workflow contract tests prove the monthly schedule, owner-only manual apply, preview-by-default dispatch, and least-privilege permissions.
- Exact-head CI passes before merge. The first production run is previewed, then the owner-authorized strict cleanup is applied; the result is verified against the live branch list.
- No Android, release, signing, app permission, or financial behavior changes.

## Decisions left to implementation

- Use a monthly schedule on the first day at a quiet UTC hour, with the repository's existing concurrency style.
- Keep the complete candidate list in the Actions summary so the first run is reviewable without expanding Project/Issue permissions.
- Recompute candidate eligibility immediately before every deletion rather than trusting the initial inventory.
