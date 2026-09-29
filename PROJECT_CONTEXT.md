# PesaTrack Project Context

Updated: 2026-09-29

## Purpose

PesaTrack is a personal Android finance companion designed around Kenyan M-Pesa usage. It is intended for private sideloading, not Play Store distribution.

The application should automatically turn M-Pesa activity into a reliable local money ledger, learn deterministic recipient/category rules, track monthly budgets and commitments, show savings/goal progress, and surface useful context such as budget pace and safe-to-spend amounts.

## Product direction already agreed

The app should not behave like a judgmental expense tracker. Spending that follows the user's plan is not inherently bad. The core experience should distinguish current money, committed money, savings/reserves, genuinely free money, and safe-to-spend pace.

Signature concepts planned:

1. Available to Live / genuinely free money.
2. Safe today amount based on remaining cycle and protected commitments.
3. Smart recipient rules learned from explicit user corrections.
4. M-Pesa-native transaction types: Send Money, Buy Goods, PayBill, airtime, cash withdrawal, reversal, fees and transfers.
5. Goals with progress and impact simulation.
6. Monthly/annual insights that explain what changed, rather than dumping charts.
7. Local-first privacy and optional biometric protection.

## Technical choices

- Native Android.
- Kotlin + Jetpack Compose.
- Package: `com.kedevjimmy.pesatrack`.
- Minimum SDK: 24.
- Target SDK: 36.
- Compile SDK: 36.
- Compose BOM: 2026.04.01 (Compose 1.11 generation, chosen for stable API 36 compatibility).
- Material 3 stable.
- Room and WorkManager are planned for the next phase and will be added when their persistence/background code is introduced.
- Source of truth must remain deterministic local data, not AI.
- Any future AI should explain already-derived facts, never invent transaction truth.

## Design system

Light mode uses warm ivory, clean white surfaces, deep navy text, emerald primary, soft mint containers and restrained amber attention states.

Dark mode uses a near-black blue/green background, lifted dark teal surfaces, mint primary, warm amber secondary and off-white text.

Typography currently uses Android's bundled sans-serif family with a strong weight/size hierarchy so the app stays offline and does not depend on downloadable fonts.

## Navigation

Primary tabs:

- Home
- Activity
- Plan
- Goals

Insights are intentionally embedded where they matter instead of becoming a fifth analytics dump.

## Reference UI implementation

The full visual alignment pass based on the approved PesaTrack concept screens is merged into `main` and verified by Android CI.

Reference-aligned scope:

- refined light/dark palette and typography hierarchy
- custom PesaTrack shield/track app icon and in-app brand mark
- reusable landscape hero treatment and premium card language
- redesigned onboarding
- redesigned Home
- redesigned Activity
- redesigned Plan
- redesigned Goals
- Transaction Details inner page
- Goal Details inner page
- Activity -> Transaction Details navigation
- Goals -> Goal Details navigation

