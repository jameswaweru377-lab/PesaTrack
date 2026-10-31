# Premium Profile + Internal Pages — Approved Visual Spec

Approved by James on 31 October 2026 after reviewing six image boards. See the associated design notes in the PesaTrack project Library: *PesaTrack_Profile_Premium_Design_Notes.md*.

## Goal

Make Profile and its destinations feel like one premium, friendly, light-mode PesaTrack experience without altering the money ledger, privacy decisions, backups, application ID or four-tab navigation.

## Board-by-board acceptance

1. **Profile + Personal information**: compact purple identity hero; circular local photo or initials/emoji; prominent edit action; Better money habits between hero and Your account; distinct Your account and Support groups; clear picture/name/optional email; save confirmation *only after* data persists and failed save stays on the form.
2. **Better money habits**: one tappable summary and a detailed local-finance view. Reuse existing HomeFinancialSignalPolicy; show Looking good / At risk only when supported, otherwise Not enough information yet. Explain one reason and a practical next step, never manufacture scores or claim a live bank connection.
3. **Security + Notifications**: polished explanatory header, PIN/fingerprint/lock timing, existing encrypted backup/restore unchanged; grouped reminder toggles reflecting real saved preferences and Android notification permission.
4. **Appearance + Privacy**: light theme selected; preview uses Home/Activity/Plan/More; text size follows the phone and is navigable to system display settings; optional reduce-movement behavior must be real if exposed. Plain-language local storage/permissions/enrichment controls and unchanged statement archive/backup links.
5. **Help + Troubleshooting**: useful search and concise everyday-language guidance; read-only SMS/notification checks and existing diagnostics preserved.
6. **About + Saved feedback**: dynamic installed app version, privacy/terms/financial notices, and a brief success snackbar after a successful profile save.

## Safety rules

- Phone-first local behavior; no new network access, permissions, financial math, data models, signing changes or automatic import behavior.
- Keep all existing usable routes and actions reachable. Do not invent an import notification service or dead switches.
- Use shared Material theme and PesaTrack components, existing spacing tokens, accessible tap sizes, readable text and short copy.
- Current Android minimum is API 24. Biometric controls must reflect availability.
- Existing encrypted backup flow, PIN persistence and security controls retain their behavior.
- All example statuses in design boards are illustrative; actual Profile state is evidence-based.

## Completion criteria

- Focused JVM policy tests pass; Android unit, lint and assemble checks pass in CI.
- Verify Profile scroll/keyboard/back routing and persisted settings on Android device when possible; otherwise explicitly mark device QA unverified.
- Avoid overlapping active PRs, update from current main, merge only after all required workflow checks pass.
