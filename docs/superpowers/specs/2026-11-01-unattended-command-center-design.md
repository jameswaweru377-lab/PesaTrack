# Unattended Command Center — evidence-first design

## Intent

James should not need to approve every routine PR or manually move completed
development tasks through a GitHub board. The Command Center should operate
on GitHub events and the hourly fallback, while never replacing an unverified
financial, security or device outcome with a success label.

## Five steps

1. **Claim and reconcile:** use explicit canonical Issues and matching PR
   ownership. Never invent chat ownership or merge old, unlinked PRs.
2. **Test and review:** require successful, exact-head Android CI and PR-secrets
   runs. The existing branch ruleset independently enforces `build`, `secrets`
   and a current base; an unknown, pending, or failed result stops automation.
3. **Merge:** allow a *narrow autonomous lane* for same-repository, owner-authored,
   new `docs/tracker/*.md` additions with an open matching Issue and no file
   overlap, even without a per-PR owner approval comment. Retain the existing
   exact-SHA owner opt-in for new tracker test files. Do not auto-merge modified
   files, workflows, Android code, financial data, migrations, security or
   releases. An installation token, configured repository App client ID and
   latest-main recheck remain mandatory; merge at most one PR per run.
4. **Verify:** automatically mark *only* merged, documentation-only tracker
   additions `Verified` when the complete snapshot confirms all related PRs
   merged and the exact-head checks passed. This is a GitHub-documentation
   acceptance decision, not device or financial acceptance. Explicit blocked,
   paused and superseded states, missing evidence, ambiguous PR links, or an
   explicit requirement for device review prevent auto-verification.
5. **Handle exceptions:** each task report includes machine-actionable reasons
   for why it remains unverified; the workflow summary exposes them without
   auto-closing Issues, changing source code or requesting unsafe privileges.
   Failed checks and stale/overlapping branches remain visible for a repair
   agent to address in a separate reviewed PR.

## Tradeoffs and non-goals

Compared with blanket green-CI merging, this lane is slower to cover all
PesaTrack features, but cannot silently override a later financial or backup
fix. Compared with per-PR signed approval, it removes repetitive approval for
its small, independently reviewable scope. It does **not** claim autonomous
programming or physical Redmi testing, which GitHub Actions alone cannot do.
Future expansion requires a separate policy and actual emulator/device evidence.

## Safety invariants

- Main remains protected and is never force-pushed or directly updated.
- The merge job checks out trusted default-branch code, never runs PR payloads
  with privileged credentials, and uses a separately scoped GitHub App token.
- No fork, draft, stale, conflicting, unclaimed, CI-red/unknown or modified-file
  PR enters the autonomous lane. Every merge targets its exact checked SHA.
- Human `Status`, Project cards, Issue bodies and finance data remain untouched.
- `Verified` must be withdrawn if new evidence shows CI failure, an open PR,
  explicit review requirement or incomplete history.
- Existing manual opt-in and all fail-closed guards are preserved for test-only
  additions until this smaller lane has real-world evidence.

## Rollout boundary

The code change alone does not prove live automatic merging. A configured
GitHub App (client ID and protected private-key secret) and a fresh eligible PR
are still necessary. The worker skips cleanly when credentials are missing;
`PESATRACK_SAFE_AUTOMERGE_ENABLED=false` is an explicit kill switch. Do not
turn on broader path categories or auto-release from this implementation.
