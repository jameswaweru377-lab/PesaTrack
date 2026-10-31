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

## Handoff

Leave the same Issue open with an explicit handoff note: old owner, new owner, scope, latest verified PR/commit/CI, unrun tests, and exact next step. Do not automatically close an issue after merge. When device verification is pending, write `Device acceptance: not verified` and say which journey remains.
