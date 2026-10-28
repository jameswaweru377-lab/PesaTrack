# Statements Phase 1: Real M-Pesa Insights Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the existing Statements presentation with the approved premium PesaTrack experience, fully backed by real imported M-Pesa statement evidence and existing Activity category/receipt integrations, while leaving a clean reusable foundation for later banks.

**Architecture:** Keep the existing `statement-activity` navigation destination and the separate verified statement archive. Move its content out of the full-screen `Dialog` into the normal app scaffold; use one source-neutral, single-selected-account projection from `StatementReportReader`, `StatementFinancialPolicy`, `StatementHistoryReport`, and existing category evidence. Introduce pure Kotlin scope, coverage and presentation models, then replace the old UI in place and route each overview action to actual functioning detail pages. Do not build a parallel dashboard or mutate the canonical Activity ledger during statement analysis.

**Tech Stack:** Kotlin 2.4.20, Jetpack Compose/Material 3, Android SDK min 24 / target 36, Gradle 9.5 / AGP 9.3.1, Java 17, app-private SQLite, JUnit4 JVM tests, existing Compose instrumentation suite.

**Spec:** [Approved illustrated Statements design PDF](https://chatgpt.com/library?file_id=libfile_13007cab94b88191a9426aea75d66b84&account_id=personal) and [written design specification](https://chatgpt.com/library?file_id=libfile_2bc5c4702ce8819180449e5a1d682e8b&account_id=personal). Read the real source in the exact execution checkout; the [28 October code audit](https://chatgpt.com/library?file_id=libfile_6440f99e4fe881919d3e9e59d1a71e18&account_id=personal) is supporting context, not a substitute for code. Baseline inspected: `main` at `c20364df226fbbfc2bb0171c9ac1845220d348e5` (remote `main` verified unchanged on 28 October 2026).

## Global Constraints

- **Scope:** Phase 1 delivers fully working **M-Pesa Statements**, including real overview, year/month trends, categories, archive/coverage, imported period and receipt evidence, and existing import/category/Activity connections. **Cross-account aggregate reports and KCB/Equity PDF parsing are not implemented in Phase 1.**
- For one or more supported M-Pesa wallets, show only the **selected wallet's** figures. Do not display an “All accounts” option or a combined sum until a separately validated aggregation layer exists. The design's All-accounts and KCB/Equity boards are reusable visual references for later phases, not permission to show fake data or pending bank buttons now.
- **No placeholders:** Every visible CTA opens a working destination. Hide unsupported bank imports and unavailable Activity deep links; do not ship “Coming soon”, demo amounts, blank sheets, or inactive decorative action cards.
- **No second UI:** Modify/reuse `StatementIntelligenceDashboard`, `StatementOverviewPage`, existing Statements detail/import surfaces and existing navigation route. Remove old chrome and dead widgets **after** their replacement has working tests. Do not stack a new dashboard over old cards or leave the old five-item `StatementBottomNavigation` visible.
- Preserve all existing useful Statements functions: Summary, Categories, Trends, Transactions, Debts, Browse/archive, category edits and sharing, import, overlap explanation, account and Day/Week/Month/Year/All-history filtering, deletion/privacy review and Android Back behavior. New detail/coverage pages may be added if connected to real data.
- Global navigation is the current **four tabs** `Home / Activity / Plan / More`, provided once by `AppScaffold`, with More selected inside Statements. Do not change the approved Home or Activity visuals. Keep the real `transaction-detail/{transactionId}` route.
- Visual tokens: canvas `#F7F7FF`, ink `#0B103A`, muted `#626B98`, primary `#5D00EB` / `#6B18FF`, green `#087E54`, red `#DC1551`, blue `#003BCD`, divider `#E6E8F2`; rounded white cards; violet-blue hero. Reuse `HomeReferencePalette`, money formatters, and app icons rather than copying hard-coded colors everywhere. Check light/dark contrast and large fonts.
- Financial authority: **classified spending** comes from `StatementFinancialPolicy.totals(...).expenseMinor`; **classified income** from `.incomeMinor`; raw **Money in/out** from `sourceCashFlow`. Transfers, loans, savings, fees, refunds, exclusions, unknown movements and investment must not be silently counted as spending. `Income − spending` is neither bank balance nor available cash. No invented account totals.
- Reuse **one selected source per account-month** from `StatementHistoryReport.selectForAccount`; preserve alternatives/overlaps for the archive. An imported partial month is **not proof** that the whole month is complete; absent months are **not zero-spending months**.
- Maintain `StatementActivityStore` as a separate, explicitly approved statement archive. Do not add Activity transactions merely because a statement was imported; leave the independent reviewed Activity-import flow intact. Shared category decisions require the existing exact-match/user-consent policy; never infer the same John from a repeated name alone.
- Local-only financial data; no new permissions, API uploads, network-based categorization, telemetry, asset downloads, provider-by-provider Kotlin UI forks, parser rewrite or database migration as part of this visual phase.
- No PR merges, workflow changes, releases, APK publication, or emulator/instrumentation dispatch without new explicit authorization. Keep unrelated PRs (#460, #459, #458, #456 and older) unmerged; borrow only independently verified ideas.
- All image amounts are **fictional examples**. UI literals should contain labels only, never sample values. Accessibility: meaningful semantics/content descriptions, color-independent statuses, >=48dp practical tap targets where applicable, and no clipped currency/date labels on narrow Redmi-sized screens.

## Review Focus

1. **Overlapping M-Pesa PDFs:** two source files cover the same account-month; the overview must count that month once and the archive must still show alternative source history. Covered by Tasks 1, 2 and 6.
2. **Absent vs observed-zero vs partial periods:** 2025 contains only January and March imports; February must be visibly missing, not a zero bar, and the UI must not claim full-month completeness. Covered by Tasks 2, 3 and 6.
3. **Financial ambiguity:** a wallet-to-bank own transfer, Fuliza loan, refund, excluded transaction and unknown debit are present alongside a purchase; only classified purchase spending contributes to the hero. Covered by Tasks 2, 3 and 5.
4. **Identity/receipt collision:** two Activity candidates share a reference or a candidate differs in account/amount/date; do not show a direct Activity record action or propagate a category. Covered by Task 7.
5. **Empty/error and long-history device usage:** no imports, partial import, corrupt file, 24-month history, large-font/narrow viewport, dark theme and repeated navigation must show truthful usable states with no second chrome or repeated expensive PDF parsing. Covered by Tasks 4, 8 and 9.

## Execution preflight (before Task 1)

- [ ] Confirm the working checkout is a clean feature branch created from the current remote `main`; verify base SHA and overlapping open PRs. The planning branch `codex/statements-phase1-plan-20261028` is **not** an authorization to merge or push.
- [ ] Read `AGENTS.md`, `README.md`, `CURRENT_STATE.md`, `CONTRIBUTING.md`, `docs/PRODUCT_CHARTER.md`, this plan and the approved PDF. The 20 October current-state text is older than the merged 27 October Statements UI; source and verified tests decide conflicts.
- [ ] Identify safe SDK/Gradle availability. On the planning machine, an isolated `--offline` unit-test attempt never configured AGP `com.android.application:9.3.1`; do not treat that as failing app tests. Use the existing CI when dependencies cannot be fetched locally.
- [ ] Keep each task on a reviewable commit. Run focused tests first, `git diff --check`, then broader checks; pause on a financial regression instead of redesigning around it.

---

## Proposed file/responsibility map

| Unit | Ownership |
| --- | --- |
| `domain/everyday/StatementReportScopePolicy.kt` (new) | One explicit All/Year/Month/Week/Day scope; filters rows without ambiguously mixing periods |
| `domain/everyday/StatementInsightProjection.kt` (new) | Pure derived spending/income/source-movement totals, twelve monthly bars and missing-month evidence |
| `ui/screens/StatementInsightModels.kt` (new) | Source-neutral presentation labels/amounts/category items and optional state for the Compose pages |
| `ui/screens/StatementOverviewPage.kt` (existing) | The approved single-wallet premium overview; one primary spending hero, chart/category/archive links |
| `ui/screens/StatementReportVisuals.kt` (existing) | Reusable statement graph and money presentation building blocks, including accessible bar labels |
| `ui/screens/StatementIntelligenceDashboard.kt` (existing) | The single state/navigation host; preserves six existing destinations and category review, but not its legacy Dialog or bottom chrome |
| `ui/screens/StatementCategoryInsightPage.kt` (new) | One category's history, subcategories/payees and supporting entries |
| `ui/screens/StatementPeriodPage.kt` (new) | A selected imported month and its filtered source entries, separate from general Activity |
| `ui/screens/StatementCoveragePage.kt` (new) | Imported and missing months for the selected wallet/year; no fake zeros |
| `ui/screens/StatementEvidenceDetailPage.kt` (new) | One source row, source provenance, category review CTA, conditional exact Activity match |
| `data/local/StatementActivityLinkReader.kt` (new) | Read-only, on-demand exact corroborated canonical transaction ID for an opened statement row |
| `ui/PesaTrackApp.kt`, `ui/components/AppScaffold.kt` (existing) | Existing route/global four-tab chrome; route nested Statements entry points through the same destination |
| `ui/screens/CashEntryScreens.kt`, `ui/screens/ActivityScreen.kt` (existing) | Preserve Activity tool-sheet Statements action while navigating to the single route, without launching another dashboard Dialog |
| `ui/screens/StatementEmptyLanding.kt`, `StatementImportWorkspace.kt`, `StatementImportScreens.kt` (existing) | Actual M-Pesa-only empty/import/review flow; route the normal import-sheet's Statements action through the parent instead of opening a nested full-screen dashboard |

Path prefix for all Kotlin entries above: `app/src/main/java/com/kedevjimmy/pesatrack/`. Test paths are stated per task. The map is deliberately **not** a request to split/refactor Home, Activity, or every large Statements file.

### Task 1: Deterministic scope and date navigation

**Files:**
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/domain/everyday/StatementReportScopePolicy.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementIntelligenceDashboard.kt` (state and archive/trend month callbacks only)
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/domain/everyday/StatementReportScopePolicyTest.kt` (new)

**Interfaces:**
- Produce sealed `StatementReportScope`: `AllHistory`, `Year(value: Int)`, `Month(key: String)`, `Week(anchorMs: Long)`, `Day(anchorMs: Long)`.
- Produce `StatementReportScopePolicy.selectRows(rows: List<StatementClassified>, scope: StatementReportScope): List<StatementClassified>`; use `Africa/Nairobi` through the existing `StatementMonthlyUsage.monthKey` and `StatementFinancialPolicy.within`.
- Dashboard owns **one** `scope` value instead of diverging `allHistory` + month/year/period state. Use the most recent imported year as the initial yearly overview when history exists; retain an explicit All-history selection.

- [ ] **Step 1: Add failing tests** `yearOnlyContainsItsRows`, `monthSelectionFromArchiveOverridesAllHistory`, `yearTrendToMonthChangesScope`, `weekMayCrossMonthBoundary`, `allHistoryRetainsEveryRow`, `otherAccountRowsNeverEnterSelectedAccountReader`. Use invented valid `StatementActivityLine` fixtures and the exact selected year/month outputs.
- [ ] **Step 2: Run the focused class**: `./gradlew :app:testDebugUnitTest --tests 'com.kedevjimmy.pesatrack.domain.everyday.StatementReportScopePolicyTest'`. Expected: failure for not-yet-defined policy; if plugin resolution blocks configuration, record the blocker rather than claiming a failed assertion.
- [ ] **Step 3: Implement the pure selector**, then replace the dashboard's conflicting scope booleans; an archive month tap and year-chart month tap must select `Month(key)` before returning to Summary/Period.
- [ ] **Step 4: Run the same focused test** and existing `StatementFinancialPolicyTest`, `StatementHistoryReportTest`. Expected: PASS with no cross-year sum or selected-month drift.
- [ ] **Step 5: Commit** `fix(statements): unify period scope and month navigation`.

### Task 2: Correct financial summary, historical bars and coverage

**Files:**
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/domain/everyday/StatementInsightProjection.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/domain/everyday/StatementTrendBuckets.kt` (only if an existing bin cannot express the intended period)
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/domain/everyday/StatementInsightProjectionTest.kt` (new)
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/domain/everyday/StatementTrendBucketsTest.kt` (extend)

**Interfaces:**
- Produce `StatementMonthSpend(year: Int, month: Int, spentMinor: Long?, imported: Boolean)` where `spentMinor == null` means no imported source month, while `0L` means an **observed** month with no classified spending.
- Produce `StatementInsightMetrics(spentMinor: Long, incomeMinor: Long, moneyInMinor: Long, moneyOutMinor: Long, unknownInMinor: Long, unknownOutMinor: Long)`.
- Produce `StatementInsightProjection.metrics(rows: List<StatementClassified>): StatementInsightMetrics` and `yearBars(rows: List<StatementClassified>, months: List<StatementHistoryReport.SelectedMonth>, year: Int): List<StatementMonthSpend>`.

- [ ] **Step 1: Add failing tests**: January expense 1_000 minor + own transfer 90_000 + debt 30_000 + unknown debit 5_000 produces hero spend **1_000**, not 126_000; income uses classified inflows, raw Money out includes all debits, and refunds/fees stay distinct. Year 2025 Jan/Mar imports yield exactly 12 bars: Jan/Mar observed, Feb missing `null`; a March imported month with only transfers has observed `0L`. Same month from two PDFs is not doubled once `selectForAccount` has selected a source.
- [ ] **Step 2: Run focused projection/trend tests**; expected red until new policy exists.
- [ ] **Step 3: Implement via `StatementFinancialPolicy.totals`, `sourceCashFlow`, `StatementHistoryReport.selectForAccount` and Nairobi months**, not bank-name conditionals. For All-history charts, aggregate by **year**; for year charts, by actual calendar month; for month charts, only that month's rows in the five existing week bins.
- [ ] **Step 4: Rerun `StatementInsightProjectionTest`, `StatementTrendBucketsTest`, `StatementHistoryReportTest`**. Confirm missing and zero are distinguishable to the UI.
- [ ] **Step 5: Commit** `feat(statements): build evidence-based overview and coverage projection`.

### Task 3: Reusable source-neutral overview presentation

**Files:**
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementInsightModels.kt`
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/ui/screens/StatementInsightModelsTest.kt` (new)

**Interfaces:**
- Produce `StatementInsightViewModel` (plain immutable **presentation data class**, not an Android architecture ViewModel) carrying selected wallet name/identifier, `StatementReportScope`, the Task 2 metrics, `List<StatementMonthSpend>`, top category rows with exact `Long` amounts, `List<StatementHistoryReport.SelectedMonth>`, `hasUnclassifiedIncome: Boolean`, and `hasMissingMonths: Boolean`.
- Produce `StatementInsightModels.from(accountLabel: String, scope: StatementReportScope, visible: List<StatementClassified>, selectedMonths: List<StatementHistoryReport.SelectedMonth>, categoryLabels: Map<String, String>): StatementInsightViewModel`.

- [ ] **Step 1: Test** that the model is a pure transformation of statement evidence; expense category rows sum to the classified expense total, `Income − spending` is subtraction of classified totals and never the raw net source movement, a completely unclassified inflow is disclosed rather than labeled salary/income, and account labels contain no hard-coded bank/provider names.
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests 'com.kedevjimmy.pesatrack.ui.screens.StatementInsightModelsTest'`; expected red for the new class.
- [ ] **Step 3: Implement** with `StatementExpenseCatalog.breakdown` and Task 2 projection; keep KES formatting outside arithmetic (minor units stay `Long`). Missing, empty and unsupported states must be values, not invented amounts.
- [ ] **Step 4: Rerun focused tests**, then existing category/evidence tests. Expected PASS.
- [ ] **Step 5: Commit** `feat(statements): define reusable account insight view state`.

### Task 4: Replace the legacy full-screen Dialog and duplicate nav in place

**Files:**
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/PesaTrackApp.kt` (existing `statement-activity` route)
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/components/AppScaffold.kt` (tools sheet Statements navigation)
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/ActivityScreen.kt` (Activity tools callback)
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/CashEntryScreens.kt` (both `ActivityMoneyActions` and `ActivityToolsDisclosure`)
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementImportScreens.kt` (normal import-sheet follow-up navigation)
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementIntelligenceDashboard.kt`
- Modify or remove after references are gone: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementActivityDialog.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementEmptyLanding.kt` (delete `StatementBottomNavigation` only when unused)
- Test: `app/src/androidTest/java/com/kedevjimmy/pesatrack/ui/screens/StatementWorkspaceNavigationUiTest.kt` (new)

**Interfaces:**
- Keep route literal `statement-activity`. Host `StatementIntelligenceDashboard` inside existing `AppScaffold(currentTab = MainTab.MORE, useHomeHeader = true)` and pass `PaddingValues` through a `modifier: Modifier` parameter.
- Retain `onDismiss: () -> Unit`; remove the old in-dashboard `onTabSelected` bottom navigation and add Task 7's `onActivityTransactionClick: (Long) -> Unit` only when needed. Existing Activity-bank CSV importing remains in the tools flow and is not moved into the statement dashboard.
- Expose `onStatementsClick: () -> Unit` at the reusable `ActivityMoneyActions`/`ActivityToolsDisclosure` call sites, and thread it through `AppScaffold` and `ActivityScreen` from the navigation owner. In `StatementImportSheet`, make `onOpenStatements: () -> Unit` a parent callback for the **normal, non-statement-only** save/open path; close its sheet before navigating. Navigate to `statement-activity` with `launchSingleTop` to avoid a self-stack when an action originates inside that route. The `statementOnly=true` flow should continue to dismiss/refresh the existing Statements route. `StatementActivityDialog` is currently referenced from **three** sites (`PesaTrackApp`, `CashEntryScreens`, `StatementImportScreens`); remove it only after all three are safely migrated.

- [ ] **Step 1: Add UI tests** asserting exactly four global tab labels (Home, Activity, Plan, More), More selected, no Accounts tab or second bottom bar, correct Back from category/archive to Summary and then the **previous route** (More or Activity), and no dialog overlay framing. Test all three entry paths—More, Activity tools and normal import-sheet completion—reach the same `statement-activity` destination rather than opening a nested `Dialog`. Use `PesaTrackTheme` in light/dark modes.
- [ ] **Step 2: Compile Android UI test sources** with `./gradlew :app:assembleDebugAndroidTest`; CI/device execution is still manually gated. Expected test source compiles once implementation exists.
- [ ] **Step 3: Move the current content from `Dialog` to the route-hosted `Surface/Column`**, remove duplicate `systemBarsPadding`/bottom nav, preserve existing refresh, import, category review, global tab navigation through the scaffold, Back callbacks and all six page destinations. Route the Activity and normal import follow-up buttons through their parent navigation callbacks, closing any sheet first. Use exactly one PesaTrack header from `AppScaffold`.
- [ ] **Step 4: Recompile and inspect the nav/back UI tests**; verify `AppScaffold` isn't nested, and remove the old adapter/legacy nav only after zero references remain.
- [ ] **Step 5: Commit** `refactor(statements): use one native scaffold and route`.

### Task 5: Implement the approved single-wallet Statements overview

**Files:**
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementOverviewPage.kt` (replace existing card hierarchy in place)
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementReportVisuals.kt` (reuse/extend relevant chart and money components)
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementIntelligenceDashboard.kt` (one model passed to Summary; navigation callbacks)
- Test: `app/src/androidTest/java/com/kedevjimmy/pesatrack/ui/screens/StatementOverviewDashboardUiTest.kt` (new, distinct from **unmerged** PR #460)

**Interfaces:**
- `StatementOverviewPage(model: StatementInsightViewModel, onSelectScope: (StatementReportScope) -> Unit, onOpenCategories: () -> Unit, onOpenTrends: () -> Unit, onOpenArchive: () -> Unit, onOpenCoverage: () -> Unit, onImport: () -> Unit, onOpenTransactions: (StatementFlow?, String?) -> Unit)`; the host maps callbacks to existing `StatementPage` navigation. Avoid invented Activity callbacks.
- The hero shows `model.metrics.spentMinor` (classified spend), NOT `sourceCashFlow.netFlowMinor`. Secondary card shows classified income and a correctly labeled `Income − spending` difference **only with clear classification caveat**; raw Money in/out remain available in account/month details.

- [ ] **Step 1: Write Compose UI tests** with synthetic `StatementInsightViewModel` asserting visible labels `Statements`, `Total spending`, `Spending through the year`, `Where your money went`, `Statement archive`/source access and actual KES display. Explicitly assert `KES 1,126,400` is not required anywhere in production and that financial cards use passed data. Test each overview CTA callback once and ensure no duplicate Net Cash Flow block.
- [ ] **Step 2: Compile test sources**; expected red before the new signature and hierarchy exist.
- [ ] **Step 3: Implement the approved spacious lavender canvas, one violet-blue spending hero, two concise supporting numbers, a 12-month bar chart, ranked category rows and wallet/archive action** using `HomeReferencePalette`. Only show a real wallet selector when multiple imported wallets exist; do not fake an “All accounts” aggregate, KCB, Equity or planned options.
- [ ] **Step 4: Check light/dark and 320–393dp widths with large text**, 48dp targets, screen-reader labels for bars and amounts, unknown/empty/partial states. Run focused Compose test compilation and JVM Task 2/3 tests.
- [ ] **Step 5: Commit** `feat(statements): replace overview with approved premium wallet insights`.

### Task 6: Evidence-backed categories, trends, archive, coverage and month view

**Files:**
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementCategoryInsightPage.kt`
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementPeriodPage.kt`
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementCoveragePage.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementIntelligenceDashboard.kt` (existing CATEGORIES, TRENDS, BROWSE and added scope/page destinations)
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/domain/everyday/StatementInsightProjectionTest.kt` (extend)
- Test: `app/src/androidTest/java/com/kedevjimmy/pesatrack/ui/screens/StatementHistoryDrilldownUiTest.kt` (new)

**Interfaces:**
- `StatementCategoryInsightPage(categoryLabel: String, rows: List<StatementClassified>, onOpenTransactions: (String) -> Unit)` reuses `StatementExpenseCatalog.matchingExpenseRows`, which currently matches a **display category label** (not an internal category ID), for the selected account/scope. Keep the distinction explicit in the callback contract.
- `StatementPeriodPage(month: StatementHistoryReport.SelectedMonth, rows: List<StatementClassified>, onOpenRow: (StatementClassified) -> Unit)` renders only that month's selected source, real cash movements, count and statement entries.
- `StatementCoveragePage(year: Int, months: List<StatementHistoryReport.SelectedMonth>, onOpenMonth: (String) -> Unit, onImport: () -> Unit)` distinguishes imported, missing, alternative/overlapping and partial evidence.

- [ ] **Step 1: Add tests**: category expenses exclude transfers/refunds/unknown; selecting Dec 2025 from archive shows only Dec rows; imported Jan/Mar plus missing Feb show a visible gap, not a zero bar; a source imported for a partial range is labeled by its observed dates, not declared a complete month; duplicate August source months show only the chosen report month in totals while archive alternatives stay discoverable.
- [ ] **Step 2: Run focused projection/history tests** and compile Compose test sources; expected red until page callbacks and rendering exist.
- [ ] **Step 3: Build the category detail, monthly historical chart, source archive, year coverage matrix and specific-month details** from current stored `StatementActivityLine`/`SelectedMonth` information. Reuse existing BROWSE/archive delete/privacy actions; don't create another statement database, generic daily Activity feed or fabricated merchant identities.
- [ ] **Step 4: Wire category/month/source taps to their exact filtered destination**, preserve account scope, and test Back returns to the prior screen without resetting to all-history. Run the focused suites.
- [ ] **Step 5: Commit** `feat(statements): add scoped category and source-history drilldowns`.

### Task 7: Individual statement evidence and verified Activity linking

**Files:**
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementEvidenceDetailPage.kt`
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/data/local/StatementActivityLinkReader.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementIntelligenceDashboard.kt` (source-row click opens detail; existing category-edit action stays explicit)
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/PesaTrackApp.kt` (route callback to existing `transaction-detail/{transactionId}`)
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/domain/everyday/StatementActivityEvidencePolicyTest.kt` (extend)
- Test: `app/src/androidTest/java/com/kedevjimmy/pesatrack/ui/screens/StatementEvidenceDetailUiTest.kt` (new)

**Interfaces:**
- `StatementActivityLinkReader(context: Context).linkedTransactionId(row: StatementClassified, accountId: String): Long?` queries the **existing** `StatementReceiptEvidenceReader.read(listOf(row.line))`, then uses `StatementActivityEvidencePolicy.verified(...)`; it returns the single `transactionId` or `null`. Run it on `Dispatchers.IO` when a specific detail opens, not in every LazyColumn row.
- `StatementEvidenceDetailPage(row: StatementClassified, sourceLabel: String, categoryLabel: String?, linkedActivityId: Long?, onReviewCategory: () -> Unit, onOpenActivity: (Long) -> Unit, onBack: () -> Unit)` renders source date/time, original amount, description, masked reference, category/evidence, source month and optional original cells with privacy care.

- [ ] **Step 1: Extend receipt tests**: unique same account/amount/direction/reference/24-hour proof returns ID; two candidates, wrong account, wrong amount, stale date, different clearly named recipient, or structural relation returns null. UI tests confirm “View Activity record” only appears when ID is nonnull; opening original statement details has no write side effects.
- [ ] **Step 2: Run the focused Activity evidence test** and compile Compose tests; expected red for new link reader/detail API.
- [ ] **Step 3: Implement on-demand read-only verification and source-row detail**, obtaining candidates from `StatementReceiptEvidenceReader.read(listOf(row.line))[row.line.reference.uppercase(Locale.ROOT)]` and requiring `StatementActivityEvidencePolicy.verified` to return exactly one verified receipt with an ID. Route that verified ID to `transaction-detail/$id`; keep explicit `StatementCrossCategoryRepository.preview`/`rememberBoth` flow behind user review. Do not infer a link from `activityReceiptVerified` Boolean alone without resolving the exact ID.
- [ ] **Step 4: Run evidence/category tests and UI test source compilation**; confirm no ledger/category mutation on merely opening a row.
- [ ] **Step 5: Commit** `feat(statements): show provenance and safe Activity receipt links`.

### Task 8: Honest M-Pesa import, review and zero-history experience

**Files:**
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementEmptyLanding.kt`
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementImportWorkspace.kt` (presentation only)
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementImportScreens.kt` (only presentation/copy/navigation where necessary)
- Test: `app/src/androidTest/java/com/kedevjimmy/pesatrack/ui/screens/StatementEmptyLandingUiTest.kt` (update)
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/domain/everyday/StatementPdfActivityPolicyTest.kt` (existing regression gate)

**Interfaces:** Keep the existing `StatementImportSheet(onDismiss, onChanged, onPaycheckClick, statementOnly = true)` invocation and its wallet ownership/verification contracts; add the optional `onOpenStatements` callback from Task 4 only for other entry points. On Save, `StatementActivityStore.save` refreshes the archive; “Review cash movements in Activity” stays a separate, explicit path through existing reviewed duplicate-aware imports.

- [ ] **Step 1: Update empty-state UI tests** to assert a real working M-Pesa import CTA, on-device privacy explanation, source-history path, no dead M-Pesa tile, no enabled unsupported bank PDF/CSV actions and no “Coming soon” placeholders. Test light/dark and no-import counts.
- [ ] **Step 2: Compile test sources**; expected red before changes.
- [ ] **Step 3: Apply the approved pastel/rounded import-research visuals only where they represent actual state**. Preserve size/date/password/receipt proof checks, cancellation, error messages, selected wallet and source-only Save path. An incomplete or rejected file must not get a “Verified” badge or create records.
- [ ] **Step 4: Check separate paths**: verified statement Save does not post Activity, explicit Activity review still uses existing import match checks, failed import leaves source archive intact; rely on existing domain tests and instrumented end-to-end acceptance when separately authorized.
- [ ] **Step 5: Commit** `feat(statements): polish supported M-Pesa import and empty states`.

### Task 9: Final screen, accounting and regression acceptance

**Files:**
- Modify tests from Tasks 1–8 only as needed for verified regressions.
- Update: `CURRENT_STATE.md` **only after** behavior and verification status are known; do not claim device acceptance from build-only checks.

**Interfaces:** No new product feature. Freeze view models and navigation from earlier tasks; remove abandoned duplicate UI code and imports only after `rg` shows no references.

- [ ] **Step 1: Source review** with `git diff --check`, `git status --short`, `rg` for old Dialog/`StatementBottomNavigation`, `rg` for `TODO`/`Coming soon`/demo KES literals in newly implemented Statements files, and diff inspection for unexpected Activity/Home/parser/security/manifest changes.
- [ ] **Step 2: Focused JVM tests**: `StatementReportScopePolicyTest`, `StatementInsightProjectionTest`, `StatementInsightModelsTest`, `StatementHistoryReportTest`, `StatementTrendBucketsTest`, `StatementFinancialPolicyTest`, `StatementReceiptEvidenceTest`, `StatementActivityEvidencePolicyTest`, `StatementCategoryMemoryPolicyTest`, `StatementExpenseCatalogTest`.
- [ ] **Step 3: Broader Android checks**: `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest` using a properly populated Gradle cache or CI. Capture exact exit codes and test results; an environment configuration failure is **not** a successful build and not an app test failure.
- [ ] **Step 4: Visual QA** against all approved PDF boards relevant to Phase 1 (main visual language, M-Pesa account, category/archive/month/evidence, import/review/coverage). Compare section by section on a Redmi-like viewport, light/dark, larger fonts, landscape/insets and back gestures. Check no duplicate surfaces, clipped KES amounts or action dead ends. **Never use fictional mockup numbers as expected production values.**
- [ ] **Step 5: Request separate authorization before actual emulator/Redmi acceptance**; repository CI intentionally pauses automatic instrumentation. If not authorized, mark the UI and PDF import journey **device-unverified**, even when unit tests/build pass.
- [ ] **Step 6: Final feature-branch review and concise report**: base/head SHAs, files changed, numeric correctness tests, UI parity, CI status, any remaining gaps, and only then discuss PR/merge/release with James. No automatic merge, release or workflow change.

## Separate release blocker (not an automatic UI refactor)

**Backup schema compatibility:** `StatementActivityStore.Db` uses SQLite archive version **4** while `StatementBackupArchive.SCHEMA_VERSION` and `assertSchema` still enforce **3**. `PesaBackupManager.buildPayload`/restore attach the archive and call this assertion. Before treating encrypted backup/restore as reliable, make this a **separately reviewed security/compatibility fix**: reproduce in `app/src/androidTest/java/com/kedevjimmy/pesatrack/security/StatementArchiveBackupIntegrationTest.kt`, establish v3 archive payload backward compatibility, restore into initialized v4, and verify fresh backup export/restore plus no-loss rollback. Do **not** simply change a version constant and silently reject old encrypted backups. Instrumentation/device execution remains opt-in. This is a pre-release gate, not permission to rework financial code while implementing visual screens.

## Future phases (explicitly excluded from Phase 1)

- Multi-institution structured file profiles for KCB, Equity and other banks, each admitted only with real source-layout/receipt validation and private-device checks.
- Genuine all-account aggregation and mirror-transfer dedup with source provenance; current `StatementReportReader.read` deliberately refuses mixed accounts.
- All-accounts dashboard institution cards and spending totals only once underlying account coverage and deductions are trustworthy; reuse `StatementInsightViewModel`/shared Compose components rather than creating `KCBStatementsScreen.kt`, `EquityStatementsScreen.kt`, etc.
- Any provider-specific PDF extraction or universal “Upload any statement” promise is a separate product/accuracy effort and never activated as a Phase 1 placeholder.

## Handoff/exit criterion

Phase 1 is complete only when an actual supported M-Pesa statement can be imported through the existing verified on-device pipeline, its **real** figures appear in the premium Statements overview, year/month/category/archive/coverage/detail views agree on totals and period, category review works with existing Activity evidence, the correct global four-tab chrome appears once, and every visible action has a truthful destination. Device and backup gates remain explicitly reported until authorized and verified.
