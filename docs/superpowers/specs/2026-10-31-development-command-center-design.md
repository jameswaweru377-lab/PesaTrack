# PesaTrack Development Command Center — Design

**Date:** 31 October 2026

**Decision:** James approved combined GitHub Projects + a dedicated PesaTrack ChatGPT chat.
**Implementation gate:** Review the execution plan before enabling repository automation.

## Problem and goal

Several chats concurrently modify PesaTrack. A PR can pass CI, be merged, be
superseded, or still need device testing while another chat retains outdated
information. The owner needs one durable, accurate overview without slower
Android builds, duplicate code, lost audit findings, or unsafe merges.

Success means every accepted audit/work item has an identifiable task record,
known claimed chat, related PRs and commit evidence, CI result, true merge
state, and remaining acceptance tests. Unknown ownership is shown as unknown.

## Approaches considered

1. ChatGPT-only tracker: easy conversational access; unreliable live PR and
   chat ownership state.
2. GitHub-only tracker: persistent evidence, less useful for discussing
   priorities and explaining warnings.
3. **Selected: GitHub Issues + GitHub Projects + dedicated ChatGPT Command
   Center chat.** GitHub owns facts; ChatGPT summarizes current GitHub data.

## System of record

- The repository is `jameswaweru377-lab/PesaTrack`. The GitHub Project is
  `PesaTrack Development Tracker` under the owner's account.
- A canonical GitHub Issue represents one independent task or actionable audit
  finding. Its stable identifier is `PT-<GitHub issue number>`. Preserve source
  audit IDs like `VC-01`, `P0-04`, and report name/date inside the issue.
- The GitHub Project is a view over those issues, not a competing database.
  PRs link to issues using `Refs #<number>`; a PR merge alone never closes the
  canonical issue or proves physical-device acceptance.
- Record title, source, severity, area, plain-language acceptance criteria,
  chat-owner alias, PR link(s), base and head commits, automated CI state,
  merge evidence, device status, final verification, and last checked time.
- Suggested issue workflow: `Untriaged`, `Unclaimed`, `Claimed`, `In progress`,
  `PR open`, `CI failed`, `CI passed`, `Merged — needs acceptance`, `Verified`,
  `Blocked`, `Paused`, `Superseded`. Keep actual PR state independent from
  this task lifecycle. A stale or unknown result is never silently green.
- Use a small `CHAT-<TOPIC>` alias registry. Each chat must claim an existing
  issue, or open one, before editing. Private ChatGPT URLs, account details,
  financial documents and personal data must **not** enter the public repo.
  Project memory may help recall past chat topics, but is not a live lock or
  authoritative activity detector.

## Ownership and conflict policy

1. A chat inspects current `main`, open PRs, current task issues and overlaps.
2. It claims exactly one issue with its alias and a scoped file/feature list;
   a second claimant receives a warning and stops for coordination.
3. Every PR declares task issue ID and owner alias; missing claims are flagged.
4. Compare open PR changed-file lists and task/feature scope; matching paths
   trigger an informational collision warning, not an assumption of conflict.
5. Recheck PR head/base and required checks immediately before any merge.
6. Never auto-merge, delete branches, rebase others' work, close issues on
   merge, alter the financial app, or change APK signing or release policy.

## Automation and safety

- Use GitHub's built-in Project auto-add rules for labeled task Issues.
  Use lightweight GitHub Actions for `pull_request` changes, completion of
  configured CI workflows, an hourly cron at minute 17, and manual dispatch.
  Do not trigger mandatory Android instrumentation or a second build.
- The reconciler reads current source-of-truth GitHub statuses and produces
  a concise audit with timestamps, never pretending a missing CI signal is
  successful. It can update only designated task-issue metadata/comment
  after dry-run tests prove idempotence. Use least-privilege workflow tokens.
- Initial historical import is previewed and deduplicated using source audit
  ID, related PRs, existing issues and current code. Start conservatively with
  actionable findings: unverified entries remain `Untriaged` rather than
  claiming all findings are open or fixed.
- A daily digest is generated from the tracker for the Command Center; an
  on-demand chat query reads the live project/issue/PR state. An hourly
  reconciliation is a safety net, not a five-minute build or app-wide scan.
- GitHub Projects administration may require a one-time owner's UI approval:
  the available repository connector can manage Issues/PRs but does not
  directly expose Project-v2 creation. Do not solicit or display tokens.

## Acceptance

- Existing merged backup PR #478 is correctly distinguished from its
  unverified physical-device restore acceptance.
- The categorization exam PR #489 remains verified CI work without claiming
  categorization behavior changed.
- Recurring v2 PR #501 is resolved from *current* GitHub status, not an old
  conversation snapshot, and live app activation remains separate.
- A deliberately unclaimed issue, conflicting claimant, two PRs touching one
  path, failed CI, stale base and missing CI all produce clear non-destructive
  warnings. Re-running reconciliation creates no duplicate issues/comments.
- A job can fail without modifying PesaTrack code, blocking Android CI, or
  publishing any APK. Private chat identifiers are not exposed.

## Deferred

- Automatic visibility into every live ChatGPT conversation, which is not
  currently available as a trustworthy task-ownership API.
- Autonomous PR merging, automatic branch cleanup and security/release
  workflow changes outside this tracker.
- Automatic migration of every historical audit suggestion into a confirmed
  defect; audit recommendations require evidence and sometimes rejection.
