# A2: move bill cadence into the pure planning domain

Source: verified audit finding A2 (P3), issue #545, owner CHAT-AUDIT-A2.
Base: latest main at the start of this branch,
`91a2a786eb75ec286161d8218a4ae7294028f096`.

## Scope and safety boundary

- Move the `BillCadence` enum from outer `planning/BillSchedules.kt` to
  `domain/planning/BillCadence.kt`.
- Keep a Kotlin `typealias` in the original package so the scheduling API and
  existing UI imports still compile. Remove the domain parser's outer-layer
  import. Do not edit parsing or financial logic.
- Preserve exactly the `MONTHLY` and `WEEKLY` enum names used by the existing
  SharedPreferences `cadence:$ruleId` string writer and reader.
- Do not modify ledger, backup, database, UI/theme, manifest or network code.

## Proof and merge gate

1. Add `BillCadenceDomainContractTest` first. Before implementation the domain
   `BillCadence` symbol is absent, so the new test cannot compile (static
   check, not a claimed executed JUnit RED).
2. Move the enum and add the compatibility alias; keep `VoicePlanningParserTest`
   green. Compare enum names and alias identity in the new JUnit test.
3. Run the available local tool tests, security scan and diff checks. Kotlin
   Gradle test execution is blocked locally by the Android Gradle plugin
   `9.3.1` being absent from the offline cache; run exact-PR-head Android CI.
4. Check the current main head and PR overlap again before merge. Merge only
   after the Android CI and PR Secrets jobs succeed. Verify the signed main
   publisher separately.

Rollback: revert this one PR; stored preferences continue to use unchanged
enum names. No new migration or data rewrite is performed.
