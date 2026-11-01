# PesaTrack Development Tracker

The source of truth for implementation work is the [PesaTrack GitHub Issues list](https://github.com/jameswaweru377-lab/PesaTrack/issues), with the existing **PesaTrack Development Tracker** GitHub Project showing the same records. This is a development tracker, not a feature inside the Android app.

One independently verifiable task or actionable audit finding gets one GitHub Issue, identified as `PT-<issue number>`. Keep the original audit ID (for example, `VC-01` or `R4`) in the issue's Source field. Existing Issues should be linked, not recreated. **Do not publish** financial records, private PDF contents, passwords, account identifiers or ChatGPT conversation URLs.

## Lifecycle and evidence

The task status is separate from the PR status, CI status and physical-device acceptance:

| Task status | Meaning |
|---|---|
| Untriaged | Finding imported or reported, not yet confirmed against current source |
| Unclaimed | Accepted task with no active chat owner |
| Claimed / In progress | A chat has claimed scope and is working on it |
| PR open / CI failed / CI passed | Code or documentation is proposed, still not necessarily merged |
| Merged — needs acceptance | GitHub merged the PR; required automatic, device or deployment validation is still missing |
| Verified | All applicable acceptance evidence is recorded or independently established by the narrow tracker-docs policy |
| Blocked / Paused / Superseded | Explicit reason; never silently label fixed |

An issue should record: source, severity (P0–P3), area, acceptance checks, chat owner alias, affected paths, PR links, base/head/merge SHAs, CI run and conclusion, real-device/deployment evidence, last checked time, and remaining risk. A **green CI build alone does not prove physical-device backup restore or a financial import**.

## Safe workflow

1. Before editing, check current `main`, open PRs and existing Issues. Claim one task by writing `Chat owner: CHAT-<TOPIC>` and the intended file/feature scope in its issue. Missing/overlapping claims are a **warning to coordinate**, not an automatic reassignment.
2. In the PR body put `Tracks #<issue>`, `Chat owner: CHAT-<TOPIC>`, affected paths and the CI/device evidence. Do not use an automatic `Closes #` keyword: merging must not close a task that still needs phone acceptance.
3. After each CI or merge event, run the lightweight read-only tracker report. A missing result means **unknown**, not passed. It must not build Android, publish APKs, merge PRs, delete branches or auto-close Issues.
4. Use the guarded audit-import **preview** before adding historical findings. Deduplicate by source audit ID, linked PR and existing Issues. Unverified historical suggestions stay Untriaged.
5. Mark Verified only after all applicable gates are evidenced. A merged tracker-documentation-only task may advance automatically under the policy below; unverified Redmi testing never does.

The checked-in reconciler defaults to **dry run/read only**. Synchronizing issue metadata or applying historical imports requires a separate reviewed approval, the least-privilege token, and a human-checked target list. The GitHub Project board is only a view; GitHub Issues remain authoritative.

For how chats claim work, see [CHAT_OWNERSHIP.md](CHAT_OWNERSHIP.md) and the [shared ChatGPT Project instructions](CHATGPT_PROJECT_INSTRUCTIONS.md). The [audit provenance register](AUDIT_SOURCES.md) and [42-finding import manifest](AUDIT_MANIFEST.json) support a controlled, repeatable preview. Use the [Command Center chat starter](COMMAND_CENTER_STARTER.md) for one coordinating ChatGPT chat.

## Event-driven Command Center updates

[Open the live tracker runs](https://github.com/jameswaweru377-lab/PesaTrack/actions/workflows/development-tracker.yml).

The lightweight **Development Tracker (Read-only)** workflow refreshes after labeled Issue changes, PR changes, a `main` push and Android CI completion. Its existing hourly schedule at UTC minute 17 is a fallback. GitHub Actions scheduling cannot guarantee one-minute reports (five minutes is the shortest supported cron interval, and scheduled starts may be delayed). No Android build, emulator or release job is added to this workflow.

Each ChatGPT/Codex chat must explicitly edit the canonical Issue to claim its task. A GitHub workflow cannot know whether a ChatGPT tab is currently active. The report distinguishes human `Task status: Untriaged/Paused` decisions, the recorded `Chat owner: CHAT-...` alias, open PRs not linked to approved Issues, PR overlap, and exact-commit CI. An auto-merge label is shown as *requested, gates unverified*; the restricted merge workflow still independently requires James's approval and its GitHub App token. The progress counts come from registered Issues, not inferred chat activity. The tracker does not create Issues or approve merges.

## Unattended documentation lane and restricted safe auto-merge

The separate **PesaTrack Restricted Safe Auto-Merge** workflow has two narrowly
bounded lanes. It never reads ChatGPT conversations. Its evidence comes from the
PR, linked tracker Issue, current `main`, overlapping PRs and exact-commit CI.

**Unattended tracker-docs lane:** a PR opened by repository owner
`jameswaweru377-lab` containing **only entirely new** `docs/tracker/*.md` files
may be merged without a fresh approval comment or `pesatrack-safe-automerge`
label. It must still have a non-draft same-repository head, one exact `Tracks #N`
line, one matching `Chat owner: CHAT-...` Issue claim, current main base and merge
base, no file overlaps, successful exact-head Android CI and PR-secrets runs,
   and a separately credentialed GitHub App merge workflow. It rechecks
all of these immediately before one merge. Executable `.py` tests, existing-file
edits and all app/workflow/security/financial/release changes are **excluded**.

**Automated acceptance:** a merged documentation-only task is `Verified`
without a manual `Final verification: verified` line only if its Issue says
`Device acceptance: not applicable`, all associated PRs have complete added-only
tracker-documentation file evidence, Android CI **and** PR Secrets passed for
every exact head, no newer open PR remains, and no explicit withdrawal, blocked
or paused state is present. If old closed-PR history is capped, the Issue must
explicitly name all related PRs (`Related PRs: #N`). CI cannot certify an
Android phone or financial feature; these still need real acceptance evidence.
The summary now shows bounded next-action codes for stale branches, failed CI
and missing evidence. It does not automatically invent a code fix or close an
Issue. Unverified work is left visible for an independent repair agent.

**Legacy owner-approved scope:** entirely new `docs/tracker/*.md` files or
entirely new `tools/tracker/tests/*.py` files. Existing-file edits, Android
tests, app code, financial rules, workflows, security, migrations and releases
remain outside this merge worker. A stale branch, conflicting PR, missing
required check or unknown state is never merged.

**Owner opt-in for an individual PR outside the unattended docs lane:**

1. Link an **open** Issue labeled `pesatrack-tracker`. Put matching `Tracks #<issue number>` and `Chat owner: CHAT-<ALIAS>` lines in the PR, and `Chat owner: CHAT-<ALIAS>` in the Issue. Keep the PR open, non-draft and current with `main`.
2. Once satisfied with the exact commit, add label `pesatrack-safe-automerge` to that PR.
3. As GitHub user `jameswaweru377-lab`, post one Issue-style comment **on the PR**, replacing the placeholder with the full current 40-character **head commit SHA**:

   `PESATRACK SAFE AUTO-MERGE APPROVED: <full PR head SHA>`

   Any new push to the PR invalidates that approval. The bot never adds a label or consent comment itself.

**Owner setup to activate the separate workflow:**

1. Create a dedicated GitHub App, installed **only** for `jameswaweru377-lab/PesaTrack`, with **Contents: read and write** (GitHub requires write to merge), **Issues: read**, **Actions: read**, and **Pull requests: read**. Do not grant access to unrelated repositories. The App has broader repository write authority than the bot's allowed paths, so protect its private key carefully.
2. Under repository **Settings → Secrets and variables → Actions**, set variable `PESATRACK_SAFE_AUTOMERGE_CLIENT_ID` to the App's Client ID, and secret `PESATRACK_SAFE_AUTOMERGE_PRIVATE_KEY` to its private key. Never paste the key into a chat, Issue or PR.
3. Explicitly set repository variable `PESATRACK_SAFE_AUTOMERGE_ENABLED` to
   `true` for the existing guarded worker, and the **separate** variable
   `PESATRACK_UNATTENDED_DOCS_ENABLED` to `true` for owner-authored new-doc
   PRs without per-PR approval. Both default to disabled. Remove either
   value or set it to `false` to stop that lane. Missing App credentials skip
   merges with a warning; no secret is printed. These are one-time repository
   permissions, not per-PR acceptance prompts. Existing-file edits and all
   sensitive Android changes remain excluded.

The automated check runs after Android CI completion, hourly as a fallback,
and when manually dispatched. **It merges at most one PR per run** after
revalidating either the unattended docs criteria or the explicit owner approval,
branch head, checks and current `main`. It uses the GitHub App token instead
of `GITHUB_TOKEN`, preserving the normal `main` push event that publishes signed
APKs. View each result under **Actions → PesaTrack Restricted Safe Auto-Merge**;
the existing hourly tracker remains unchanged. Old unlinked or overlapping PRs
do not become eligible by historical presence alone.

## GitHub Project setup and maintenance

The [PesaTrack Development Tracker](https://github.com/users/jameswaweru377-lab/projects/1/views/1) GitHub Project was verified through its configured status mirror on **31 October 2026**: Development Tracker workflow run #62 successfully applied a `Tracker status` update. Reuse this existing board; do not create a duplicate. Its current access and configuration must still be checked before changes.

1. In the repository, open **Issues → Labels**. Check for the exact label `pesatrack-tracker`; create it if absent. Only explicitly approved tracker Issues should receive this label. The read-only script intentionally reads labeled Issues only so it does not silently conscript unrelated work.
2. On your GitHub profile, open **Projects** and check whether **PesaTrack Development Tracker** already exists. If it does, reuse it. Otherwise choose **New project → Board** and **Create a project** named exactly `PesaTrack Development Tracker` under `jameswaweru377-lab`. Keep access private until you choose otherwise.
3. Add the repository's labeled task Issues to that board. Configure the built-in Project **auto-add** workflow for repository `jameswaweru377-lab/PesaTrack` and label `pesatrack-tracker`; do not auto-close an Issue on a PR merge.
4. Add or confirm board fields for `Status`, `Priority`, `Area`, `Chat owner` and `Last reviewed`. Suggested statuses are listed above; only supported GitHub Issue/PR evidence changes a status.
5. Run historical import **preview** and have the owner approve its `create/link/review` counts against current Issues. No historical Issues are created by the hourly workflow.

The tracker workflow provides event-driven advisory summaries (GitHub Actions → **Development Tracker (Read-only)**). It uses standard Python only, has a four-minute job timeout and does not run Gradle or publish APKs. The report may show no tasks until approved Issues have the label.

### Optional Project v2 status mirror — owner-controlled, activated on 31 October 2026

The Project remains a view. The new `tools/tracker/project_sync.py` can update **only** a dedicated, bot-owned single-select field named exactly `Tracker status`. It never creates a Project, adds cards, edits Issue bodies, changes a human `Status` field or merges a PR. To support Project charts, configure these options under `Tracker status` (exact names): `Untriaged`, `Unclaimed`, `Claimed`, `In progress`, `PR open`, `CI failed`, `CI passed`, `Merged — needs acceptance`, `Verified`, `Blocked`, `Paused`, `Superseded`. Preserve the built-in human `Status` field separately.

1. Verify the existing user Project's identity and copy its **Project v2 node ID** (starts `PVT_`); do not guess or create a duplicate board. Ensure built-in auto-add includes only labeled `pesatrack-tracker` Issues. GitHub Project charts can then group by `Tracker status`.
2. Configure a separate least-privilege Project-authorized credential in repository Actions secret `PESATRACK_TRACKER_PROJECTS_TOKEN`, and an Actions variable `PESATRACK_TRACKER_PROJECT_ID` with the verified node ID. Grant Project access only as required. Do **not** reuse the release-signing secrets or restricted auto-merge GitHub App private key; never paste tokens into chats or Issues.
3. While synchronization is off, choose **Actions → Development Tracker (Read-only) → Run workflow → Preview project**. This reads the configured Project and prints the proposed task numbers without modifying cards. Fix any missing field/option, duplicate card, wrong Project or missing auto-add first. A globally incomplete Issue/PR snapshot fails closed. When *only* old closed-PR history is capped, the mirror preserves stronger existing Project evidence against unsupported downgrades, logs each skipped task, and still applies unrelated safe status updates. A known merged PR with newly failed CI or revoked device acceptance can still move from `Verified` to `Merged — needs acceptance`; a newly linked open PR can also reopen a previously merged or verified task.
4. Only after reviewing that preview, set repository variable `PESATRACK_TRACKER_PROJECT_SYNC_ENABLED` to `true`. The same lightweight event run then mirrors the `Tracker status` field. Unset or change this variable to `false` to stop Project writes immediately.

Dependabot and forked PR events still generate read-only tracker summaries but do not attempt Project-field writes. Their PR-triggered workflows do not receive the separately configured Project Actions secret; trusted Issue, `main` push, Android-CI completion, scheduled, and owner-run workflows continue to synchronize the board.

Only GitHub Projects metadata (task IDs and public task states) is mirrored. The real financial ledger stays on the phone. On 31 October 2026, run #62 proved that the configured credential, board and `Tracker status` field were operational; this is dated evidence, not a guarantee that synchronization remains enabled forever. Inspect the latest workflow before relying on current status.

### Optional Project Chat owner mirror — separate activation

The same verified Project already has a **`Chat owner` text field**. The tracker
can preview the difference between that field and the canonical Issue claim,
then (only with an additional opt-in) mirror aliases such as `CHAT-AUDIT-R1` or
the explicit unclaimed state. It does not infer that a ChatGPT chat is actively
running. The Project remains a read-only *view* of ownership recorded in Issues.

1. Run **Actions → Development Tracker (Read-only) → Run workflow → Preview owner**.
   Check the proposed PT issue numbers and whether the existing `Chat owner`
   text field and task cards were correctly recognized. No Project writes occur
   in the preview. Do not enable it if the preview fails or cites an unexpected
   card/field; investigate first.
2. Only after the preview succeeds, set repository variable
   `PESATRACK_TRACKER_OWNER_SYNC_ENABLED` to `true`. The already enabled
   `PESATRACK_TRACKER_PROJECT_SYNC_ENABLED=true` and dedicated Project token
   remain required. Leaving this new variable unset preserves the status-only
   mirror. Setting it to `false` stops only chat-owner writes.
3. The owner mirror never changes built-in human `Status`, owner assignments,
   Issues, PRs or other Project fields. Unknown ownership is shown as
   `Unclaimed`, not guessed from PR authors or an open ChatGPT window. Missing
   or duplicate Issue cards and an ambiguous/wrong-type owner field stop the
   planned owner update; they never cause auto-creation or reassignment.

On 1 November 2026 the Task Register was manually backfilled with the six
canonical Issues' recorded owner aliases; this is a snapshot, not proof of
automated mirroring. Its visible status and the `PesaTrack Work by Status`
insight chart now use `Tracker status`, not the unrelated built-in `Status`.
The chart is limited to Issues, so separate PR cards are not double-counted as
tasks. The signed-in Project views were checked on 1 November 2026: Workflow
board (view 1) filters to `is:pr`, Task Register (view 2) filters to
`is:issue`, and `PesaTrack Work by Status` remains `is:issue`. That snapshot
contained 29 PR cards (26 open and 3 merged) and 6 canonical tracker Issues.

### GitHub-visible PR intake (1 November 2026)

The existing issue-only tracker **cannot see running ChatGPT conversations**. For visibility, the separate `tools/tracker/pr_inbox.py` now discovers **open GitHub PRs**, including drafts and PRs without a `Tracks #N` link, and adds them as **PR cards** to the existing Project. It uses the already-approved Project token and existing `PESATRACK_TRACKER_PROJECT_SYNC_ENABLED=true` gate; no new credential, Issue, chat owner or Project is created.

The PR card uses the bot-owned `Tracker status = PR open` option, not the human `Status` field. An existing PR card with a different status is not overwritten without exact evidence. When an already-added PR merges, its bot-owned `PR open` status moves to `Merged — needs acceptance`; when a PR closes without merge, that bot-owned open status is cleared, **not** labeled `Superseded`. The Issue card remains the single authoritative task, and Project item counts must distinguish PR cards from task Issues.

Runs are previewable using **Actions → Development Tracker (Read-only) → Run workflow → Preview PR inbox**. GitHub-event runs and the hourly fallback reconcile the PR intake only when the existing Project status sync is enabled. A bad Project ID, missing field, duplicate Project PR card, invalid identity or incomplete inventory blocks writes rather than inventing a mapping.

**Dashboard viewing:** GitHub Project views may have human-configured `is:issue`, status, or other filters. The verified setup uses Workflow board (view 1) with `is:pr` for intake cards and Task Register (view 2) with `is:issue` for canonical tasks. Keep `PesaTrack Work by Status` limited to `is:issue` so separate PR cards do not inflate task progress. No workflow can infer live ChatGPT tab presence or automatically assign a task owner based on PR authorship. For true chat ownership, each implementation chat still claims its Issue as documented in [CHAT_OWNERSHIP.md](CHAT_OWNERSHIP.md). The [PR recovery queue #528](https://github.com/jameswaweru377-lab/PesaTrack/issues/528) remains an advisory triage task, not a merge queue.

### Tracker event burst reliability (1 November 2026)

The GitHub workflow uses a repository-wide concurrency group with `cancel-in-progress: false`. A burst of PR, Issue, `main` or CI events must not interrupt a running Project field/card mutation. GitHub may consolidate pending runs in the same group, while the current run finishes before the next runs. This does **not** add polling, Android builds or new credentials. If the latest run is pending or cancelled, never report a successful Project update without checking a completed job's actual step log.

### Monthly merged-branch cleanup

The separate **Monthly merged branch cleanup** workflow inventories repository branches and pull requests, then proposes deletion only when a branch is unprotected, is not the current default branch, has no open same-repository PR, and its current tip SHA exactly matches a PR head that was merged into the current default branch. Branch age, naming, and a closed-but-unmerged PR are never evidence for deletion. The workflow fetches bounded pages (up to 2,000 PR rows); a page cap, malformed response, permission error, or API failure stops or skips cleanup safely.

An owner-authored comment exactly `/pesatrack-cleanup-apply-10` on canonical PR recovery **Issue #528** also runs the preview followed by at most 10 guarded deletions; all other comments skip the workflow. This is an alternate explicit command channel for the repository owner's existing GitHub connection, not a generic issue-command interpreter. Manual dispatch defaults to a read-only preview. Apply is limited to a manual run by `jameswaweru377-lab` from the current default branch, or the monthly schedule on that branch. Before each deletion, the workflow rechecks the current default branch, candidate SHA, protection, merged-PR evidence and the **complete open-PR list**, including unknown head-repository identities. A changed or uncertain candidate is retained. Deletion uses Git's exact-SHA `--force-with-lease` on precisely one ref; a concurrent push rejects deletion even after the last API read. A bounded post-delete verification checks GitHub up to five times (two-second pauses) and counts the deletion only after a 404 confirms the ref is absent. Reads of the original SHA may be briefly stale. A different SHA, 403/429, malformed response, or persistent presence fails closed; the deletion push is never repeated. No unguarded REST DELETE is permitted. An initial apply attempts at most **10** candidates per run, with one second between rechecks. Remaining candidates are deferred. On an API/lease error, the batch stops without skipping ahead.  A 403/429 is reported with safe rate-limit and accepted-permission headers and stops the current apply loop without retrying. The preview job receives `contents: read` and `pull-requests: read`; only the separate apply job receives `contents: write` and `pull-requests: read`. Neither job can write Issues, Projects, PRs, Actions, or merge PRs. The existing hourly Development Tracker remains read-only and does not run this cleanup.

Use the preview list and its skip counts to review repository state. No-PR branches, protected branches, open work, and closed-unmerged work are intentionally retained. This workflow reports GitHub branch/PR evidence only; it does not detect active ChatGPT chats or verify Android-device acceptance. Check the latest run summary for the live count and result before treating cleanup as complete.
