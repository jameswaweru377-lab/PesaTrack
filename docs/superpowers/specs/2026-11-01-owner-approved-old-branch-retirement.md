# Owner-approved mass retirement of 42 old branches

## Intent

The owner explicitly authorized a one-time mass deletion of 42 old branch refs, rather than indefinite per-branch review. All 42 selected branch tips have last commit dates between 27 September and 2 October 2026. No branch updated in the last week is selected. Deleting only branch refs does not rewrite `main` or the approved UI.

## Approaches

- **Delete all branches by age:** fast but could remove an active protected branch or destroy recoverability; excluded.
- **Exact manifest with recovery tags (selected):** 42 pinned branch names and SHAs, live open PR/protection/movement exclusion, immutable archive tag before exact-lease branch deletion. Preserves historical work for restoration.
- **Continue strict successor patch proofs:** safe for automated inference, but unsuitable for the owner's explicitly authorized archival retirement of forgotten old branch refs.

## Rules

- Dedicated command `/pesatrack-retire-old-42` only from repository owner on canonical Issue #528; no automatic schedule.
- Exact 42 branch names/tips in a reviewed JSON manifest. Cutoff 2026-10-02, at least four weeks before this run.
- Only local Git branches are retired; no changes to `main`, Android code, releases, Equity, instrumentation, or current approved UI.
- Never delete default/protected/open-PR branches or branches whose tips changed since the manifest.
- Before deleting an eligible branch, push a lightweight `archive/retired-20261101/<original-branch>` tag at its original SHA and verify the tag points to that exact commit. Failure stops the batch without deleting that branch.
- Delete the branch via exact SHA `--force-with-lease`, verify GitHub 404, stop on any failure, and report every result.

## Recovery

Restore with `git push origin refs/tags/archive/retired-20261101/<branch>:refs/heads/<branch>` after reviewing exact restored commit. Existing PR discussions remain available.
