# Chat ownership and handoff

Use a short public alias such as `CHAT-BACKUP`, `CHAT-STATEMENTS`, `CHAT-PROFILE`, `CHAT-SECURITY`, `CHAT-UI` or `CHAT-AUDIT`. A name is a label, not proof a chat is currently running. A GitHub Issue's most recent explicit claim is the recorded owner; unknown is acceptable.

## Claim format

- `Task: PT-<GitHub issue number>`
- `Chat owner: CHAT-<TOPIC>`
- `Scope: <specific feature and intended paths>`
- `Base commit: <40-character SHA or unknown>`
- `PR: <number or not opened>`
- `Last checked: <UTC ISO-8601 timestamp>`

If another owner already claimed the issue, stop and coordinate in GitHub rather than overwriting its claim. Two PRs touching a file require review but are not proof of a semantic conflict. Do not publish private ChatGPT links, user account numbers, personal financial data, receipts, PDFs or secrets in Issues, PRs or Actions logs.

## Connection to live chats

There is no trustworthy GitHub event that means a ChatGPT chat opened, started sampling, or stopped. A recorded `CHAT-...` alias is a **claim**, not evidence the chat is running at this instant. Give each implementation chat the [ChatGPT Project instructions](CHATGPT_PROJECT_INSTRUCTIONS.md). The chat claims its Issue with the authorized GitHub connector **before writing code**. Editing or labeling a `pesatrack-tracker` Issue triggers a lightweight tracker report; PR/main/CI events also refresh the report. The hourly cron is only a fallback; one-minute GitHub Actions schedules are unsupported.

Keep the claim until explicit completion, pause or handoff. The Command Center reads current GitHub state when asked; its report is a timestamped snapshot, not a live presence service.

The GitHub Project's `Chat owner` text column can be mirrored from canonical
Issue claims only after a successful GitHub Actions `Preview owner` run and
separate `PESATRACK_TRACKER_OWNER_SYNC_ENABLED=true` opt-in. Updating that
column manually is a snapshot; it does not move ownership away from the Issue.
The mirror writes an exact `CHAT-...` alias or `Unclaimed` and changes neither
the built-in Project `Status` nor anyone's GitHub account assignment.

## Handoff

Leave the same Issue open with an explicit handoff note: old owner, new owner, scope, latest verified PR/commit/CI, unrun tests, and exact next step. Do not automatically close an issue after merge. When device verification is pending, write `Device acceptance: not verified` and say which journey remains.
