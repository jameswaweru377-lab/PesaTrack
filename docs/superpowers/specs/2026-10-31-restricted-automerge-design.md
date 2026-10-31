# Restricted PesaTrack auto-merge — approved first stage

## Intent

Catch forgotten, previously approved small PRs without reviving old app behavior. The existing read-only Development Tracker remains untouched.

## Safety contract

- This is **opt-in per PR**, not a bot that guesses the owner's consent from ChatGPT memory. A PR needs the label `pesatrack-safe-automerge` and an exact-head approval comment written by GitHub user `jameswaweru377-lab`: `PESATRACK SAFE AUTO-MERGE APPROVED: <40-character head SHA>`.
- Approval is invalid as soon as the PR head changes. New approval is required for the new SHA.
- The PR must target `main`, be open and not draft, come from this same repository, and be based directly on the **current** main commit. It must carry a `Tracks #<issue>` reference and `Chat owner: CHAT-<ALIAS>` in its body; the tracked Issue must still be open and owned by that alias.
- The PR may only **add new files** with `.md` extension in `docs/tracker/` or `.py` extension in `tools/tracker/tests/`. Android tests are deliberately excluded in stage one: they can execute during release CI. Changes to an existing file, workflows, AGENTS.md, README, production code, release signing, secrets, security policy, migrations and backup code are never eligible. Path traversal, renames, deleted files and oversized/unbounded file lists fail closed.
- The latest pull-request runs for both `Android CI` and `Security - PR Secrets` must have successfully completed on the exact PR head SHA. Unknown, skipped, cancelled, pending or failed results are not safe.
- Detect overlap with **all** currently open PR file paths; if coverage is incomplete or any path overlaps, do not merge.
- Immediately before merge, re-fetch the current PR, current main commit, and approval; verify the exact head and base SHA still match. Send the exact expected head SHA in the merge request. Never force-push or bypass branch protection.
- Process at most one merge per run. Report only PR numbers and short reason codes (no raw Issue or chat content). A missing permission, API error or unexpected shape is a stop, not permission to guess.

## Design decision

Use an isolated, trusted-main GitHub Action triggered after Android CI, on an hourly schedule, or manually. A separate Python module performs GitHub REST read checks and the guarded merge. The existing hourly advisory workflow stays read-only and unchanged. No ChatGPT transcript reader is added.

**Critical release safeguard:** GitHub suppresses normal push-triggered workflows when its default `GITHUB_TOKEN` makes a merge. PesaTrack publishes signed update/full APKs on a normal `push` to `main`, so this workflow **must never merge using `GITHUB_TOKEN`**. It instead requires an explicitly configured GitHub App installation token scoped only to `PesaTrack`, with **Contents: write** (required by the merge PR API) plus Issue/Actions/Pull requests read. The GitHub App's write scope is broader than the file policy, which is why code-side guardrails and explicit owner opt-in are essential. The owner must separately opt in via `PESATRACK_SAFE_AUTOMERGE_ENABLED=true`; without this variable and App credential, the merge job is inactive. The code cannot create credentials or grant itself write access.

## Considered approaches

1. Report forgotten PRs only: no merge risk, but James still has to remember to finish each one.
2. **Selected:** one narrowly approved, addition-only PR per run after exact-head checks. It can complete well-understood work without changing app behavior.
3. Auto-merge any green PR: rejected because green CI cannot establish that a stale branch still matches approved app behavior.

## User-facing expectations

- An enabled candidate reports `eligible`, a short blocked-reason code, or `merged` in the Action summary. No secret or private chat content is printed.
- An old PR or a chat saying “approved” without an exact GitHub approval is **not** auto-merged; the Command Center can still flag it for a human review.
- The bot cannot read all ChatGPT conversations by itself. Handoffs stay in GitHub Issues/PRs, without private chat URLs or personal finances.

## Deployment boundary

The automated merge has no effect until this separate PR is reviewed, its own CI passes, and it is merged to `main`, **and** the owner has configured the GitHub App and enabled the variable. Current open PRs are not opted in by default. Do not apply the new opt-in label or approval comment to any existing PR during implementation.
