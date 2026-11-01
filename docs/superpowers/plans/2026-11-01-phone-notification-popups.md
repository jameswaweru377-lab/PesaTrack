# PesaTrack phone notification pop-ups — implementation plan

**Spec:** User-approved phone-notification concepts and 1 Nov 2026 request. Deliver real Android notifications outside the app, tap-to-open destinations, and practical previews. Local-only, opt-in, privacy-safe, no premium marketing push.

**Global constraints:** Preserve accounting and import truth; do not invent bill/debt due dates; use Android notifications, not custom overlays; user and Android retain final control over heads-up banners. No signing or release workflow edits. Keep existing review digest/cooldowns and explicit bill/salary timing. New testing reminders are user-triggered only, never a permanent 10-minute spam loop.

## Task 1: High-visibility phone delivery

**Interfaces:** Existing bills, salary, and planning reminder workers create Android notifications; introduce a shared channel policy and use new high-importance channel IDs to avoid Android's immutable existing channel settings.

1. Add focused unit tests for channel IDs and available phone-alert templates; expect red before implementation.
2. Add channel policy and upgrade existing bills, salary, budget, savings notification builders to distinct `IMPORTANCE_HIGH` channels, high priority, private public versions, and existing destinations.
3. Preserve existing `review-attention` coordinator and its user-controlled quiet mode.
4. Run focused tests and inspect notifications for unintended repetition.

## Task 2: Backup reminder and verified-backup date

**Interfaces:** A pure backup policy consumes opt-in timestamp, latest verified export date, last notification date; worker consumes policy and opens backup flow. Successful export verification records date without affecting backup success itself.

1. Add unit tests for 30-day opt-in cadence, quiet hours, snooze after a verified backup, clock rollback, and off mode; expect red.
2. Implement opt-in monthly backup reminder with daily periodic work and stable alert ID; do not schedule if turned off. Record verified export only after a successful reread.
3. Wire a destination that opens the actual backup sheet behind normal app lock and respects setup gating.
4. Run policy tests.

## Task 3: Phone-preview controls, permission explanation, and end-to-end wiring

**Interfaces:** Notification settings can select one of eight test types and send it now or after 10/20/30 minutes. Preview worker sends a visibly labelled synthetic example and opens the matching page. Existing review/account/They Owe Me alerts continue to use their smart coordinator.

1. Test selection/route mapping and scheduling behavior. Add real Android preview sender and one-time worker (user-triggered only).
2. Expose salary, account/data review, backup, test notification, and notification settings controls in Reminders page. Keep premium promotion out of phone notifications. Explain phone/Redmi banner settings.
3. Schedule backup on app start/setup. Confirm no new permissions or finance mutations.
4. Run all feasible checks; document any Android SDK/device limitations; review whole diff and open a GitHub PR (do not merge without the user).

**Review focus:** Android 13 permission, pre-26 priority, immutable channels, work cancellation, duplicate notices, wrong routes, screen-lock privacy, and false financial inference. **Expected real-device check:** A preview should appear in the Android notification shade; heads-up appearance depends on phone channel/banners/DND and requires Redmi verification.