All financial values in these screens remain preview data until the local data layer is connected. The reference UI passed Android CI on the PR branch and again on merged `main` (run #11).

## Redmi responsiveness and appearance correction

A second visual pass was implemented from real Redmi Note 14 screenshots after the first reference-UI build exposed narrow-screen and dark-mode problems.

Implemented on this pass:

- phone-width metric areas switch from four squeezed columns to 2x2 layouts where appropriate
- Home summary cards use vertical content hierarchy so values and status pills do not collide
- Activity transaction rows prioritize recipient + amount and move category/time into a secondary line
- Plan budget rows use full-width progress bars instead of three competing horizontal columns
- Transaction Details uses a 2x2 metadata grid on phone widths and removes the decorative sun behind the amount
- Goal Details stacks its actions on narrow phones
- semantic green/amber/blue/violet/red containers now have dedicated dark-mode surfaces and foregrounds
- hero landscape artwork automatically follows the active theme
- added Profile screen
- added Settings screen
- added persistent System / Light / Dark theme preference stored locally on-device
- Profile/Settings remain inner pages and do not pretend to be one of the four primary money tabs

The correction was validated with Android unit tests and debug APK assembly on CI before merge.

## M-Pesa ingestion foundation

The first real data-ingestion slice is implemented using Android Notification Listener access rather than requiring restricted SMS permissions.

Implemented:

- `MpesaNotificationListenerService` receives newly posted notifications after the user explicitly grants Notification Access.
- Notification bodies are filtered locally; unrelated notifications are ignored.
- M-Pesa confirmation text is parsed into structured fields using the deterministic `MpesaParser`.
- Person-to-person counterparty parsing prioritizes the recipient/sender name and strips trailing full or masked phone tokens.
- Parsed transactions are stored in an app-private SQLite ledger.
- Raw M-Pesa notification/SMS bodies are **not persisted**.
- Transactions are deduplicated by transaction code when available and a deterministic SHA-256 fingerprint as fallback.
- Stored fields include transaction code, type, amount, fee, counterparty name, optional balance, observed time and ingestion source.
- If a confirmation includes a new M-Pesa balance, PesaTrack stores it as a **last known observed balance** with timestamp. It is not treated as continuously live.
- Settings now includes an M-Pesa Connection screen with Notification Access controls and local diagnostics.
- The Android app still declares no Internet permission; cleartext traffic is explicitly disabled.
- Unit tests cover parser name cleanup, M-Pesa notification detection and transaction fingerprint stability.

Why Notification Access first:

- Modern Android treats `READ_SMS` and `RECEIVE_SMS` as restricted permissions whose availability depends on installer/platform policy.
- Notification Listener access is explicitly granted by the user in Android Settings and is suitable for testing real-time M-Pesa confirmation capture on the Redmi Note 14.
- Historical SMS import remains a separate follow-up experiment after the real-time path is proven on-device.

The first code-complete ingestion build passed Android CI run #17.

## Smart money rules and real ledger phase

The interrupted smart-money phase was recovered from branch `feature/smart-money-rules-ledger`, completed, and validated before merge.

Implemented behavior:

- Persistent Smart Rules are stored locally and survive app restarts.
- First-run flow is now Onboarding -> Money Setup -> Home. Even skipping onboarding still passes through Money Setup; the user can explicitly defer rule creation from there.
- Money Setup supports three rule types:
  - salary/income source
  - recurring expense
  - own-account transfer
- Salary rules are not hardcoded to any bank or employer. The user defines notification sender/title text and/or a message phrase.
- Salary rules can assign income to the same planning month or the following planning month. This supports salary received at the end of one month being used to fund the next month's plan.
- Configured non-M-Pesa money notifications can be parsed for an amount after a saved rule matches.
- M-Pesa rules can match normalized counterparty names.
- One-off/unrecognized transactions are intentionally labeled **Needs review** rather than guessed.
- Activity is backed by the real local transaction ledger rather than preview/demo transactions.
- Activity provides real captured totals for spending, income, fees and net cash flow.
- Activity filters include All, Expenses, Income, Transfers and Needs review.
- Tapping Activity opens the exact stored transaction by database ID.
- Transaction Details is backed by the stored record and supports:
  - one-time manual classification
  - optional "use this for future payments too" smart-rule creation
  - marking an item as an own-account transfer
  - excluding an item from budget calculations
- Income direction is preserved during manual review unless explicitly changed to a transfer; descriptive categorization cannot accidentally turn captured incoming money into spending.
- Savings and own-account movements are treated as transfers rather than consumption spending.
- Transaction records now persist direction, category, review state, matched rule ID, planning month and optional referenced reversal code.
- Reversal-aware summaries omit a known reversed original transaction when the reversal identifies the original transaction code.
- Withdrawals default to transfer-to-cash rather than immediate spending.
- Existing unreviewed transactions can be re-evaluated when a new counterparty rule is saved.
- Planning month calculations remain compatible with the app's Android API 24 minimum.

Tests added/expanded:

- counterparty smart-rule matching
- amount caps on person rules
- configured salary notification sender + message matching
- disabled notification rules
- same-month vs next-month salary planning
- generic configured bank-money notification parsing

The final implementation state passed Android CI run #23 before the documentation update.

## Historical SMS backfill and canonical dates

PesaTrack now separates the date when money actually moved from the time the app learned about the transaction.

Implemented:

- Database schema v3 adds `occurred_at_epoch_ms`.
- Existing ledger rows migrate safely by using their previous received timestamp as the initial occurrence timestamp.
- New M-Pesa notifications resolve the transaction date embedded in the confirmation when the format is recognized.
- If the embedded date cannot be parsed, PesaTrack safely falls back to the source notification/SMS timestamp.
- Activity monthly calculations now use `occurred_at_epoch_ms`, not import/observation time.
- Activity and Transaction Details display the true transaction date.
- Historical SMS Import is available from Settings with:
  - This month
  - Last 30 days
  - Last 90 days
- Historical import queries the phone SMS provider only after explicit `READ_SMS` permission is granted.
- Imported M-Pesa messages flow through the same deterministic parser, Smart Rules, Needs Review, reversal and duplicate handling as live notifications.
- Configured bank/salary SMS messages can also be imported when they match a saved notification smart rule.
- Imported messages keep their original SMS/transaction date and do not become transactions dated on the day of import.
- Re-importing the same history is safe because transaction-code/fingerprint deduplication remains active.
- Raw SMS bodies are processed in memory and are not stored in the local ledger.
- If Android or the installer refuses the hard-restricted SMS permission, the importer reports that limitation and does not attempt to bypass Android security; real-time Notification Access remains usable.
- Salary planning wording is now `Month it arrives` versus `Following month` to distinguish transaction date from budget-cycle assignment.

Date semantics:

1. `occurred_at_epoch_ms` = when the financial transaction actually happened.
2. `received_at_epoch_ms` = when the phone/source received or posted the message.
3. `created_at_epoch_ms` = when PesaTrack stored/imported the structured record.
4. `planning_month` = which budget cycle the transaction, especially income, is assigned to.

Home and Plan must use these semantics and must never group historical imports by import time.

The implementation passed Android CI run #26 before the documentation update.

## Salary-driven pay cycles, My Accounts and simplified setup

PesaTrack now uses salary events to define budgeting cycles instead of assuming a fixed calendar payday.

Implemented:

- Added persistent My Accounts with default M-Pesa and Cash accounts.
- Users can add their own bank, savings, wallet or other accounts; no bank/employer is hardcoded.
- Smart Rule creation is preset-first to reduce complexity:
  - Monthly salary
  - Rent
  - Internet
  - Electricity
  - Food / lunch
  - Transport
  - Family support
  - Savings
  - My own transfer
  - Other recurring
  - Custom
- Presets automatically choose the normal category/direction behavior. The user only provides the recognition text, optional custom label, account(s), and an optional planned amount.
- Rules can recognize either an M-Pesa counterparty name or a configured notification sender/message phrase.
- Salary rules start pay cycles. A fixed payday is not required.
- Salary rules can optionally define a usual pay window, including a cross-month window such as 24th -> 1st. The window is a confidence check, not the salary identifier.
- The first recognized salary starts a pay cycle.
- The next accepted salary closes the prior cycle and starts the next one, regardless of whether it arrives on the 24th, 31st, 1st, or another normal date.
- Tracked remainder from the closed cycle becomes carryover into the next cycle.
- Salary-like credits less than 14 days after the current cycle start are held for confirmation instead of silently opening another cycle.
- Salary-like credits outside an optional usual pay window are also held for confirmation.
- A pending salary can be confirmed as:
  - Start new cycle
  - Extra income in the current cycle
- This supports unusual December pay dates without hardcoding December behavior.
- Pay-cycle timing policy is unit-tested for 24th/31st/1st ranges, early December salary, no-fixed-window salary, and duplicate-like credits.
- Transactions now store their primary account, optional linked account, pay-cycle ID and salary-cycle review state.
- Existing M-Pesa transactions migrate to the M-Pesa account; withdrawals link M-Pesa -> Cash.
- Cash withdrawal remains a transfer, not spending.
- Own-account movements remain transfers and do not increase income or spending totals.
- Historical M-Pesa transactions are assigned to M-Pesa even though their ingestion source is historical SMS.
- Existing income rules migrate into salary/pay-cycle rules and stop relying on the old calendar-month offset.
- Historical imports rebuild pay cycles once at the end of the batch.
- Recurring fixed commitments are protected immediately when a pay cycle starts.
- Flexible rules such as food/transport only become protected commitments when the user explicitly enters a planned amount.
- Planned savings can also reserve money until the savings transfer happens.
- Home is now backed by the real pay-cycle ledger:
  - Available to Live
  - salary + carryover starting funds
  - commitments
  - deliberate savings
  - spending + fees
  - last-known M-Pesa balance with observation time
  - Needs Review count
  - salary confirmation card
- Plan is now backed by the same real pay-cycle ledger and shows protected commitments, paid progress and pay-cycle accounting.
- Safe Today is shown only after PesaTrack has at least one completed cycle from which to estimate cycle length; otherwise it says Learning.
- Calendar-month Activity remains separate from pay-cycle planning.
- The old Historical Import wording is now more explicit:
  - Settings -> Import M-Pesa SMS history -> Request SMS access

The first full implementation build reached green unit tests and debug APK assembly on Android CI run #32 before the final wording/context update.

## Progressive onboarding and teach-once learning

The first-run experience is now intentionally simpler than the advanced Smart Rules manager.

Implemented:

- First-time users are detected through the existing persistent setup-complete state.
- The original dense onboarding/setup flow is replaced by:
  1. What usually funds your month?
  2. Where does that money arrive?
  3. Identify the salary/source.
- Main source choices include Salary, Business income, Freelance/side income, Multiple income sources and Other.
- Arrival choices include Bank account, M-Pesa and Cash.
- Bank selection is searchable and backed by an independent offline Kenyan-bank catalogue with a manual-name fallback.
- Common banks are surfaced first on a narrow phone layout; the catalogue does not control financial logic and can be updated independently.
- The bank catalogue is informed by CBK-regulated commercial-bank directories, while manual fallback prevents setup from depending on catalogue completeness.
- Choosing a bank no longer exposes technical fields such as notification title, direction, planning month or rule internals.
- With SMS permission, the salary step scans recent SMS from the selected bank for likely incoming credits and presents a small set of candidates.
- Candidate detection uses incoming-credit language plus KES/Ksh amounts; salary amount and payday are not used as the persistent salary identifier.
- When a likely employer/originator can be extracted, it is prefilled as "Who pays you?" and the user may correct it.
- A user correction takes precedence over the inferred originator.
- The confirmed salary example creates the persistent salary rule automatically; no redundant "auto-identify future salary?" question is shown.
- After salary teaching, up to roughly 100 days of SMS history can be imported immediately through the existing local historical importer when permission is available.
- Historical matching salary events can therefore rebuild earlier pay cycles automatically.
- Cash salary is not guessed from phone data; the app explicitly states that cash cannot be auto-detected.
- Income-source and arrival choices are persisted locally even when the source is not Salary.
- First-run controls use a single-column Redmi Note 14 baseline, existing Material typography, and minimum interaction heights at or above Android's 48dp accessibility recommendation.

Teach-once transaction review:

- M-Pesa parsing now captures a full Kenyan counterparty phone number when one is present and unmasked.
- Database schema v5 stores transaction counterparty phone, exact counterparty rules, and exact phone rules.
- New learned rules can use:
  - exact normalized full name
  - exact normalized phone number
- Loose first-name learning is intentionally avoided.
- If both phone and exact name are known, phone is strongest; exact name is used as a fallback only when a future message lacks a phone.
- The first manual review of a strong identity now asks:
  - Only this one
  - Remember
- "Remember" saves the rule and immediately reclassifies matching existing Needs Review transactions.
- The personally reviewed transaction remains USER_CLASSIFIED; retroactively matched records become AUTO_CLASSIFIED through the saved rule.
- The UI reports how many matching pending transactions were classified.
- No manual refresh is required.
- One-word person-like identities are not automatically learned; merchant-like PayBill/Buy Goods identities can still be learned exactly.
- Existing legacy contains-based rules remain supported for backwards compatibility.

Tests added/expanded:

- bank incoming-credit candidate parsing
- full-name exact matching
- first-name collision avoidance
- phone identity normalization/matching
- M-Pesa counterparty phone parsing

The implementation passed Android CI run #42 before the final bank-order/context update.

## Optional history import and multi-income first-run refinement

The progressive onboarding was simplified further so the user controls whether any historical SMS is read at all.

Implemented:

- First-run income selection is now **multi-select** rather than a single "main income" choice.
- Available sources:
  - Salary
  - Business income
  - Freelance / side income
  - Other regular income
- The previous separate "Multiple income sources" option was removed because selecting multiple sources directly is clearer.
- Only selected income sources receive follow-up questions.
- Each selected income source is configured independently, one at a time, so Salary can arrive in a bank while Business income can arrive in M-Pesa or Cash.
- Bank selection remains searchable with a manual fallback and reuses an existing My Account rather than creating duplicates.
- Historical SMS import during onboarding is explicitly optional.
- The user chooses exactly one of:
  - Don't import messages
  - Import 1 month
  - Import 2 months
  - Import 3 months
- Three calendar months is the maximum onboarding look-back.
- Month labels are generated from the phone's current date, not hardcoded. For example, when the device is in September, three months means September, August and July.
- The import boundary is the first day of the oldest selected calendar month, not an arbitrary 30/60/90-day approximation.
- Selecting "Don't import messages" does not request SMS permission.
- Selecting 1-3 months explains why message access is useful before requesting permission.
- SMS access remains optional; denial does not block setup.
- The onboarding permission screen includes an App Settings shortcut and explains that Android may require the user to enable "Allow restricted settings" themselves. PesaTrack never attempts to bypass or silently enable restricted settings.
- If SMS access is granted and Salary -> Bank is configured, PesaTrack searches only the user-selected history window for likely incoming credits from the chosen bank.
- If a salary candidate is found, the user can select the real example and PesaTrack uses its stable payer/originator signal rather than amount or payday.
- If no matching salary message is found, SMS access is skipped/blocked, Salary arrives through M-Pesa/Cash, or the user explicitly chooses manual history, PesaTrack shows manual salary cards only for the selected months.
- Manual history fields contain:
  - salary amount
  - actual day salary arrived
- A selected month may be left blank when salary had not arrived yet or the user does not want to add it.
- Manual salary dates are validated against the real calendar month; impossible dates are rejected.
- Manual salary history is inserted as structured local income using the saved salary rule and real occurrence timestamp so reconstructed pay cycles start on the correct date.
- Manual salary entries do not persist a raw SMS body.
- Setup persists the chosen set of income sources, arrival route for each source, chosen bank names, and requested history depth.
- Non-salary sources are saved without inventing transaction-recognition rules; they can be taught later from real transactions.
- Existing teach-once review behavior from the previous phase remains intact: exact full-name/phone learning can immediately classify matching pending history.

Tests added:

- 1-month window uses only the current calendar month.
- 3-month window produces current month + previous two months.
- 0-month selection means no history import.
- manual salary dates reject impossible days for a selected month.

The implementation passed Android CI run #46 before the documentation update.

## Fuliza financing and duplicate-spend correction

Fuliza is treated as financing metadata attached to the real M-Pesa payment, not as a second purchase.

Implemented:

- Added a dedicated Fuliza parser for confirmation messages such as:
  - Fuliza M-PESA amount
  - Access Fee charged / charge of
  - Total Fuliza M-PESA outstanding amount
  - optional available Fuliza limit when present
- Generic M-Pesa parsing explicitly refuses Fuliza drawdown confirmations, preventing them from entering the transaction ledger as ordinary transactions.
- Database schema v6 adds a separate `fuliza_events` store plus attached payment metadata:
  - financed amount
  - Fuliza access fee
  - outstanding Fuliza amount after the event
  - Fuliza event transaction code
- A Fuliza event is conservatively linked to the real outgoing payment:
  - exact payment amount + normal M-Pesa fee match is preferred
  - a partial-shortfall match is allowed only when there is exactly one plausible outgoing payment within a three-minute window
  - ambiguous matches remain unlinked rather than guessed
  - out-of-order pairing is restricted to exact matches
- Supported payment candidates are Send Money, Pay Bill, Buy Goods and Airtime.
- The Fuliza financed principal is never counted as new income or additional spending.
- The original merchant/person payment remains the single spending transaction.
- Normal M-Pesa transaction fees remain separate.
- Fuliza access fees are included exactly once in monthly Activity and pay-cycle fee totals, even if a Fuliza event cannot be confidently linked.
- When a link is available, Activity annotates the real payment with the Fuliza financed amount and access fee rather than displaying a second purchase row.
- Transaction Details explains that the payment was funded partly by Fuliza and displays:
  - financed amount
  - Fuliza access fee
  - outstanding Fuliza balance after the event when present
- Historical SMS import routes Fuliza through the financing store and reports Fuliza financing messages separately from new transactions.
- Re-importing historical SMS can repair Fuliza confirmations previously ingested by older builds as generic `OTHER` transactions:
  - the exact legacy row is identified by the Fuliza confirmation transaction code
  - it is removed from the spending ledger
  - the message is re-stored as Fuliza financing metadata
  - pay-cycle totals are rebuilt
- Raw Fuliza SMS bodies are not persisted.
- Available Fuliza limit is captured when present in a supported Fuliza message but is not yet presented as spend or a live credit balance.

Accounting example:

- Real payment: KES 160
- normal M-Pesa transaction fee: KES 7
- Fuliza financed amount: KES 167
- Fuliza access fee: KES 1.67

PesaTrack records one KES 160 payment, KES 7 normal transaction fee, KES 1.67 Fuliza access fee, and KES 167 as financing metadata/debt context. It does not record KES 160 + KES 167 as two expenses.

Tests added:

- common Fuliza confirmation format
- access-fee "charge of" variant
- generic M-Pesa parser rejects Fuliza confirmations
- exact payment + M-Pesa-fee pairing
- unique partial-shortfall pairing
- ambiguous shortfall refusal
- distant payment refusal

The implementation passed Android CI run #49 before this documentation update.

## Simplified review queue and one-off triage

Transaction review is now intentionally conservative and lower-effort.

Implemented:

- Unknown person/merchant identities are never assigned Shopping/Rent/etc. from a guessed name.
- For unruled outgoing Send Money, Pay Bill and Buy Goods transactions:
  - one occurrence of an exact identity within a calendar month defaults to `Other`
  - two or more occurrences of that exact identity in the same month move into `Needs Review`
- Phone number is the strongest identity when an unmasked Kenyan number is available.
- Otherwise the complete normalized counterparty name is used; the system does not reduce a person such as "Joseph Mwango" to "Joseph".
- Activity reconciles this policy across the current month plus the previous two calendar months, so already-imported recent history is cleaned up without re-importing.
- Changes made by triage rebuild pay-cycle calculations so Home/Plan do not show stale totals.
- Activity now has a compact `Review` filter and groups pending records by identity.
- A repeated identity is represented by one review card showing:
  - identity/name
  - number of matching transactions
  - combined amount
- Tapping the group opens one representative transaction rather than requiring the user to review every matching message.
- Existing teach-once behavior remains:
  - choose a category
  - choose `Only this one` or `Remember`
- `Remember` can now retroactively update both:
  - matching Needs Review records
  - matching one-off records that had previously defaulted to Other
- User-classified and excluded transactions are never overwritten by automatic triage.
- Transactions already controlled by a saved Smart Rule are excluded from one-off/repeat triage.
- Fuliza financing metadata and duplicate-spend handling are preserved unchanged.

Tests added:

- phone identity precedence
- exact full-name normalization
- one occurrence -> Other
- repeated identity -> teaching/review

Android CI run #52 passed unit tests, debug APK assembly and artifact upload before this documentation update.

## Temporary APK delivery while GitHub Actions artifact quota is blocked

Status on 2026-09-28: **implemented and verified**.

Current problem:

- GitHub Actions can successfully build/test PesaTrack, but the final `actions/upload-artifact` step can fail with `Failed to CreateArtifact: Artifact storage quota has been hit`.
- PesaTrack already uses `retention-days: 3` for its debug APK artifact.
- Old workflow runs were checked on 2026-09-28. PesaTrack itself had no completed runs older than the three-day cutoff to remove.
- The account-level storage pressure was also affected by NSE-Watcher; its old runs were cleaned separately.
- GitHub's Actions artifact-storage accounting can lag after deletion, so waiting for the usage counter to recalculate should not block PesaTrack development.

Temporary delivery decision:

Until the Actions artifact quota has recalculated, CI builds and tests normally but **does not depend on Actions artifact storage for the APK**. Successful `main` builds publish the debug APK as a GitHub **Release asset** instead.

Implemented temporary model:

1. Keep the existing `Android CI` trigger and all normal unit-test / debug-build steps.
2. On a successful push to `main`, skip or temporarily disable `actions/upload-artifact@v4`.
3. Use one rolling private prerelease/tag, recommended name: `ci-apk`.
4. The release should contain one current asset, recommended name: `PesaTrack-debug.apk`.
5. Each successful `main` build replaces that same asset rather than creating a new release per commit.
6. A failed build must never replace the existing known-good APK.
7. Pull-request builds should test/build but should not publish or replace the rolling release APK.
8. Once GitHub Actions artifact quota reporting has recovered, the project may return to the normal 3-day `upload-artifact` path and remove the temporary Release upload.

Recommended implementation shape:

```yaml
permissions:
  contents: write

# ...existing tests/build steps...

- name: Publish rolling debug APK
  if: success() && github.event_name == 'push' && github.ref == 'refs/heads/main'
  env:
    GH_TOKEN: ${{ github.token }}
  run: |
    gh release upload ci-apk \
      app/build/outputs/apk/debug/app-debug.apk#PesaTrack-debug.apk \
      --clobber
```

Important setup note:

- The workflow creates the `ci-apk` prerelease automatically if it does not already exist, then reuses it.
- The release description should explicitly say that it is a **rolling CI APK** and that the attached APK is the authoritative current asset; the source archive attached to the static release tag is not the authoritative build source.
- Verification completed on Android CI run #259: unit tests passed, debug APK assembly passed, and the rolling Release upload passed.
- Do not create a new release for every commit; that would just create another form of build clutter.
- GitHub Release assets are separate from Actions artifacts. GitHub currently allows up to 1000 assets per release, each asset under 2 GiB, with no stated total release-size or bandwidth limit.

How the user gets the APK during this workaround:

1. Open the PesaTrack repository.
2. Open **Releases**.
3. Open the rolling `ci-apk` prerelease.
4. Download `PesaTrack-debug.apk`.
5. The file should always be the APK from the latest successful `main` build that completed the publish step.

Handoff rule for future chats:

- Read this section before changing CI.
- The Release workaround is active in `.github/workflows/android-ci.yml`.
- Current rolling release: tag `ci-apk`, prerelease title `PesaTrack Rolling CI APK`, asset `PesaTrack-debug.apk`.
- The verified release asset from run #259 is about 19.8 MB and is replaced with `--clobber` after each successful `main` build.
- If Actions artifact uploads are working again, the project may return to the normal 3-day artifact retention unless the user explicitly wants the rolling Release workflow to remain permanent.

## Entity intelligence and automatic Activity review

Status on 2026-09-29: **local intelligence is merged and the privacy-safe remote enrichment path is deployed, verified, and active in rolling APK builds.**

### Local entity intelligence already merged

PR #66 introduced the first automatic-review layer:

- Generic `EntityIntelligence` and pluggable `EntityEnrichmentProvider` architecture.
- Local/offline Kenyan seed profiles for representative merchants and financial entities.
- High-confidence eligible merchant payments can auto-classify without user review.
- Current trusted examples include:
  - Kenya Power / KPLC -> Electricity
  - Naivas / Quickmart / Carrefour -> Groceries
  - Java House -> Eating out
  - Uber / Bolt -> Transport
  - Netflix / Showmax -> Entertainment
- Financial names such as M-Shwari, Branch, Tala, Equity and KCB are identity/enrichment knowledge only. Their names alone must never turn a transaction into an expense.
- Personal names are not resolved by the entity registry.
- Activity no longer contains a company-by-company merchant badge if/else chain; canonical entity evidence and generic category icons are used instead.
- Review triage only touches unresolved `Other` / `Uncategorized` transactions, so a confident automatic category is not overwritten later.
- PR #66 CI #269 passed; post-merge main CI #270 passed and updated the rolling Release APK.

### Privacy-safe online enrichment boundary

PR #68 adds the second layer:

Android side:

- `PESATRACK_ENTITY_ENRICHMENT_URL` is a build-time endpoint. It defaults to blank, so ordinary CI builds do not perform remote enrichment until a real endpoint is deliberately configured.
- Only the extracted counterparty/merchant descriptor is considered for remote lookup.
- Before networking, `EntityDescriptorPrivacyPolicy` rejects:
  - likely personal names
  - Kenyan phone numbers
  - M-PESA-style 10-character transaction codes
  - long account/reference numbers
  - email addresses
- The full SMS body, amount, balance, transaction code, phone number and account number must never be sent to the enrichment service.
- Lookup runs asynchronously after local ingestion; failures/timeouts never block storing the transaction.
- Successful results are cached locally for 30 days in `entity_enrichment_cache` (database schema v18).
- Cached HIGH-confidence safe merchant results are consulted synchronously during new transaction classification, after Smart Rules/local entity intelligence and before fallback Review. This means a merchant already learned online can auto-review immediately even while offline, until its cache entry expires.
- In-flight requests are deduplicated.
- Opening Activity Review can enqueue older unresolved merchant transactions as well as new transactions.
- Remote enrichment may auto-review only when all of these are true:
  - transaction is still unresolved
  - outgoing merchant/payment type
  - no saved Smart Rule controls it
  - user has not manually classified or excluded it
  - remote confidence is HIGH
  - returned category is in the conservative safe expense allow-list
- Finance/loan categories, ambiguous matches, generic utilities and uncertain results remain in normal Review.

Worker side:

- Source: `worker/entity-enrichment/`.
- Endpoint contract: `POST /v1/entity/resolve` with only `{"descriptor":"..."}`.
- The Worker repeats the same privacy checks before contacting any external provider.
- Current adapter uses Logo.dev transaction enrichment with server-side secret `LOGO_DEV_SECRET_KEY`.
- The Worker intentionally does not map generic `Utilities` automatically because that does not safely distinguish electricity, water and internet.
- The app remains the final auto-review policy gate even when the Worker returns a category.
- CI now runs `node --check worker/entity-enrichment/src/index.js` before Android tests.
- PR #68 CI #273 passed Worker syntax validation, unit tests and debug APK assembly before this documentation update.

Activation is now verified:

1. GitHub contains the Cloudflare deployment credentials required by the deploy workflow.
2. The existing Cloudflare Worker named `pesatrack` is deployed successfully.
3. `LOGO_DEV_SECRET_KEY` is configured on that Worker and health checks report `providerConfigured: true`.
4. The verified production resolve endpoint is:
   `https://pesatrack.jameswaweru399.workers.dev/v1/entity/resolve`
5. Android CI/release builds embed that public endpoint directly as `PESATRACK_ENTITY_ENRICHMENT_URL`.
6. Real Kenyan merchant descriptors should still be validated on-device before broadening the automatic category allow-list.

Never commit a provider secret to GitHub, put it in Gradle source, or embed it in the APK.

Deployment automation added after PR #69:

- `.github/workflows/entity-enrichment-deploy.yml` provides repeatable Cloudflare Worker deployment and health verification.
- It requires repository secrets `CLOUDFLARE_API_TOKEN` and `CLOUDFLARE_ACCOUNT_ID`.
- `LOGO_DEV_SECRET_KEY` remains a Cloudflare Worker secret; it is not copied into the APK or repository.
- The Worker exposes `GET /v1/entity/health` so deployment can verify the correct privacy-mode service is live and that the provider secret is configured.
- Android CI and Worker deployment both run `worker/entity-enrichment/test.mjs`, which verifies health, rejects phone-bearing descriptors and likely personal names, and confirms a safe commercial descriptor reaches the provider boundary.
- GitHub's workflow token cannot write Actions repository variables in this repository (`HTTP 403 Resource not accessible by integration`), so the verified production endpoint is embedded directly into Android CI/release builds instead.


## Current implementation

Baseline: `main` after the verified reference UI merge

Bootstrap scope:

- Android/Gradle structure.
- Compose Material 3 shell.
- light/dark theme foundations.
- reference-aligned onboarding/privacy screen.
- reference-aligned Home screen.
- reference-aligned Activity screen.
- reference-aligned Plan screen.
- reference-aligned Goals screen.
- Transaction Details screen.
- Goal Details screen.
- custom PesaTrack app icon/brand mark.
- Activity and Goals inner-page navigation.
- bottom navigation.
- deterministic `MpesaParser`.
- `SmartRuleEngine`.
- parser/rule unit tests.
- Android CI.
- debug APK artifact upload on successful CI.

Activity, Transaction Details, Home and Plan are connected to the persistent local ledger. Goals still contain preview values and must only be connected to deliberate goal allocations/savings transfers; unspent money must never be treated as automatically saved.

## Build rule

Do not call an implementation successful merely because code was written.

For every implementation phase:

1. Make a narrow, understandable change.
2. Run the relevant build/test.
3. If it fails, inspect the exact failure.
4. Fix the actual cause before continuing.
5. Only mark the implementation complete when the relevant check passes.

## Immediate next phase after Fuliza correction and optional-history onboarding

1. Install the latest debug APK on the Redmi Note 14 and clear app data so the real first-run flow can be tested from scratch.
2. Test a single-source path: Salary -> Bank -> real Kenyan bank -> Don't import messages, and verify setup completes without any SMS permission request.
3. Clear app data again and test Salary -> Bank -> Import 1/2/3 months; verify the month labels follow the current device month and never exceed three months.
4. If Android blocks SMS access, test the App Settings / restricted-settings guidance and confirm setup still continues manually.
5. With SMS access allowed, verify salary candidate discovery uses only the chosen history window and the actual bank sender on-device.
6. If no candidate is found, enter one or more manual salary months with amount + actual arrival day and verify Home/Plan reconstruct pay-cycle boundaries correctly.
7. Test a multi-income path such as Salary -> Bank plus Business income -> M-Pesa and confirm no irrelevant fields appear for either source.
8. Open Activity -> Review and verify repeated identities are grouped. Review one group, choose Remember, and confirm exact-name/phone matching cleans up the full group plus matching one-off Other history without first-name collisions.
9. Validate bank -> M-Pesa own-account transfers so movement between owned accounts never becomes duplicate income/spending.
10. After these real-device flows are stable, add month/pay-cycle history selection to Activity/Plan and connect Goals only to deliberate savings allocations.

## Important constraints

- Raw SMS content should not leave the phone.
- Never silently classify uncertain transactions as expenses; uncertain items go to Needs Review.
- Transfers between the user's own stores of money are not spending.
- Cash withdrawal is a transfer to Cash until cash is explicitly spent.
- Reversals must neutralize the original transaction rather than become fake income.
- Fees should be tracked separately.
- Do not fabricate account balances or financial insights.
- UI must remain readable on the Redmi Note 14 and adapt cleanly to larger Android form factors.

### Cloudflare Git-deploy incident and resolution

Historical failure observed before the final activation:

```text
Executing user deploy command: npx wrangler deploy
ERROR: Could not detect a directory containing static files (e.g. html, css and js) for the project
```

Root cause:

- Cloudflare Git Builds executed `npx wrangler deploy` from the repository root.
- At that point Wrangler did not have a root configuration pointing at the actual Worker entry file under `worker/entity-enrichment/`.
- Wrangler therefore did not resolve the Worker project correctly and fell into the wrong project/static-assets detection path.
- This failure was a deployment-path/configuration problem, not an Android or entity-intelligence problem.

Resolution sequence already merged:

- PR #73: added repository-root `wrangler.jsonc` so `npx wrangler deploy` from the repository root points to `worker/entity-enrichment/src/index.js`; Android CI also performs a Wrangler dry run from the root.
- PR #74: enabled Wrangler `keep_vars: true` so dashboard-managed Worker variables/secrets are not removed by later code deployments.
- PR #75: aligned the deploy workflow with the existing Cloudflare Worker name `pesatrack`.
- PR #76: verified the live Worker endpoint, confirmed the provider secret through the health endpoint, and activated that public endpoint in rolling Android APK builds.

Credential/setup state:

- The user reported adding `CLOUDFLARE_API_TOKEN` and `CLOUDFLARE_ACCOUNT_ID` to the GitHub repository secrets.
- The user reported adding `LOGO_DEV_SECRET_KEY` to the Cloudflare `pesatrack` Worker.
- Subsequent deployment, URL discovery, health/privacy checks and provider-readiness checks passed, so future chats should treat those credentials as configured unless a new failure proves otherwise.
- Do not ask the user to paste any of these secret values into chat.
- ChatGPT did not have a callable Cloudflare connector in this conversation; Cloudflare state was verified indirectly and safely through the GitHub deployment workflow and public health endpoint.

The pasted static-files error is therefore historical and resolved. Do not reintroduce a second Worker project or a separate Cloudflare app unless the current verified `pesatrack` Worker actually fails.

### Entity enrichment activation verified on 2026-09-29

- The active Cloudflare Worker is the existing project named `pesatrack`.
- Production Worker base URL: `https://pesatrack.jameswaweru399.workers.dev`.
- Production resolve endpoint: `https://pesatrack.jameswaweru399.workers.dev/v1/entity/resolve`.
- Cloudflare deployment credentials in GitHub are working: deployment, Worker URL discovery and health/privacy checks all passed.
- `LOGO_DEV_SECRET_KEY` is visible to the deployed `pesatrack` Worker; health returned `providerConfigured: true`.
- GitHub's workflow token cannot write Actions repository variables (`HTTP 403 Resource not accessible by integration`), so activation no longer depends on `gh variable set`.
- Android CI embeds the verified public Worker endpoint directly as `PESATRACK_ENTITY_ENRICHMENT_URL` for CI/release builds.
- The Worker deploy workflow health-checks Cloudflare and then triggers a fresh Android CI build so the rolling release APK is rebuilt against the active endpoint.
- The provider key remains only in Cloudflare and is never embedded in Android.

