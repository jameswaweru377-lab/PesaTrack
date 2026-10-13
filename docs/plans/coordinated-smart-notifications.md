# Coordinated smart notifications — 13 October 2026

The current task authorizes issues #329, #320, #326 and #334, superseding their earlier backlog-only status. Extend PR #360 rather than creating a second Activity worker. Starting main: 54baf19. Open PRs at audit: dependency updates #371, #370, #369, #259, #258, #257, #256, #254. Recent statement/PDF changes are outside this branch.

## Design

One local coordinator reads canonical snapshots without maintenance or financial writes. Account candidates require existing high-confidence balance/transfer evidence and the same ownership presentation filter as Activity. Stable entity deep links open only still-pending review. Activity retains its minimum of two rows and existing setting/channel/work identity.

They Owe Me candidates require a unique normalized Kenyan phone, valid current/former receivable history, canonical recent transaction after opt-in, live SMS/notification source, and existing journal link eligibility. Names alone, historical imports, transfers, refunds, reversals, ambiguous duplicates, fees and already-decided events are excluded. Notification actions open a review sheet; the existing journal entry dialog still requires confirmation. Gift/other is notification metadata only. No notification can create financial records.

One shared Android banner per rolling 24 hours, account before Activity before payment before discovery. Same review evidence pauses seven days; dismissal persists until evidence changes. A payment is shown once with a per-person daily cooldown. Discovery is once per feature lifetime, in-app by default, stops on visit/use/dismiss; phone delivery separately opt-in. Quiet hours: 20:00–08:59 local. Import settling: two hours. All categories off for phone by default; existing Activity choice is retained. Quiet summary and off modes, category switches, snooze and Android settings coordinate preferences without changing existing commitment reminders.

Revalidate under the canonical store lock before dispatch and on tap. Ledger revisions cancel changed/resolved posted content even in foreground or quiet hours. No posting while foreground, processing or recovery failure. Existing periodic worker remains; revision-driven one-time evaluation uses the same worker after settling. Private lock-screen copy contains no names, amounts or identifiers. Android controls heads-up behavior.

## Coordinated implementation stages

- [x] Policy and synthetic JVM regressions: shared budget, priorities, dismissal, clocks, completion, unique phone/live-event gating.
- [x] Canonical candidate reader, persisted preferences and existing worker integration; lifecycle/revision cancellation and private actions.
- [x] Safe exact review destinations, existing financial confirmation UI, in-app discovery and coordinated settings.
- [ ] Unit/lint/APK/Android-test compilation through unchanged Android CI; fresh integration review; verify latest main overlap before final report. No merge/release. Instrumentation and physical-device acceptance remain paused unless explicitly requested.

Local baseline test attempt could not start: no Java or Android SDK. Use isolated pure Kotlin/JUnit runtime where available, and CI for full Android verification. No workflow, parser, schema, signing or statement changes.

## Review and scope decisions

Independent review of 54baf19..7f84457 identified payment-cooldown cancellation, legacy seven-day pause migration and lost pending routes during recreation. Addressed in 2f7c09f; additional JVM and Android regression coverage includes grouped digest ordering, delivery-versus-actionability, preference restart/migration, stale evidence, explicit repayment completion, clean-slate discovery and recreation while locked. Android tests are compiled, not executed while the instrumentation pause remains.

The review of the additional import stage reproduced three further races: revision extras sharing a PendingIntent token, delayed dismissal invalidating newer evidence and overlapping imports persisting success before all finish. Revision-specific Android intent identity, evidence-scoped dismissal and a persisted active-import/batch-failure state address them. JVM tests exercise both completion orders and interruption recovery; Android fixtures cover real PendingIntent identity and delayed receiver callbacks. No financial writes are added by these fixes.

Historical SMS import now has a small notification-only wrapper: processing suppresses dispatch and a persisted unsuccessful/interrupted outcome remains fail-closed until a subsequent successful consented import. Parsing, checkpointing and financial writes are unchanged. New-event alerts intentionally use live approved financial notifications; historical SMS, catch-up history and statement imports never become retroactive debt suggestions. Lending history must have been recorded before the new event. Unknown-account observations without existing material ownership-review evidence stay silent. Over-balance receipts offer original-payment review rather than preselecting repayment; the existing dialog remains available for a explicitly chosen partial amount.

The first full Android CI passed at 7f84457 (run 38426737670): unit tests, lint, APK assembly and Android-test compilation. Final-head verification and artifact inspection must still complete after subsequent fixes. No merge or publication from this branch.

When a new matched payment is opened directly in Activity, an optional inline hint can open the same review without phone notification permission. It is shown once per evidence version and respects event dismissal. Closing/dismissing it cannot cancel an unrelated posted account digest. This is page-entry guidance only and never dispatches an OS notification.
