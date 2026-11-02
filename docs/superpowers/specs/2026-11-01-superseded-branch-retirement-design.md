# Guarded superseded branch retirement

## Intent

Retire demonstrably redundant closed/unmerged PR branches in owner-authorized batches without altering app code, approved UI, or independent active work. A closed PR stays available on GitHub as review history.

## Approaches

- **Age/title-only cleanup:** fast but rejects unique code without proof; excluded.
- **Manually delete all closed branches:** difficult to audit and not safe under concurrent development; excluded.
- **Reviewed manifest plus live mechanical proof (selected):** commit-pinned branch/old-PR/new-PR triplets, file patch containment, successor commit in current main, exact-tip lease deletion.

## Rules

- Explicit manifest of at most 30 entries; no wildcard selection.
- Old PR must be closed and unmerged, from this repository to default; its head SHA must match the current branch.
- Successor PR must be merged into default, with its merge commit contained in current main. Every old PR changed file must have the same exact Git blob at the successor PR's final head, its merge commit (preventing conflict-resolution drops), and current main (preventing later reversions). Any intervening edit, removal, or rename defers retirement.
- All changed `+` and `-` lines of the old PR must occur in successor's complete GitHub PR file patch at the same path, with occurrence counts and identical contiguous hunk bodies including unchanged context. Context-free hunks, missing/truncated patches, or altered surrounding functions fail closed. This strict test intentionally defers many plausible replacements.
- Exclude default, protected, moved, and any open-PR branch. Independently fetch fresh GitHub state before each deletion and enforce exact SHA with Git lease.
- Use a dedicated owner-only Issue #528 command; existing schedule and ordinary cleanup stay unchanged.
- Equity, instrumentation, and current approved UI are not placed in the manifest.

## First candidate

Closed/unmerged PR #200 at `3c6585383aed5d33e1f67932cf1d6fe8c9d67259` has matching changed-line membership within merged PR #201, but later changes to its files on main cause the current-tree preservation gate to **defer deletion**. Exact context-hunk proof may additionally exclude it. This is an expected conservative result, not a failure. Other older branches with non-contained patches are deferred as well.
