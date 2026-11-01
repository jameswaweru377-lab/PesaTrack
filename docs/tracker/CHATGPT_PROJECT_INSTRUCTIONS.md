# PesaTrack ChatGPT Project — task-claim instructions

Paste the instructions below into the **existing PesaTrack ChatGPT Project instructions** so every implementation chat follows the same handoff. This file is not an API integration or a live ChatGPT conversation detector.

---

You are working on `jameswaweru377-lab/PesaTrack`. GitHub Issues, PRs and exact-head CI are the development source of truth. The GitHub Project is a view of those Issues. ChatGPT conversations do not expose a reliable live-running roster to GitHub.

**At the first implementation action in a chat:**

1. Look up latest `main`, open PRs, and the existing `pesatrack-tracker` Issues. Find the one Issue matching the requested task. Do not create a duplicate. If no Issue exists and the user has authorized implementation, create one concise canonical Issue with the `pesatrack-tracker` label, source, acceptance criteria, affected paths and `Device acceptance: not verified` (or `not applicable` for developer-only work).
2. Claim it **explicitly through the authorized GitHub tool** by setting one `Chat owner: CHAT-<TOPIC>` line and `Task status: Claimed` in that Issue body. Preserve unrelated body content. Record exact intended paths, current base SHA and `Last checked: <UTC ISO time>`. Do not infer ownership from a chat title or memory.
3. If another alias owns it, or another PR touches intended paths, warn and stop for coordination; do not overwrite the other claim or branch. A missing connection means ownership is **unverified**, not automatically claimed.
4. Work in a fresh scoped branch. Put `Tracks #<issue>` and the **same** `Chat owner: CHAT-<TOPIC>` line in every PR. Record test/CI evidence and device acceptance separately. A green build is not proof of a real-phone flow.
5. At important checkpoints (PR opened, CI failed/passed, merged, paused or handed off), update the Issue or PR with exact evidence and a standalone `Related PRs: #N` line listing each linked PR. Only mark `Final verification: verified` after required acceptance has actually passed. The narrow tracker-documentation-only policy can instead derive Verified from complete CI and file evidence; it cannot accept Android/device/financial work. Merge does not automatically close an Issue.

**Command Center and restricted auto-merge:** Read `docs/tracker/README.md`, the latest tracker workflow summary and live GitHub records. Show untriaged, unclaimed, claimed, PR/CI, merged-awaiting-acceptance and verified separately. Owner-authored added-only tracker Markdown can enter the unattended lane without a per-PR label/comment **only after** repository variables `PESATRACK_SAFE_AUTOMERGE_ENABLED=true` and `PESATRACK_UNATTENDED_DOCS_ENABLED=true` explicitly activate it; the strict Issue, current-head CI, main freshness, overlap and GitHub App gates still apply. `pesatrack-safe-automerge` remains a request for the legacy signed-approval lane for executable tracker test additions. Never add that legacy approval on James's behalf without his explicit request.

Never publish private ChatGPT conversation URLs/IDs, financial SMS, statements, phone/account numbers, credentials or signing keys. Do not auto-create task ownership based only on chat activity. When GitHub access is unavailable, explain what is not connected and ask for the supported authorization instead of guessing.

---

**One-time setup:** An owner must paste these instructions into the ChatGPT Project. No repository workflow can read, count, or automatically claim an active ChatGPT conversation. Each authorized chat can *perform the claim* when it starts implementation; the Issue edit then triggers the event-driven GitHub tracker.
