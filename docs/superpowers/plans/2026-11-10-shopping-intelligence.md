# Shopping Intelligence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task by task.

**Spec:** `docs/superpowers/specs/2026-11-10-shopping-intelligence-design.md`

**Goal:** Add on-device photo OCR, review-before-save item import, local item suggestions and monthly planning history without touching the ledger.

**Architecture:** Pure `ShoppingIntelligencePolicy` handles textual candidates, duplicate/cap decisions, recommendations and monthly summaries. A dedicated Compose file owns photo picker, ML Kit and review UI. Existing `ShoppingListsScreen` gets only two integration slots. Keep `ShoppingListStore` format untouched.

**Global constraints:** No new runtime permissions, remote uploads, telemetry, balance effects, schema or backups. Follow approved coffee-themed cards. User tests on phone; no instrumentation/emulator tests.

## Review focus
1. No text/blank/bad OCR: clear empty or error state, no save.
2. Receipt totals and price ambiguity: ignore totals, never count ambiguous values as verified spending.
3. Max 100 items and duplicate names: skip with visible count and no overwrite.
4. Suggestions from different months with changing prices: frequency over distinct months; unknown price when added.
5. History estimates incomplete: show missing-price count, never label as actual expense.

### Task 1: Pure parsing and recommendation policy

**Files:** Create `domain/planning/ShoppingIntelligencePolicy.kt` and `app/src/test/.../domain/planning/ShoppingIntelligencePolicyTest.kt`.

**Interfaces:** `parseLines(text: String): List<ShoppingScanCandidate>`; `importCandidates(existing: ShoppingList, candidates: List<ShoppingScanCandidate>, ids: () -> String): ShoppingImportResult`; `suggest(lists: List<ShoppingList>, selected: ShoppingList): List<ShoppingSuggestion>`; `history(lists: List<ShoppingList>): List<ShoppingMonthSummary>`.

- [ ] Write synthetic JVM tests for ordinary lines, quantities/prices, totals/noise, duplicate/cap, distinct-month suggestions and history exact minor sums.
- [ ] Run focused tests and record baseline limitation if Gradle distribution unavailable.
- [ ] Implement pure policy and rerun focused tests.

### Task 2: On-device OCR review UI

**Files:** Create `ui/screens/ShoppingIntelligencePanels.kt`, modify `app/build.gradle.kts`, minimally modify `ui/screens/ShoppingListsScreen.kt`.

**Interfaces:** `ShoppingPhotoAndSuggestions(selected, allLists, busy, onSave)`; `ShoppingHistoryPanel(lists)`.

- [ ] Add separate import candidate/policy tests first (Task 1).
- [ ] Add bundled `com.google.mlkit:text-recognition:16.0.1` and Android photo picker, explicit review dialog, suggestions, monthly history cards, and error/empty states.
- [ ] Ensure privacy: no image copy, no permission request, no transaction writes.
- [ ] Run available static checks and GitHub Android CI after pushing.

### Task 3: Documentation and PR verification

**Files:** Modify `CURRENT_STATE.md`, `README.md`, `docs/PRIVACY_POLICY.md`.

- [ ] Document offline OCR, no photo retention, estimates not expenses and planned feature limits.
- [ ] Review `git diff --check`, changed files, ownership overlap, CI checks and APK size impact.
- [ ] Publish an isolated draft PR and report exact-head CI results. Do not merge without separate owner approval.
