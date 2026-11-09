# Plan Shopping Lists Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans (inline) to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Provide private, reusable monthly shopping lists with honest estimated totals inside Plan.

**Architecture:** A pure `ShoppingListPolicy` owns validation, cloning and integer arithmetic. A small `ShoppingListStore` persists bounded JSON in an app-private SharedPreferences file, already covered by the encrypted-backup preference export. A dedicated Compose screen handles edits, while Plan only links to it.

**Tech Stack:** Kotlin, Jetpack Compose, Gson (already bundled), SharedPreferences, JUnit/Robolectric (already configured).

**Spec:** `docs/superpowers/specs/2026-11-09-plan-shopping-lists-design.md`

## Global Constraints

- No ledger, budget, schema, permission, network, analytics or signing changes.
- KSh prices use `Long` minor units, not floating point.
- Shared scaffold and five tabs remain unchanged; reviewed designs and 48dp touch targets stay in place.
- No emulator, phone upgrade or instrumentation tests without the user's request.

## Review Focus

- Blank/overlong item names and invalid quantities/prices must fail before storing.
- Unknown prices must not be counted as zero-cost purchases or fully priced totals.
- Reusing an earlier list must not modify its items or carry bought state forward.
- Corrupt stored JSON must surface a recoverable error rather than overwrite records.
- Failed preference writes must never be reported as saved; backup export still includes the new private preference file.

---

### Task 1: Pure shopping-list rules

**Files:**
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/domain/planning/ShoppingListPolicy.kt`
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/domain/planning/ShoppingListPolicyTest.kt`

**Interfaces:** `ShoppingList`, `ShoppingItem`, `ShoppingListPolicy.validate`, `.total`, `.reuse`, `.parsePriceMinor`.

- [ ] Write JUnit tests for bounded names, quantities, price parsing, missing-price status, sum and unchanged reuse.
- [ ] Run targeted test; expect RED because implementation is absent.
- [ ] Implement pure policy and immutable models using integer minor units.
- [ ] Run targeted test; expect PASS; commit task.

### Task 2: Local persistence and restoration

**Files:**
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/data/local/ShoppingListStore.kt`
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/data/local/ShoppingListStoreTest.kt`

**Interfaces:** `ShoppingListStore(context).list()`, `.save(list)`, `.delete(id)`; JSON payload in private `shopping_lists_v1` preferences.

- [ ] Write Robolectric tests for restart persistence, edit/delete, failed/invalid JSON, and backups' allowed-pref filter.
- [ ] Run targeted test; expect RED because store is absent.
- [ ] Implement bounded, all-or-nothing preference writes and strict decode.
- [ ] Run targeted test; expect PASS; commit task.

### Task 3: Plan entry and shopping editor

**Files:**
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/ShoppingListsScreen.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/PlanScreen.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/PesaTrackApp.kt`
- Modify: `README.md`, `CURRENT_STATE.md`, `docs/PRODUCT_CHARTER.md`

**Interfaces:** `ShoppingListsScreen(scaffoldPadding, onBack)`, `PlanScreen(onShoppingClick)` and route `shopping-lists`.

- [ ] Add focused UI/screen rule tests where pure behavior needs coverage; run them RED.
- [ ] Add entry card, screen, item form and explicit reuse action. Keep the whole shopping journey local and never post a transaction.
- [ ] Run tests, static diff and (where available) `./gradlew testDebugUnitTest lintDebug assembleDebug`; update product docs with exact verification limits.
- [ ] Commit task, review whole branch and hand off for CI/device visual acceptance.
