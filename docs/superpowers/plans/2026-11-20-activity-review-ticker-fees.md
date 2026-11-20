# Activity review prompt, ticker and fees implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax.

**Goal:** Preserve the approved Activity interface while showing a gentle once-daily review prompt on unlocked app entry, exposing missing fees honestly and making the compact ticker readable.

**Architecture:** Keep ledger and classification untouched. Share the existing Activity supplemental review reader with an app-home prompt, persist only its display throttle in local preferences, and retain the existing review route. Move ticker timing and fee eligibility into pure testable helpers.

**Tech Stack:** Android, Kotlin, Jetpack Compose, SharedPreferences, JUnit.

**Spec:** GitHub issue #698 (owner-approved 20 November follow-up).

## Global constraints

- No changes to persisted transactions, money meanings, permissions, signing, navigation destinations or the approved dark/light Activity theme.
- Unknown fees show an unobtrusive dash, never a fabricated amount or an asserted zero charge.
- A 48dp touch target remains on actionable controls even if their visible surfaces become more compact.
- Do not run emulator/instrumentation; GitHub JVM/lint/APK workflows gate merge and real-phone acceptance remains pending.

## Review focus

- No outstanding review items: no prompt (unit test).
- Reopening the app the same local day: no repeat for unchanged or smaller counts (unit test).
- More review items on the same day: eligible for a new prompt (unit test).
- Midnight in Africa/Nairobi: new local day re-enables reminder (unit test).
- Fee is missing/zero versus positive on outgoing/incoming rows: dash only on unknown outgoings, real amounts only where recorded (unit test).

## Task 1 — Review prompt

- [ ] Write the failing eligibility and Kenyan day-boundary unit tests.
- [ ] Confirm they cannot pass without the new policy (local Gradle may be blocked).
- [ ] Extract the existing supplemental review reader, derive the same count on Home and Activity, and render the coffee/cream modal only while content is unlocked.
- [ ] Persist a shown-day/count throttle and route **Review now** to existing Activity attention; allow **Not now** or outside/back dismissal.
- [ ] Remove the in-list review banner without altering the review queue or its scroll anchor.

## Task 2 — Fees and filters

- [ ] Write the failing tests for unknown outgoing fees and incoming no-fee rows.
- [ ] Keep the recorded fee chip beneath the amount; show muted `Fee —` only for unknown outgoing fees.
- [ ] Reduce visible segmented-control padding, keeping the 48dp interactive footprint.

## Task 3 — Ticker, verification and merge

- [ ] Write the failing timing constants test (40s per period, 20s metric travel).
- [ ] Keep the year/month/day label stationary for 40 seconds while metric text scrolls to center Net and fades/restarts.
- [ ] Run focused checks and repository security/source checks, then GitHub JVM tests, lint, APK assembly and PR review.
- [ ] Merge only on successful exact-head checks and report Redmi testing as unverified.
