# Unified premium UI: route and state verification ledger

This is the acceptance ledger for the owner-approved *single* UI release.
The 34 destinations are taken from `ui/PesaTrackApp.kt` on 11 November 2026.
Three are redirect-only compatibility routes and are not visual pages.

**Progress key:** foundation = shared Material theme, shell and palette adapters
are migrated in the integration branch; checked = individually inspected in
the exact final APK. **No page is marked checked yet**, because local Android
compilation is blocked by the unavailable Gradle wrapper distribution, and a
physical Redmi review has not happened.

## Destinations — 31 user-facing, three compatibility redirects

| Group | Routes / saved subpages | Current migration / inspection state |
| --- | --- | --- |
| Entry | `onboarding`, `smart-onboarding-setup` | Foundation; onboarding gradient migrated; setup needs review |
| Home | `home` | Foundation; approved coffee art and finance evidence retained |
| Activity | `activity`, `activity-attention`, `transaction-detail/{transactionId}`, `smart-review/{key}?action={action}&revision={revision}` | Foundation; chart and account review palette updated; overlays need review |
| Plan | `plan`, `salary`, `paycheck-analysis`, `budgets`, `bills`, `saving-goals`, `pay-cycle` | Foundation; hero, progress, breakdown, pay-cycle gradient updated; forms need review |
| Accounts | `accounts`, `account-detail/{accountId}`, `financial-institutions`, `debts` | Foundation; card palette adapters updated; investment PR integration pending |
| More and Profile | `more`, `profile?section={section}`, `settings`, `smart-rules`, `data-health`, `identity-review` | Foundation; More/Profile palette adapters updated; nested preferences need review |
| Statements | `statement-activity`, `statement-import`, `reports`, `historical-import` | Foundation; Statements overview/deep dive palette updated; import/export states need review |
| Setup and privacy | `mpesa-connection`, `backup-reminder` | Foundation; needs review, including permission/backup states |
| Redirect-only | `community`, `goals`, `goal-detail` | Existing redirects preserved; no visual surface |

Additional non-route surfaces to verify: splash and app lock; Profile
notifications, security, appearance, privacy, support, about and terms;
account selection/add/edit; goals and bills bottom sheets;
transaction note/receipt/filter/category; statement evidence/detail/export;
all dialogs, keyboard states, empty/error/partial/review-required states.

## Release gates still open

- [ ] Resolve dark/system-mode requirement: `MainActivity` currently forces
  `useDark = false` in the inherited baseline, despite storing Appearance mode.
  Do not turn it on until every reachable palette has dark-mode support.
- [ ] Review active screen-specific foreground/background literals; retained
  chart and provider identity colours must be legible and deliberate.
- [ ] Run exact-head Kotlin/JVM, lint and APK build through GitHub CI;
  no Android instrumentation or emulator run.
- [ ] Review 31 destinations and nested states for visual consistency,
  navigation, font-scale and 48 dp targets using safe synthetic data.
- [ ] Reconcile latest `main` changes from other financial/feature PRs without
  overwriting their files or changing money presentation meaning.
- [ ] Complete independent code review, merge one PR and verify rolling signed
  Full/Update assets reference the merged commit.
- [ ] Owner checks installed update on physical Redmi; preserve in-place
  update and financial data.

No percentage of UI completion should be derived from the number of files
touched; rendered consistency has not yet been measured.
