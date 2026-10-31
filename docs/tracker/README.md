# PesaTrack Development Tracker

The source of truth for implementation work is the [PesaTrack GitHub Issues list](https://github.com/jameswaweru377-lab/PesaTrack/issues), with a future **PesaTrack Development Tracker** GitHub Project showing the same records. This is a development tracker, not a feature inside the Android app.

One independently verifiable task or actionable audit finding gets one GitHub Issue, identified as `PT-<issue number>`. Keep the original audit ID (for example, `VC-01` or `R4`) in the issue's Source field. Existing Issues should be linked, not recreated. **Do not publish** financial records, private PDF contents, passwords, account identifiers or ChatGPT conversation URLs.

## Lifecycle and evidence

The task status is separate from the PR status, CI status and physical-device acceptance:

| Task status | Meaning |
|---|---|
| Untriaged | Finding imported or reported, not yet confirmed against current source |
| Unclaimed | Accepted task with no active chat owner |
| Claimed / In progress | A chat has claimed scope and is working on it |
| PR open / CI failed / CI passed | Code or documentation is proposed, still not necessarily merged |
| Merged — needs acceptance | GitHub merged the PR; required human/device/deployment validation remains |
| Verified | All acceptance evidence is recorded |
| Blocked / Paused / Superseded | Explicit reason; never silently label fixed |

An issue should record: source, severity (P0–P3), area, acceptance checks, chat owner alias, affected paths, PR links, base/head/merge SHAs, CI run and conclusion, real-device/deployment evidence, last checked time, and remaining risk. A **green CI build alone does not prove physical-device backup restore or a financial import**.

## Safe workflow

1. Before editing, check current `main`, open PRs and existing Issues. Claim one task by writing `Chat owner: CHAT-<TOPIC>` and the intended file/feature scope in its issue. Missing/overlapping claims are a **warning to coordinate**, not an automatic reassignment.
2. In the PR body put `Tracks #<issue>`, `Chat owner: CHAT-<TOPIC>`, affected paths and the CI/device evidence. Do not use an automatic `Closes #` keyword: merging must not close a task that still needs phone acceptance.
3. After each CI or merge event, run the lightweight read-only tracker report. A missing result means **unknown**, not passed. It must not build Android, publish APKs, merge PRs, delete branches or auto-close Issues.
4. Use the guarded audit-import **preview** before adding historical findings. Deduplicate by source audit ID, linked PR and existing Issues. Unverified historical suggestions stay Untriaged.
5. Manually mark Verified only after all applicable gates are evidenced. Unverified Redmi testing is recorded explicitly.

The checked-in reconciler defaults to **dry run/read only**. Synchronizing issue metadata or applying historical imports requires a separate reviewed approval, the least-privilege token, and a human-checked target list. The GitHub Project board is only a view; GitHub Issues remain authoritative.

For how chats claim work, see [CHAT_OWNERSHIP.md](CHAT_OWNERSHIP.md). The [audit provenance register](AUDIT_SOURCES.md) and [42-finding import manifest](AUDIT_MANIFEST.json) support a controlled, repeatable preview. Use the [Command Center chat starter](COMMAND_CENTER_STARTER.md) for one coordinating ChatGPT chat.

## One-time GitHub setup (owner action pending)

The **PesaTrack Development Tracker** GitHub Project board is **not verified as created**. The repository connector can read Issues and PRs, but does not currently expose Project v2 creation or configuration. Do not present a made-up Project URL or duplicate a board that might already exist.

1. In the repository, open **Issues → Labels**. Check for the exact label `pesatrack-tracker`; create it if absent. Only explicitly approved tracker Issues should receive this label. The read-only script intentionally reads labeled Issues only so it does not silently conscript unrelated work.
2. On your GitHub profile, open **Projects** and check whether **PesaTrack Development Tracker** already exists. If it does, reuse it. Otherwise choose **New project → Board** and **Create a project** named exactly `PesaTrack Development Tracker` under `jameswaweru377-lab`. Keep access private until you choose otherwise.
3. Add the repository's labeled task Issues to that board. Configure the built-in Project **auto-add** workflow for repository `jameswaweru377-lab/PesaTrack` and label `pesatrack-tracker`; do not auto-close an Issue on a PR merge.
4. Add or confirm board fields for `Status`, `Priority`, `Area`, `Chat owner` and `Last review`. Suggested statuses are listed above; only supported GitHub Issue/PR evidence changes a status.
5. Run historical import **preview** and have the owner approve its `create/link/review` counts against current Issues. No issue creation or Project writes are wired into the hourly workflow.

After the label and board exist, the tracker workflow provides periodic advisory summaries (GitHub Actions → **Development Tracker (Read-only)**). It uses standard Python only, has a four-minute job timeout and does not run Gradle or publish APKs. The report may show no tasks until approved Issues have the label. The Project URL must be recorded only after actually verifying it.
