# PesaTrack Settings Visual Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Improve Settings and money message access visual hierarchy without changing actions or data.

**Architecture:** Reuse shared Compose icon bubbles/tone helpers and sizing tokens in two existing screens. No new state, services or dependencies.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android CI.

**Spec:** `docs/superpowers/specs/2026-11-03-settings-visual-polish.md`

## Global Constraints

- Preserve Settings and Money Access callback signatures and behaviour.
- Preserve source permission controls, offline privacy and user data.
- 48dp minimum interactive targets; 16dp card inset; semantic violet/blue/green icon tones.
- Do not modify Statements reconciliation, account totals or notification schedules.
- No instrumentation or emulator run in this stage.

## Review Focus

- Narrow 320dp view: titles and wrapped descriptions remain legible.
- Large Android fonts: icon bubbles do not crowd row labels.
- No approved sources: source chooser still opens and explains selection.
- Permission disabled: SMS and notifications rows still invoke existing flows.
- Dark system theme: semantic bubble foregrounds retain readable contrast.

---

### Task 1: Settings list consistency

**Files:** `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/SettingsScreen.kt`

- [ ] Keep the two existing Settings destinations and their callbacks.
- [ ] Replace same-colour plain icons with shared violet/blue bubbles.
- [ ] Remove the repeated More-navigation explanation and dividers between cards.
- [ ] Inspect layout at narrow/large fonts in a device pass; run source diff checks.

### Task 2: Money message access clarity

**Files:** `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/SettingsDetailPages.kt`

- [ ] Preserve all previous `EverydayRow` actions, consent state and Android intents.
- [ ] Use concise row copy and semantic icon tones from shared components.
- [ ] Ensure stop-access, Android-settings and details buttons have 48dp targets.
- [ ] Verify exact-head Android CI JVM/lint/debug APK; document device QA remaining.

### Task 3: Safe handoff

**Files:** `CURRENT_STATE.md`, this plan and PR description.

- [ ] Confirm PR #610 has merged before branching against fresh main.
- [ ] Record visual-only scope and latest-main SHA, avoid overlapping other PRs.
- [ ] Open a separate draft PR tracking #399 and `CHAT-VISUAL-POLISH`.
- [ ] Merge only after required exact-head checks pass; phone QA remains separate.
