# Plan: visual audit and three safe interaction corrections

Spec: `docs/superpowers/specs/2026-10-31-visual-audit-safe-fixes.md`

1. Build an evidence register for every major screen family, with file and
   line references, severity, status, ownership and visual-QA limitations.
2. When a focused emulator window is explicitly authorized, write Compose
   UI tests for accessible 48dp tap targets and scrollable bank-year chips;
   watch them fail **RED** against current UI. Avoid source-text-only tests.
3. Fix only `ActivityReferenceSummary.kt`, `StatementUnifiedOverviewPage.kt`,
   and `StatementBankOverviewPage.kt`. Make the *entire statement account
   coverage row* clickable, with 12 visual-only bars. Re-run UI tests GREEN;
   preserve callbacks, copy, financial logic and existing navigation.
4. Compile UI tests in ordinary CI but run them only in a deliberately
   scheduled focused instrumentation session. Until RED→GREEN execution is
   available, leave production UI code unchanged and report the blocker.
5. Review `git diff`, check competing PRs, run source tests and request a
   whole-branch review. Push an isolated branch and open a focused PR if safe;
   merge only after fresh main, check status and conflicts are verified.

Review focus: app accessibility size, test tag semantics, 12-bar density,
year-row overflow, visibility of tiny text, and accidental style divergence.
