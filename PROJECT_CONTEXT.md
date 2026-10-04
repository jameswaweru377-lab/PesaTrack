# PesaTrack Project Context

Updated: 2026-09-28

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

Status on 2026-09-29: **local intelligence is merged; privacy-safe remote enrichment plumbing is implemented but requires deployment/configuration before online lookup is active**.

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

Activation still required:

1. Obtain/configure a Logo.dev secret key outside the repository.
2. Deploy `worker/entity-enrichment` to Cloudflare Workers.
3. Set the Worker secret with `npx wrangler secret put LOGO_DEV_SECRET_KEY`.
4. Build PesaTrack with:
   `PESATRACK_ENTITY_ENRICHMENT_URL=https://<worker-host>/v1/entity/resolve`
5. Verify real Kenyan descriptors on-device before expanding the automatic allow-list.

Never commit a provider secret to GitHub, put it in Gradle source, or embed it in the APK.

Deployment automation added after PR #69:

- `.github/workflows/entity-enrichment-deploy.yml` provides a manual, repeatable Cloudflare Worker deployment path.
- It requires repository secrets `CLOUDFLARE_API_TOKEN` and `CLOUDFLARE_ACCOUNT_ID`.
- `LOGO_DEV_SECRET_KEY` remains a Cloudflare Worker secret; it is not copied into the APK or repository.
- The Worker now exposes `GET /v1/entity/health` so deployment can verify the correct privacy-mode service is live.
- Android CI and Worker deployment both run `worker/entity-enrichment/test.mjs`, which verifies health, rejects phone-bearing descriptors and likely personal names, and confirms a safe commercial descriptor reaches the provider boundary.
- After deployment, set repository variable `PESATRACK_ENTITY_ENRICHMENT_URL=https://<worker-host>/v1/entity/resolve`. The next successful `main` build embeds only that public endpoint in the rolling APK.


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



## Provider-agnostic bank and debt intelligence — PR #78

Status on 2026-09-29: **implemented and Android CI verified on the code head before this documentation commit**.

This phase fixes the architecture that caused unfamiliar bank/lender messages to disappear or require provider-specific code.

Implemented behavior:

- Unknown financial institutions are treated as discovered entities instead of requiring a dedicated parser file.
- Production transaction logic does not contain Equity-, Co-op-, Family Bank-, Branch-, or Tala-specific branches. Those names are used only in regression fixtures.
- Explicit unknown labels containing financial product language such as `Loan` or `Overdraft` can be represented as liabilities without adding them to the static provider registry.
- Ordinary organization names containing words such as `Credit` are not automatically treated as debt.
- Generic bank debit language such as an account being debited for a transfer/payment is interpreted from transaction grammar and anchored to the message source as the user's account.
- Generic named-transfer language is separated from true account-to-account transfers:
  - explicit account/wallet/bank destinations remain transfers;
  - a transfer to an ordinary named person/entity becomes a payment instead of an unresolved account transfer.
- Unknown bank/wallet message sources can become locally discovered owned accounts when the transaction wording provides strong ownership evidence. Balance-only evidence remains conservative and does not automatically claim ownership.
- Generic lender repayment wording can create debt repayment events without a lender-specific parser.
- A debt repayment can carry an explicitly observed outstanding balance into the debt ledger.
- Liability balance observations are bridged into `DebtStore` as observed outstanding balances. This lets an existing M-Shwari/other loan appear even if PesaTrack never saw the original drawdown SMS.
- Existing M-Shwari Loan provider knowledge remains available, but the debt-balance bridge is generic.
- The existing mirror-transaction layer is protected by regression coverage so a bank-side observation and an M-Pesa-side observation of the same movement can reconcile instead of becoming duplicate income/spending.
- A privacy-safe ingestion diagnostic trail was added. It records only:
  - sender/title metadata,
  - source package/kind,
  - timestamp,
  - outcome code,
  - inserted transaction/event counts,
  - pending-review count.
- The diagnostic trail does **not** persist the SMS/notification body, amount, account number, phone number, or transaction code.

Verification history during PR #78:

- Early CI runs exposed two over-broad ownership expectations; the implementation was tightened rather than weakening tests.
- Balance-only unknown-provider inference was restored to conservative behavior.
- Explicit account-transfer grammar evidence was preserved so bank-to-wallet transfers remain transfers while named recipients become payments.
- Nullable diagnostic metadata handling was corrected.
- Final code head `b228573c5c926730e19bf4e4d8160550985bfb4f` passed Android CI run #304, including Worker validation, Android unit tests and debug APK build.

Real-device validation still required after merge:

1. Re-import or ingest actual Equity SMS/notifications and confirm outgoing debits now appear in Activity.
2. Verify Equity -> M-Pesa / M-Pesa -> Equity mirrored messages reconcile as one movement and are not income/spending.
3. Re-import M-Shwari history and confirm an observed current loan balance appears in debt tracking even when the original drawdown message is absent.
4. Test a real Branch message if present; do not assume Branch activity is debt unless message semantics prove borrowing/repayment.
5. When a new lender/bank appears later, first check the privacy-safe ingestion diagnostic outcome before adding any provider-specific code.
6. Add new grammar fixtures only when the financial wording itself is genuinely new; do not add company-specific parsers for ordinary bank/loan semantics.

Architecture rule going forward:

**PesaTrack should learn financial language and relationships, not memorize provider names.** Static provider profiles are reserved for products with genuinely special mechanics (for example Fuliza transaction-shortfall financing) or stable identity metadata, not ordinary debit/credit/loan parsing.


## Activity financial-source feed and captured-message visibility — PR #79

Status on 2026-09-29: **implemented and Android CI verified before this documentation commit**.

This phase was added after real-device Activity testing showed a bank transfer row labelled `Equity Bulk`, which exposed the SMS delivery-channel sender name rather than a useful financial identity. It also addresses the need to prove that bank/lender messages are actually reaching PesaTrack independently from the reconciled ledger.

### Source identity

- Financial sender suffixes such as `Bulk`, `Bulk SMS`, `Alerts`, `SMS`, and `Notifications` are treated as delivery-channel metadata rather than institution identity.
- Unknown sources remain provider-agnostic.
- If message language itself is bank-like (for example account/debit/credit/available-balance wording), an unknown message source can be represented as a discovered Bank account.
- Example regression: `Equity Bulk` + bank semantics becomes display identity `Equity Bank` without adding an Equity-specific transaction parser.
- Unknown wallet names keep meaningful identity words such as `Wallet`; they are not forced into Bank unless the message language provides bank evidence.
- Existing provider-specific knowledge remains only for stable identity/special mechanics such as M-Pesa, M-Shwari, and Fuliza.

### Activity source navigation

The expanded month in Activity now includes a **Scanned financial sources** row below the existing All / Expenses / Income / Transfers filters.

- Sources are generated dynamically from captured financial-message diagnostics.
- Typical chips can therefore become:
  - All sources
  - M-Pesa
  - Equity Bank
  - M-Shwari
  - Branch
  - Tala
  - Co-op / another future bank
- No company list is hardcoded into the Activity navigation.
- Each source chip includes the number of captured financial messages for that source in the selected month.

Selecting a source changes the visible section from the reconciled transaction ledger to the **captured financial-message feed** for that source.

This distinction is intentional:

- The normal Activity ledger should still reconcile duplicate observations into one financial movement.
- The captured-message feed is pre-reconciliation evidence.
- Therefore a bank -> M-Pesa movement can show:
  - one Equity/other-bank captured message;
  - one M-Pesa captured message;
  while the money ledger still keeps one transfer.

This is the correct place to verify whether messages from a bank/lender are arriving.

### Captured event labels

The diagnostic feed stores a short semantic label, not raw message text. Current labels include:

- Bank transfer
- M-Pesa transfer
- Transfer
- Bank payment
- Payment
- Bank credit
- Money received
- Loan
- Loan repayment
- Loan repaid
- Loan balance
- Balance update
- Cash withdrawal
- Cash deposit
- Reversal
- Needs review
- Not understood

Loan labels come from transaction semantics, not from a hardcoded lender name. A future lender can therefore appear as Loan / Loan repayment when the wording proves that meaning.

### Diagnostic privacy and retention

The local diagnostic record now keeps up to 500 financial-message observations so a real monthly import with many transactions remains inspectable.

It stores locally:

- sender/source display identity,
- source type,
- semantic event label,
- source package/kind,
- timestamp,
- ingestion outcome and counts.

It still does **not** store the raw SMS/notification body. Raw financial messages must not be uploaded by this feature.

### Capture-boundary audit

The code was checked to make sure unknown institutions are not filtered before interpretation:

- `HistoricalSmsImporter` queries all SMS in the selected time window and sends each non-empty message to `FinancialMessageIngestionCoordinator` before legacy M-Pesa/configured-rule fallback logic.
- `MpesaNotificationListenerService` sends each notification body/title through `FinancialMessageIngestionCoordinator` before legacy evidence/configured-rule handling.
- Therefore Equity, Branch, Tala, Co-op, Family Bank, or a future source can reach the generic interpreter without being pre-registered.
- If a source does not appear in the new feed after an import/live notification, investigate device permission/delivery first.
- If it appears as `Not understood`, add a generic financial-language fixture for the new wording rather than a company-specific parser.

### Verification before documentation update

- PR #79 initial CI exposed two source-display expectation mismatches.
- The code was corrected so meaningful wallet identity is preserved and uppercase bank sender IDs are humanized.
- Final code head `230300737af0c4749a8fdd9b9cb4538ce850405b` passed Android CI run #309:
  - entity-enrichment Worker validation: passed;
  - 185 Android unit tests: passed;
  - debug APK assembly: passed.

Real-device check after merge:

1. Install the newest rolling APK.
2. Import the desired history window again so the source feed is populated from those messages.
3. Open Activity -> selected month -> Scanned financial sources.
4. Confirm Equity Bank, M-Pesa, M-Shwari and any Branch/Tala/other sender actually seen on the device appear as dynamic source chips.
5. Tap each source and inspect its captured-message labels.
6. For a single bank <-> M-Pesa movement, confirm both source observations can be seen in the source feed while only one transfer affects the ledger.
7. Report any row labelled `Not understood` with a sanitized wording example so the generic grammar can be extended.


## Financial Intelligence V2 — Phase 1 foundation — PR #80

Status on 2026-09-29: **implemented and Android CI verified on code head `531998a24f4691bb13e5a2f2bfb1ba276d06ce0c` before this documentation commit.**

This phase starts the replacement of brittle financial-message recognition with a provider-agnostic language pipeline. It deliberately does **not** add an ML model yet and does **not** add Equity/KCB/Branch/Tala-specific parser branches.

### Permanent architecture rule

Provider identity is context, not transaction meaning.

Production logic must not encode rules such as:

- `if sender == Equity -> credit`
- `if sender == Branch -> loan`
- `if sender == KCB -> transfer`

The same financial language must work for an unknown future bank/lender. Provider names may appear in sanitized regression fixtures to prove that production logic remains provider-agnostic.

### Phase 1A: Kenyan money normalization

`FinancialMessageNormalizer` now canonicalizes common Kenyan currency typography before semantic parsing.

Accepted forms include:

- `KES` / `Kes`
- `Ksh` / `KSh`
- `Kshs` / `Kshs.`
- `Sh` / `Shs`
- `Kenya Shilling` / `Kenya Shillings`
- currency written before or after the numeric amount

These forms normalize to a canonical `KES <amount>` representation so downstream interpretation does not need provider-specific currency spelling rules.

Examples:

- `Kshs. 2,000` -> `KES 2,000`
- `Sh73.00` -> `KES 73.00`
- `Kenya Shillings 20,000.00` -> `KES 20,000.00`
- `2,500 Kenya Shillings` -> `KES 2,500`

### Phase 1B: provider-agnostic financial relevance gate

A new local `FinancialMessageRelevanceGate` runs before full interpretation.

It reasons from language/evidence rather than sender brands. Signals currently include:

- Kenyan currency + numeric amount
- credit/debit/payment language
- contextual transfer/movement language
- account/balance/reference context
- debt/loan context
- investment/share-order context
- financial confirmation language
- actual utility/airtime/data purchase context

Noise signals include:

- OTP/password/security messages
- promotions/reward scams
- loan/credit solicitations such as `you qualify... apply now`
- airtime/data/bundle informational balances/expiry messages when no actual purchase/payment occurred

Ambiguous words such as `received`, `sent`, `transfer`, or `moved` are not enough by themselves. They require financial context such as currency, an account/wallet, debt/investment meaning, a confirmation phrase, or a real purchase context. This prevents unrelated messages such as `David sent you a photo` or `You received 500 reward points` from entering the financial feed.

### Phase 1C: sanitized regression corpus

A permanent `FinancialIntelligenceV2FoundationCorpus` was added under Android unit tests.

The corpus is sanitized and includes representative language for:

- Equity-style long-form `Kenya Shillings` transaction/reference wording
- M-Shwari transfers
- M-Shwari loan-balance wording
- locked-savings movement
- Branch-style loan disbursement
- Branch-style `transfer of funds has been processed` confirmation
- unknown/future bank credit and debit messages
- merchant/internet payment confirmation
- investment order submission and executed share purchase
- irrelevant Safaricom airtime balance
- bundle promotion
- loan marketing solicitation
- Google verification code
- Discord/login-style message
- reward points
- ordinary personal-message noise

Provider names in this file are **test fixtures only**, not production routing rules.

Tests verify:

1. all expected Kenyan currency variants normalize to canonical KES;
2. relevant corpus examples pass the relevance gate;
3. irrelevant examples are rejected;
4. the pre-existing financial fixture corpus remains relevant;
5. the pre-existing non-financial fixture corpus remains rejected;
6. an unknown provider can pass purely because its financial language is meaningful;
7. a provider/bank-looking sender name alone cannot make an ordinary message financial.

### Ingestion/privacy effect

Historical SMS import and live notification ingestion may still inspect message text locally in memory so PesaTrack can decide whether it is financial.

However, the local captured-message diagnostic store now records an unrecognized message only when the relevance gate considers it a financial candidate. A message is no longer retained in financial diagnostics merely because it contains a currency-looking amount.

The existing privacy boundary remains:

- raw SMS body is not stored in the diagnostic feed;
- raw SMS body is not uploaded by this feature;
- irrelevant messages should stop at the relevance gate.

### Verification

Android CI run #317 passed on code head `531998a24f4691bb13e5a2f2bfb1ba276d06ce0c`:

- entity-enrichment Worker validation: passed;
- Android unit tests: passed;
- debug APK assembly: passed.

### Deliberately not solved in Phase 1

A message can now be correctly recognized as **financially relevant** without yet being fully understood as a ledger event.

For example:

- `Your transfer of funds has been processed` can pass the relevance gate but still needs Phase 2 confirmation/event correlation.
- a loan-balance sentence without a currency marker can pass relevance but may still need broader semantic amount extraction.
- Equity reference + M-Pesa reference correlation is not added in this phase.
- investment order lifecycle interpretation is not added in this phase.
- no TensorFlow Lite / SLM model is added in this phase.

Do not treat a Phase 1 relevance success as final ledger interpretation.

### Recommended next implementation — Financial Intelligence V2 Phase 2

Build the canonical semantic fact layer on top of the new foundation:

1. define general intents/facts such as PAYMENT, RECEIPT, TRANSFER, LOAN_DISBURSEMENT, LOAN_REPAYMENT, LOAN_BALANCE, BALANCE_UPDATE, REVERSAL, REFUND, FEE, INVESTMENT_ORDER, INVESTMENT_EXECUTED, and CONFIRMATION_ONLY;
2. broaden amount/reference/account extraction independently from provider names;
3. classify message status (new financial event vs confirmation/reminder/information);
4. keep uncertain meaning in Needs Review instead of fabricating ledger effects;
5. add cross-message correlation only after semantic facts are stable, with exact shared references as the strongest evidence;
6. continue growing the sanitized real-world corpus as new wording is observed;
7. evaluate a small offline ML classifier/token extractor only after deterministic semantic failures are measured against the corpus.


## Financial Intelligence V2 — Phase 2 semantic layer — PR #81

Status on 2026-09-30: **implemented and Android CI verified on code head `fa350bd7bbfeef8b291f6880feedf00fe2f982bf` before this documentation commit.**

Phase 2 adds a provider-agnostic semantic layer between the Phase 1 relevance gate and the deterministic ledger/accounting engine.

### Core semantic model

A new `FinancialSemanticAnalyzer` classifies financially relevant messages into general meanings instead of bank/lender-specific handlers.

Current semantic intents include:

- PAYMENT
- RECEIPT
- TRANSFER
- LOAN_DISBURSEMENT
- LOAN_REPAYMENT
- LOAN_BALANCE
- BALANCE_UPDATE
- REVERSAL
- REFUND
- FEE
- INVESTMENT_ORDER
- INVESTMENT_EXECUTED
- CONFIRMATION_ONLY
- UNKNOWN

Semantic roles distinguish message lifecycle/status:

- NEW_EVENT
- PENDING_ACTION
- CONFIRMATION
- REMINDER
- INFORMATION
- NEEDS_REVIEW

Semantic facts also include:

- amount facts with roles such as PRIMARY, BALANCE, OUTSTANDING, FEE and INVESTMENT_VALUE;
- labeled external references;
- account-role facts;
- interpretation confidence;
- rule IDs explaining why the message received that meaning.

The semantic layer itself does **not** mutate the ledger. The deterministic interpreter/accounting system remains authoritative for persistence.

### Provider-agnostic reference extraction

Reference extraction is generic and supports multiple labeled references in the same message without hardcoding a provider.

For example, a message containing both:

- `Bank Ref ABC12345`
- `M-Pesa Ref UFT12345`

can expose both references as semantic facts.

This prepares Phase 3 for exact cross-message correlation without requiring rules such as `if sender == Equity`.

### Confirmation-only behavior

Messages such as:

`Your transfer of funds has been processed.`

are now semantically understood as `CONFIRMATION_ONLY` with role `CONFIRMATION`.

They do **not** create a second transfer or other ledger event merely because they contain the word `transfer`.

This distinction is important for lenders and banks that send both an event message and a later status/processing message.

### Loan semantics

Generic lender language is expanded without provider-specific production branches.

Implemented behavior includes:

- sender-scoped loan disbursement for an unknown lender;
- sender-scoped loan balance, allowing a message such as `Your total loan balance is KES ...` to become an observed liability using the message source as context;
- existing M-Shwari provider identity still resolves to the known M-Shwari Loan liability;
- unknown future lenders can create a generic liability when message semantics prove a loan event;
- loan disbursement amounts are treated as gross borrowed amounts unless explicit fee/net-proceeds evidence says otherwise.

The original loan drawdown still does not need to have been observed for a later explicit loan-balance message to establish the outstanding liability.

### Investment semantics

Phase 2 distinguishes:

- `INVESTMENT_ORDER`: e.g. buy/sell order submitted or pending;
- `INVESTMENT_EXECUTED`: e.g. shares actually bought/sold.

A submitted order is semantically understood but does not pretend that an investment purchase already completed.

Investment persistence/portfolio accounting is intentionally not implemented in this phase.

### Semantic understanding vs ledger recognition

`FinancialMessageInterpretation` now keeps two separate ideas:

- `recognized`: the deterministic system produced an actual balance/event that can participate in the ledger;
- `semanticallyUnderstood`: the semantic layer understands what the financial message means even when no ledger mutation is appropriate.

This prevents confirmation-only, pending-investment and informational messages from being incorrectly treated as unrecognized financial language.

### Activity diagnostics

The privacy-safe financial diagnostics now have an `UNDERSTOOD_NO_LEDGER_EFFECT` outcome.

Therefore financially meaningful messages that intentionally do not affect the ledger no longer need to appear simply as `Not understood`.

Examples:

- processed-transfer confirmation -> understood confirmation;
- investment order submitted -> understood pending action;
- loan-balance information -> understood information / balance observation when extractable.

The diagnostic store still does **not** persist raw SMS bodies.

### Canonical draft integration

Semantic assessment is carried into canonical financial drafts so future correlation/review layers can use semantic intent, role, references, amounts and confidence alongside deterministic records.

### Phase 2 regression corpus

A dedicated semantic corpus/tests cover:

- M-Shwari loan balance without an observed original drawdown;
- unknown future lender loan disbursement;
- confirmation-only transfer processing;
- investment order submission;
- investment execution;
- multiple provider-neutral references in one message;
- provider-agnostic semantic intent/role expectations.

Older financial fixture/evidence tests remain in force and are not replaced by semantic tests.

### Verification

The first Phase 2 CI runs intentionally exposed semantic/regression mismatches while the branch was being consolidated.

The final code head `fa350bd7bbfeef8b291f6880feedf00fe2f982bf` passed Android CI run #326:

- entity-enrichment Worker validation: passed;
- Android compilation: passed;
- Android unit tests: passed;
- debug APK assembly: passed.

### Deliberately not solved in Phase 2

Phase 2 understands messages better, but it does not yet solve full cross-message event correlation.

Still pending for the next phase:

1. exact shared-reference correlation across providers/sources;
2. bank-side + M-Pesa-side observation linking into one canonical movement;
3. confirmation message linking to the specific original event;
4. amount/time/account fallback correlation when no shared reference exists;
5. investment persistence/portfolio accounting;
6. broader real-world corpus expansion from sanitized device examples;
7. offline ML only if measured deterministic semantic gaps justify it.

### Recommended next implementation — Financial Intelligence V2 Phase 3

Build a canonical observation-correlation layer.

Priority order:

1. exact shared external reference match;
2. compatible source/destination account identities;
3. same amount and direction;
4. tight time window;
5. semantic event compatibility;
6. preserve both source observations for audit, but allow only one canonical financial movement to affect the ledger;
7. link confirmation-only messages to an existing event instead of creating a new one;
8. send ambiguous matches to review rather than auto-linking.

Permanent rule remains:

**PesaTrack learns financial language and relationships, not provider-specific parsers.**


## Financial Intelligence V2 — Phase 3 cross-message correlation — PR #82

Status on 2026-09-30: **implemented and Android CI verified on code head `833061a45df0326eeea81bab92700e52946f6cc8` before this documentation commit.**

Phase 3 links multiple financial-message observations that describe one real-world money movement while preserving the source observations for audit.

Primary use case:

- bank message describes a bank -> wallet transfer;
- wallet message describes the corresponding receipt;
- the two messages may contain different local references but share one external reference;
- Activity should keep the two source observations visible;
- only one canonical ledger movement should affect totals.

### Correlation evidence hierarchy

`MirrorTransactionMatcher` now returns an explicit match confidence and reason instead of only a boolean.

Current confidence levels:

- HIGH: safe for automatic linking;
- MEDIUM: possible match, do not auto-link;
- NONE: unrelated / insufficient evidence.

Current correlation reasons include:

- SAME_TRANSACTION_CODE;
- EXACT_SHARED_REFERENCE;
- COMPLEMENTARY_ACCOUNTS;
- SHARED_IDENTITY;
- COMPLEMENTARY_DIRECTION;
- CODED_CODELESS_VERY_CLOSE;
- ULTRA_CLOSE_DISTINCT_SOURCE.

The matcher keeps amount agreement as a hard prerequisite for normal mirror correlation. A conflicting amount blocks the link even if a reference happens to match.

Reversals are excluded from ordinary mirror correlation.

### Reference-first correlation

A new local table, `transaction_external_references`, stores normalized external references associated with canonical transactions.

Database version is now **19**.

The migration backfills existing non-empty `transactions.transaction_code` values into the external-reference index so older canonical transaction codes are immediately searchable.

New interpreted messages can contribute multiple references extracted by the Phase 2 semantic layer.

Example:

- bank-side observation:
  - local bank ref `EQA...`
  - wallet/M-Pesa ref `UFT...`
- M-Pesa-side observation:
  - transaction code `UFT...`

If the shared `UFT...` reference and amount agree, the observations can link even when the timestamps are outside the normal short heuristic window.

Production correlation logic does **not** ask which bank supplied the reference.

### Ambiguity protection

Automatic linking is intentionally conservative.

Rules include:

- exact shared reference + same amount -> high confidence;
- multiple transactions matching the same external reference -> do not auto-link;
- shared account-path evidence can produce a high-confidence link;
- weak identity/direction fallbacks require distinct observation sources;
- one coded + one codeless observation can auto-link only inside a very tight window;
- two fully codeless observations that are merely close in time are MEDIUM confidence and are not auto-linked;
- user-classified and excluded transactions are protected from automatic correlation rewrites.

An important Android detail discovered by regression testing: messages from two different financial institutions can both arrive through the same SMS application. Therefore Android package/source kind is not treated as equivalent to financial-provider identity.

### Canonical transaction-code stability

When a correlated observation is merged into an existing canonical transaction:

- an empty canonical transaction code can be upgraded from a later coded observation;
- an already populated canonical transaction code is kept stable rather than being overwritten by a different provider-side code;
- all discovered codes/references remain associated through the external-reference table.

This avoids making the canonical row depend on which provider observation arrived last.

### Existing mirror reconciliation

Historical mirror reconciliation now reads external references as evidence and preserves references from the removed duplicate on the surviving canonical transaction.

The one-time mirror reconciliation key was bumped to `mirror_transaction_reconciliation_v3` so the stronger correlation pass can run on installations upgraded from the earlier matcher.

Historical messages that contained additional references which were never previously stored still need a re-import to expose those references to the new semantic/correlation pipeline.

### Financial-event linkage

Interpreted transfer/cash financial-event records now fall back to the external-reference index when the canonical transaction uses a different transaction code.

This keeps the financial-understanding/event layer connected to the surviving canonical ledger row even when two providers describe the same movement using different references.

### Source-feed behavior

The privacy-safe financial diagnostic feed now supports `LINKED_OBSERVATION`.

When a second financial message is understood and linked to an existing canonical movement without inserting another transaction, Activity shows its source observation as:

`Linked`

instead of treating it as another captured ledger movement or as not understood.

The raw SMS body is **not** stored in the correlation-reference table or diagnostic feed.

### Phase 3 regression coverage

Regression tests now explicitly protect:

- a bank-side message and wallet-side message sharing one semantic external reference;
- different bank/wallet local codes linked through the common external reference;
- amount conflict blocking a shared-reference merge;
- provider-agnostic coded/codeless bank-wallet mirror behavior;
- two genuine M-Pesa transactions with different codes staying separate;
- account-path correlation;
- fully codeless time-only similarity remaining review-only;
- exact same transaction code remaining linkable.

The Phase 3 development CI exposed and corrected:

1. a diagnostic JSON nullable-field compile mistake;
2. diagnostic reference scope missing inside the helper;
3. same-SMS-app bank/wallet observations being incorrectly rejected;
4. weak shared-counterparty evidence accidentally allowing two real coded M-Pesa transactions to merge.

Final code head `833061a45df0326eeea81bab92700e52946f6cc8` passed Android CI run #335:

- entity-enrichment Worker validation: passed;
- Android compilation: passed;
- Android unit tests: passed;
- debug APK assembly: passed.

### Deliberately not solved in Phase 3

Phase 3 creates the core correlation engine, but several larger product areas remain separate:

- investment holdings/portfolio persistence from broker execution messages;
- a dedicated UI for manually resolving MEDIUM-confidence correlation candidates;
- unlimited long-term source-observation storage (the diagnostic source feed still has its existing bounded retention);
- every possible real Kenyan message wording;
- offline ML/SLM inference.

Do not weaken the correlation thresholds merely to remove duplicate-looking rows. Ambiguous matches should remain reviewable rather than silently combining separate real transactions.

### Recommended next work after real-device validation

Before starting another large intelligence phase, install the latest rolling APK and re-import a controlled history window.

Validate specifically:

1. bank -> M-Pesa transaction where the bank SMS contains the M-Pesa reference;
2. matching M-Pesa receipt appears as a separate source observation but only one ledger movement affects totals;
3. same-amount genuine separate M-Pesa transactions remain separate;
4. M-Shwari loan balance continues to populate debt correctly;
5. Branch-style disbursement + processing confirmation does not become two loans;
6. any remaining `Not understood` messages are captured as sanitized language fixtures before changing production grammar.

Permanent architecture rule remains:

**financial language defines meaning; references/accounts/amount/time define relationships; provider names do not define transaction logic.**


## Financial Intelligence V2 — Phase 4 event-chain reconciliation — PR #83

Status on 2026-09-30: **implemented and Android CI verified on code head `a5122fb01fd715a6148726a25f3505e029be8131` before this documentation commit.**

Phase 4 extends Phase 3 from observation deduplication to deterministic multi-event relationships.

Phase 3 answers:

`Are these two messages observations of the same movement?`

Phase 4 answers:

`Do these different financial events belong to one observed financial sequence?`

The first Phase 4 production scope is debt-repayment chains because PesaTrack already receives enough deterministic evidence to reason about them without provider-specific parsers.

### Primary event chain

The canonical chain is:

`incoming funds -> debt repayment -> resulting account balance`

A second supported relationship already present in the generic debt ledger remains:

`loan drawdown -> proceeds account -> repayment of another liability`

Example financial sequence:

- KES 14,381 enters an owned funding account;
- KES 1,381 is automatically used to repay a liability;
- the resulting owned-account balance is KES 13,000.

PesaTrack can now preserve those as separate observed financial facts while linking them as one deterministic event chain.

The repayment itself remains a **debt event**. It is not converted into income or spending.

### Provider-neutral funding account identity

The earlier repayment reconciler still contained an M-Pesa-specific runtime gate.

Phase 4 removes that assumption.

Debt events now persist both:

- `funding_account_key` — semantic financial entity identity;
- `funding_account_id` — confirmed local owned-account identity when available.

Repayment funding correlation now uses account relationships rather than provider names.

For an incoming canonical movement to fund a repayment:

- INCOME must arrive into the funding account; or
- an owned-account TRANSFER must have the funding account as its destination.

The correlator does not ask whether the account is M-Pesa, Equity, Co-op, KCB, M-Shwari, or a future institution.

If account ownership/identity is not known, Phase 4 does not invent the relationship.

### Resulting-balance reconciliation

A new provider-neutral `DebtRepaymentBalancePolicy` links a repayment event to a resulting `account_balance_observations` row.

Evidence priority:

1. explicit final funding-account balance carried by the repayment message;
2. otherwise a deterministic balance equation:
   `previous balance + incoming funds - repayment = resulting balance`;
3. the resulting balance observation must be unique inside the bounded time window.

If multiple balance observations satisfy the same candidate chain, PesaTrack refuses the automatic link.

The policy deliberately receives only amounts/timestamps/observation IDs. It has no provider-name logic.

### Available credit remains separate from cash

Phase 4 preserves the existing distinction between:

- debt outstanding;
- available credit / available loan limit;
- spendable asset-account balance.

For example, an available loan limit of KES 1,440 is not allowed to satisfy an expected M-Pesa/bank balance of KES 13,000.

Available credit remains stored on the liability/debt account, not as spendable cash.

### Out-of-order ingestion

The event chain can be completed when evidence arrives in different orders.

Reconciliation is triggered when:

- a debt repayment is recorded;
- a qualifying incoming transaction is inserted or correlated;
- a funding-account balance observation is recorded;
- an uncertain financial entity is later confirmed by the user as an owned account.

When ownership is confirmed, existing debt events using that semantic entity are backfilled with the confirmed local account ID and reconsidered.

### Historical reconciliation

Schema version is now **20**.

The v20 migration adds:

- `debt_events.funding_account_id`;
- `debt_events.resulting_balance_observation_id`;
- `debt_events.balance_link_confidence`;
- `debt_events.balance_link_reason`.

Existing debt events backfill `funding_account_id` from confirmed financial entities when possible.

A one-time `financial_event_chain_reconciliation_v1` pass reconsiders existing repayment events using the new account/balance evidence. Later incoming transactions, balances, and ownership confirmations continue to trigger targeted reconciliation.

### Ambiguity and safety rules

Phase 4 must remain conservative.

It does not:

- fabricate a missing incoming transaction;
- infer account ownership from provider name alone;
- treat a nearby outgoing transfer as repayment funding;
- choose between multiple matching balance observations;
- turn available credit into asset balance;
- convert a debt repayment into normal expense/income;
- weaken Phase 3 transaction-correlation safeguards.

Existing drawdown/repayment ambiguity protection remains in place.

### Regression coverage

The debt-linking regression suite now also covers:

- explicit final balance -> resulting balance observation;
- deterministic computed final balance -> resulting balance observation;
- ambiguous duplicate result balances -> no automatic link;
- available-credit amount -> cannot masquerade as resulting cash balance.

Code head `a5122fb01fd715a6148726a25f3505e029be8131` passed Android CI run #340:

- entity-enrichment Worker validation: passed;
- Android compilation: passed;
- Android unit tests: passed;
- debug APK assembly: passed.

### Deliberately outside this Phase 4 slice

This phase does not yet add:

- a visual multi-event chain explorer;
- manual UI for ambiguous chain candidates;
- investment holdings / execution settlement chains;
- ML/SLM inference;
- provider-specific debt rules for ordinary lenders/banks.

Those should only be added when measured real-device evidence requires them.

Permanent architecture rule remains:

**financial language defines meaning; observed accounts/references/amounts/balances/time define relationships; provider names do not define transaction logic.**


## Financial Intelligence V2 — Phase 5 manual relationship review — PR #84

Status on 2026-09-30: **implemented and Android CI verified on code head `3f08e7358f654913e7d106f0fe3959fad2ec1217` before this documentation commit.**

Phase 5 closes the intentional safety gap left by Phases 3 and 4:

- automatic correlation stays conservative;
- ambiguous event-chain relationships are not guessed;
- the user can now resolve those relationships explicitly in Activity;
- the user’s decision is persisted and automatic reconciliation cannot overwrite it.

### Activity Links queue

Activity now has a third global review queue:

- Review — unresolved transaction classification;
- Duplicates — possible duplicate transaction observations;
- Links — ambiguous multi-event relationships.

The new Links queue currently covers debt-repayment event chains because Phase 4 already has deterministic provider-neutral evidence for those relationships.

Two review types are supported:

1. **Repayment funding**
   - question: which incoming transaction funded this debt repayment?
   - candidates must reach the already-confirmed owned funding account;
   - candidates must be inside the same bounded ten-minute provider-neutral review window used by Phase 4;
   - the UI shows amount, time and why each candidate is plausible.

2. **Repayment resulting balance**
   - question: which observed account balance represents the balance after this repayment?
   - a deterministic target balance must already exist from either:
     - an explicit final balance in the financial message; or
     - the Phase 4 balance equation using prior balance + incoming funds - repayment;
   - only observations matching that deterministic target inside the bounded balance window are offered.

The user can select a candidate or explicitly choose that none of the offered candidates belong to the relationship.

### Durable manual decisions

Schema version is now **21**.

The v21 migration adds two columns to `debt_events`:

- `funding_link_user_decision`;
- `balance_link_user_decision`.

Manual relationship confidence adds:

- `USER_CONFIRMED`.

When the user confirms a funding transaction:

- `linked_transaction_id` is set;
- `funding_link_confidence = USER_CONFIRMED`;
- a human-readable reason states that the user confirmed the link;
- only downstream resulting-balance reconciliation is rerun.

When the user confirms a resulting balance:

- `resulting_balance_observation_id` is set;
- `balance_link_confidence = USER_CONFIRMED`;
- the reason states that the user confirmed the balance relationship.

When the user selects **none of these**:

- the relevant user-decision column is persisted as a keep-unlinked decision;
- automatic reconciliation no longer retries or silently replaces that choice.

### Revalidation before writes

The UI does not pass arbitrary record IDs directly into the debt ledger.

Before persisting a manual link, `RelationshipReviewStore` rebuilds the current pending review and verifies that the selected transaction/balance observation is still in the provider-neutral candidate set.

If the candidate is no longer valid, the write is rejected.

### Automatic reconciliation protection

Phase 4 repayment funding and resulting-balance reconciliation now require the corresponding manual-decision field to be null.

Therefore:

- automatic HIGH/MEDIUM relationships continue to work where no user decision exists;
- a confirmed manual relationship remains stable;
- a manual keep-unlinked decision remains stable;
- later SMS imports, balance observations, or account confirmations cannot overwrite the user’s decision.

### Accounting boundary

Phase 5 changes **relationships only**.

It does not:

- turn a debt repayment into income or expense;
- change the repayment amount;
- change the debt account identity;
- change the transaction category;
- fabricate a missing transaction or balance;
- infer ownership from provider names.

A repayment remains a debt event regardless of how its funding/balance relationship is resolved.

### Provider-neutral candidate generation

The Links queue uses:

- confirmed funding-account identity;
- transaction direction into that account;
- amount;
- bounded time;
- deterministic balance equations;
- observed account balances.

It does not contain provider-name branches for M-Pesa, Equity, KCB, Co-op, Branch, M-Shwari or future institutions.

### UI behavior

Each Links review card shows:

- the debt account;
- repayment amount;
- funding account;
- repayment timestamp;
- each plausible candidate;
- why the candidate is being offered;
- an explicit button to use that candidate;
- an explicit “none of these” action.

The card also tells the user that the decision is remembered and will not be overwritten by automatic reconciliation.

### Verification

The first Phase 5 CI run exposed a schema-migration placement error: the v21 `oldVersion` guard had accidentally been inserted into `onCreate`.

That exact compiler failure was corrected by keeping new-install columns in `createDebtTables` and moving the v21 migration guard into `onUpgrade`.

Code head `3f08e7358f654913e7d106f0fe3959fad2ec1217` then passed Android CI run #344:

- entity-enrichment Worker validation: passed;
- Android compilation: passed;
- Android unit tests: passed;
- debug APK assembly: passed.

### Deliberately outside Phase 5

Phase 5 does not yet add:

- manual editing of arbitrary financial-event semantics;
- investment settlement/holding chains;
- long-term unbounded raw source-message retention;
- ML/SLM inference;
- provider-specific relationship rules.

The existing Duplicates review remains separate because “two observations of one movement” and “two different events in one chain” are different decisions.

### Recommended next work

After Phase 5, the most useful next engineering step should be driven by real-device evidence rather than another broad parser expansion.

The likely candidates are:

1. investment execution -> funding -> holding/position settlement chains;
2. a richer event-chain detail view for already-resolved relationships;
3. corpus-driven semantic expansion for any genuine remaining Not understood messages;
4. only then evaluate offline ML if deterministic language gaps remain measurable.

Permanent architecture rule remains:

**automatic intelligence may propose relationships; ambiguous financial truth requires explicit evidence or an explicit user decision.**


## Financial Intelligence V2 — real-device correctness repair — PR #85

Status on 2026-09-30: **implemented and Android CI verified on code head `24f1ebe32f38ddf46215334d16e7c13d136c5b53` before this documentation commit.**

This corrective slice was triggered by real-device evidence after Phases 1–5:

- Equity-originated outgoing transfers were missing from the ledger;
- an existing M-Shwari loan balance could still remain at zero;
- lender cash disbursement can differ from the true debt obligation when fees/taxes/interest are not fully stated in the SMS.

### Equity-style destination-credit wording

Historical SMS import was already scanning all SMS and routing every nonblank message through the generic Financial Message Ingestion Coordinator.

Therefore the missing Equity transactions were not caused by an Equity sender allowlist or by historical SMS pre-filtering.

The actual parser gap was provider-neutral wording such as:

`Your transaction of KES 20,000 has been credited to SAMPLE PERSON.`

The generic parser previously understood:

- account debited -> beneficiary;
- sent/paid -> beneficiary;
- credited/received <- sender;

but did not treat destination-credit wording as an outgoing movement.

Phase #85 adds a new generic semantic action:

`TRANSACTION_CREDITED_TO`

It is resolved through the same existing `resolveTransferTo` path:

- destination looks like another financial account -> TRANSFER;
- destination looks like a person/entity -> PAYMENT.

No Equity-specific production branch was added.

The regression fixture deliberately uses an Equity-style sender only to prove that the provider-neutral grammar works with the real wording family.

### M-Shwari loan balance without a currency marker

The Phase 1 relevance gate could already identify explicit debt language such as:

`Your total loan balance is 1,075.00.`

However, `FinancialCandidateExtractor` still returned immediately when no canonical KES currency marker was present.

That meant a message could be semantically understood as financial/debt language while never producing the balance fact needed by `FinancialUnderstandingStore.recordBalance` and `DebtStore.recordObservedOutstanding`.

The unconditional currency-hint early return has now been removed.

Safety remains at the Phase 1 relevance gate. The generic action regexes still require their own financial structure, while the existing balance grammar can now safely extract an explicit debt balance from a relevant message even when the sender omitted `KES/Ksh/Kshs`.

Regression coverage verifies:

- sender `M-Shwari`;
- body `Your total loan balance is 1,075.00.`;
- resulting liability provider key `NCBA_MSHWARI_LOAN`;
- observed outstanding amount KES 1,075.00.

### Lender payout is not automatically loan principal

A lender message such as:

`Your loan of KES 464 has been disbursed to your Mobile Wallet account.`

does not provide enough universal evidence to assume that KES 464 is the full principal/outstanding obligation. Some lending products can withhold fees/taxes or otherwise present cash proceeds separately from the amount ultimately owed.

Generic sender-scoped loan disbursements are therefore now marked:

`amountIsNetProceeds = true`

unless independent principal/outstanding evidence is present through richer debt grammar.

The debt ledger already respects this flag:

- the drawdown/proceeds event can be retained;
- the amount is not blindly added to outstanding debt as an estimated principal;
- a later explicit outstanding-balance observation can establish the true liability;
- a later repayment relationship can be reconciled without inventing a principal.

Do **not** add a Branch-specific reverse formula such as payout / (1 - fee rate). Published lender fee/rate schedules can change and may not describe the exact product/offer or whether a fee is withheld from cash proceeds.

### Repayment-to-lender follow-up

This PR deliberately does not convert every M-Pesa payment to a lender-looking counterparty into a debt repayment.

The correct provider-neutral next step is:

1. observe the outgoing M-Pesa payment;
2. compare its destination identity to an existing known liability/debt account;
3. if the liability relationship is unique and strongly evidenced, propose/link it as a debt repayment;
4. if ambiguous, send the relationship to the existing Activity Links review queue;
5. never require a provider-specific Branch/Tala/KCB/M-Shwari branch.

This is especially important for lenders that may not send a separate repayment-confirmation SMS.

### Regression coverage

`RealDeviceFinancialMessageRegressionTest` now protects:

- generic bank destination-credit wording -> outgoing payment;
- M-Shwari-style explicit loan balance without a currency marker -> liability balance;
- generic lender disbursement -> net proceeds, with no invented outstanding balance.

Code head `24f1ebe32f38ddf46215334d16e7c13d136c5b53` passed Android CI run #347:

- entity-enrichment Worker validation: passed;
- Android compilation: passed;
- Android unit tests: passed;
- debug APK assembly: passed.

### Real-device validation after merge

After installing the new rolling APK, re-import a controlled history window and verify:

1. Equity direct-to-person transfer appears in Activity and reduces the Equity account movement correctly;
2. Equity + M-Pesa shared-reference transfer still correlates to one canonical movement;
3. current M-Shwari outstanding loan balance appears from an explicit balance SMS even if no original drawdown exists;
4. lender cash disbursement does not invent an outstanding principal when only net proceeds are known;
5. M-Pesa repayment to a known lender remains available for the next provider-neutral liability-payment relationship slice.

Permanent architecture rule remains:

**financial language defines meaning; explicit debt evidence defines liability amounts; never infer provider-specific loan arithmetic from a brand name.**


## Financial Intelligence V2 — provider-neutral liability-payment bridge — PR #86

Status on 2026-09-30: **implemented and Android CI verified on code head `7877514d56f3ba50dbdd7b539d54b9d71de3bbf5` before this documentation commit.**

This slice continues the real-device correctness work after Phases 4–5 and PR #85.

The gap being closed:

- PesaTrack can already understand and persist explicit debt-repayment SMS messages;
- some lenders do not send a separate repayment-confirmation SMS;
- the actual evidence may only be the outgoing wallet/bank payment to the lender;
- that payment must not remain ordinary spending when it is strongly evidenced as repayment of an already-known liability.

### Permanent architecture rule

This feature is provider-neutral.

There is no production branch such as:

- `if counterparty == Branch`;
- `if counterparty == Tala`;
- `if counterparty == KCB`;
- `if counterparty == M-Shwari`.

The bridge operates only on:

- an already-existing debt/liability account;
- normalized counterparty identity;
- current observed outstanding balance;
- payment amount;
- existing transaction/account evidence;
- explicit user decisions when automatic evidence is insufficient.

### Liability-payment matching policy

A new `LiabilityPaymentLinkPolicy` compares the outgoing transaction counterparty with existing debt-account identities.

Identity comparison removes generic business/financial words such as:

- LOAN;
- BANK;
- FINANCE / FINANCIAL;
- CREDIT / LENDING;
- LIMITED / LTD / PLC;
- INTERNATIONAL / KENYA;
- PAYMENT / ACCOUNT / COMPANY / GROUP / SERVICES.

Those generic words can never create a provider match by themselves.

Examples of the intended general behavior:

- `Branch International Ltd` vs `Branch Loan` -> core identity `BRANCH`;
- a future lender `Example Finance Ltd` vs `Example Loan` -> core identity `EXAMPLE`;
- `Financial Services Kenya Ltd` does not match an unrelated debt merely because both contain generic financial words.

### Automatic linking threshold

Automatic repayment linking is deliberately strict.

A payment can auto-link only when:

1. exactly one known liability matches the counterparty identity;
2. the identity match is exact after generic-word normalization;
3. the liability has an **observed** outstanding balance;
4. the outgoing payment amount is positive;
5. the payment does not exceed that observed outstanding amount;
6. the transaction is not user-classified, excluded, protected by an explicit Smart Rule, or already in another relationship;
7. the user has not previously chosen to keep the payment unlinked.

When those conditions are met:

- a generic debt-repayment event is created/linked to the existing transaction;
- the transaction is reclassified from ordinary EXPENSE to TRANSFER;
- category becomes `DEBT_LOAN`;
- relation type becomes `DEBT_REPAYMENT`;
- the debt event keeps the real funding-account ID;
- the repayment amount reduces estimated outstanding only through the existing debt-ledger rules;
- later explicit observed debt balances still remain authoritative.

### Ambiguous matches use Activity -> Links

If identity evidence is plausible but not strong enough for automatic linking, the payment appears in the existing **Activity -> Links** queue.

Examples include:

- liability identity matches but no reliable observed outstanding balance exists;
- payment exceeds the currently observed outstanding balance;
- more than one known liability matches the same counterparty identity;
- identity overlap is strong but not exact.

The review card asks:

`Is this payment a debt repayment?`

It shows:

- original payment counterparty;
- amount;
- funding account;
- candidate liability/debt accounts;
- why each liability is being offered.

The user can either:

- choose the correct debt -> creates/links the repayment;
- choose `This was not a debt repayment` -> keeps it unlinked.

### Durable user decisions

Schema version is now **22**.

New table:

`liability_payment_decisions`

It stores:

- transaction ID;
- decision;
- selected debt-account ID when linked;
- decision timestamp.

A manual keep-unlinked decision prevents later automatic logic from reinterpreting the same payment as repayment.

A manual linked decision is revalidated against the current provider-neutral candidate set before the debt event is written.

### Accounting behavior

When a payment is confirmed as debt repayment:

- the original money movement is preserved;
- it no longer counts as ordinary spending;
- direction is TRANSFER;
- category is DEBT_LOAN;
- a repayment event is linked to the canonical transaction;
- debt outstanding uses the existing DebtStore observed/estimated balance protections.

The feature does not:

- fabricate a liability from a lender-looking name;
- create provider-specific loan formulas;
- infer loan principal from cash proceeds;
- overwrite a later explicit observed outstanding balance;
- override an excluded transaction;
- automatically override a user classification or explicit Smart Rule.

### Historical/re-import behavior

Newly ingested or re-imported payment messages run through the automatic liability bridge.

Historical transactions that already exist can still appear in Activity -> Links because the review queue derives candidates from current canonical transactions and current known liabilities.

Therefore a full database reset is not required simply to review older lender payments, although re-import is still useful when older SMS text contains financial facts/references that were never extracted by an earlier app build.

### Regression coverage

`LiabilityPaymentLinkPolicyTest` protects:

- exact institution identity + observed outstanding -> HIGH automatic match;
- exact identity without reliable outstanding -> MEDIUM review;
- payment above observed outstanding -> MEDIUM review;
- generic financial/business words do not create a match;
- ambiguous matching liabilities never auto-link.

Code head `7877514d56f3ba50dbdd7b539d54b9d71de3bbf5` passed Android CI run #350:

- entity-enrichment Worker validation: passed;
- Android compilation: passed;
- Android unit tests: passed;
- debug APK assembly: passed.

### Recommended real-device validation after merge

Use a controlled history window and verify:

1. a payment to a lender with a known observed outstanding balance becomes one debt repayment rather than ordinary spending;
2. a lender payment without strong enough debt evidence appears in Activity -> Links rather than being guessed;
3. choosing a debt in Links reclassifies the payment and reduces the debt ledger once;
4. choosing `This was not a debt repayment` is remembered;
5. re-importing the same SMS does not create a second debt repayment;
6. explicit later lender balance SMS remains authoritative;
7. unrelated payments with generic words such as bank/finance/payment never become debt repayments.

Permanent rule remains:

**financial language establishes the event; existing liability identity and explicit evidence establish debt relationships; uncertain debt relationships require the user.**


## Financial Intelligence V2 — investment settlement foundation — PR #87

Status on 2026-09-30: **implemented and Android CI verified on code head `d796d593ec115ac92c1ffc0c3209c706882d2c69` before this documentation commit.**

This slice extends the provider-agnostic semantic engine into investment settlement without turning broker names into parser logic.

### Problem being solved

Phase 2 could already distinguish:

- `INVESTMENT_ORDER`;
- `INVESTMENT_EXECUTED`.

However, those facts were diagnostic-only. PesaTrack did not yet persist an order/execution lifecycle, track observed position changes, or connect an executed trade to the wallet/bank transaction that funded it or received its proceeds.

The target language is generic financial language such as:

- `Your buy order of 10 SAMPLE shares ... has been submitted.`
- `You have successfully bought 3 EQA shares valued at KES 3,021 via code UFT12345.`
- `You successfully sold 2.5 ACME shares valued at KES 1,250 via code ABC12345.`

Names/symbols in tests are fixtures only. Production logic does not branch on Zidi, a broker, EQA, ACME, or any provider/security name.

### Semantic investment facts

`FinancialSemanticAnalyzer` now extracts an optional structured investment fact:

- action: BUY or SELL;
- stage: ORDER_SUBMITTED or EXECUTED;
- quantity in fixed-point microunits;
- instrument label;
- normalized instrument key;
- investment value when stated;
- source message spans for quantity/instrument.

Fractional quantities are supported without floating-point storage. One whole share/unit equals 1,000,000 quantity microunits.

### Order submitted is not execution

A submitted order is persisted as pending investment evidence only.

It does **not**:

- modify observed position quantity;
- create a cash transaction;
- reclassify an existing payment;
- imply that shares were actually bought or sold.

Only an explicit executed-trade semantic fact can create an observed position delta.

### Local investment event ledger

Schema version is now **23**.

New local tables:

- `investment_events`;
- `investment_event_references`;
- `investment_positions`.

`investment_events` stores:

- ORDER_SUBMITTED, BUY_EXECUTED or SELL_EXECUTED;
- instrument identity;
- quantity;
- stated investment value;
- linked canonical money transaction when proven;
- source/time metadata.

`investment_event_references` preserves all normalized references observed for an investment event.

Raw SMS text is not stored in these tables.

### Duplicate execution protection

Executed trades can be reported by more than one observation or by slightly different message text.

To avoid double-counting a position:

- event identity prefers shared external references plus event type/instrument/quantity/value;
- before inserting a new event, PesaTrack searches existing investment events for the same semantic facts sharing any reference;
- only one unique match is reused;
- only a newly inserted executed event changes the observed position.

If identity is ambiguous, the store does not collapse multiple events into one automatically.

### Observed position quality

`investment_positions` tracks **net observed quantity change from imported execution events**.

This is intentionally not labelled as an authoritative brokerage holding.

For example:

- observed BUY 3 -> net observed +3;
- later observed SELL 1 -> net observed +2.

If imported history begins with a sale larger than the buys PesaTrack has observed, the position is marked `INCOMPLETE_HISTORY` rather than pretending the negative observed delta is the user's true holding.

A future broker statement/position baseline can establish authoritative holdings separately.

### Exact-reference settlement

An executed trade is linked to a wallet/bank transaction only when strong deterministic evidence exists.

Automatic settlement requires:

1. an exact external reference shared by the investment execution and exactly one canonical transaction;
2. stated execution value equals the canonical transaction amount;
3. BUY corresponds to an outgoing/expense movement;
4. SELL corresponds to an incoming movement;
5. the transaction is not excluded;
6. the transaction is not user-classified;
7. no Smart Rule protects it;
8. it is not already part of another relationship.

No amount/time-only investment matching is used in this slice.

### Accounting treatment of a strongly linked trade

When the settlement link is proven:

- category becomes `INVESTMENT`;
- direction becomes `TRANSFER`;
- relation type becomes `INVESTMENT_BUY` or `INVESTMENT_SELL`;
- evidence records that exact shared-reference settlement established the relationship.

This prevents:

- a purchase of shares being counted as ordinary consumption spending;
- sale proceeds being counted as ordinary income.

The original money movement remains the canonical transaction.

### Out-of-order ingestion

Settlement works in either order.

If the investment execution arrives first:

- the investment event is persisted;
- later arrival/re-import of a transaction carrying the same reference can reconcile it.

If the money transaction arrives first:

- its existing external-reference index is available;
- the later executed investment event can link immediately.

A single cash transaction is not allowed to auto-settle multiple investment executions.

### Semantic-only ingestion

Investment order/execution messages can now be persisted even when the ordinary transaction interpreter intentionally produces no ledger movement.

This preserves the distinction:

- semantic understanding means PesaTrack understands the investment event;
- ledger recognition means PesaTrack has an actual cash/accounting movement.

The semantic investment event does not invent missing money movement.

### Regression coverage

The semantic regression suite now protects:

- executed BUY extraction;
- submitted BUY order extraction without pretending it executed;
- executed SELL extraction;
- fractional quantity conversion;
- instrument normalization;
- investment-value extraction;
- existing order/execution semantic roles.

Code head `d796d593ec115ac92c1ffc0c3209c706882d2c69` passed Android CI run #354:

- entity-enrichment Worker validation: passed;
- Android compilation: passed;
- Android unit tests: passed;
- debug APK assembly: passed.

### Deliberately outside this slice

This phase does not yet add:

- an authoritative brokerage position/balance import;
- market-price valuation or unrealized P/L;
- corporate actions such as splits/rights issues;
- dividend settlement into holdings;
- manual review UI for ambiguous investment settlement;
- broker API synchronization;
- provider-specific fee/tax arithmetic.

Do not infer a complete portfolio from partial SMS history.

### Recommended real-device validation

After the rolling APK contains this merge, validate with a controlled history window:

1. submitted investment order appears as understood/pending and does not change observed quantity;
2. executed BUY creates one investment execution and one positive observed quantity delta;
3. re-importing the same execution does not add quantity twice;
4. executed trade + wallet transaction sharing the same reference/value produces one canonical investment transfer;
5. executed SELL with a matching incoming movement is not treated as normal income;
6. execution without a shared transaction reference remains an investment event without inventing a cash link;
7. unrelated same-amount transactions do not settle the trade.

Permanent architecture rule remains:

**financial language establishes investment intent and execution; exact references establish settlement; partial message history is not an authoritative brokerage position.**


## Financial Intelligence V2 — investment settlement manual review — PR #88

Status on 2026-09-30: **implemented and Android CI verified on code head `20afb890f6ae883348adff37be0eace2560ef3f0` before this documentation commit.**

This work was started when the conversation asked to continue “Phase 4”. The live repository already contained the original Phase 4 event-chain reconciliation (PR #83), Phase 5 manual debt relationship review, the real-device correctness repairs, liability-payment bridge, and investment settlement foundation through PR #87. This slice therefore continues from current `main` instead of duplicating already-merged Phase 4 logic.

### Gap being closed

PR #87 deliberately allowed automatic investment settlement only when exact deterministic evidence proved the relationship.

That left an intentional gap:

- an executed BUY/SELL is understood;
- its value is known;
- one or more plausible wallet/bank cash movements exist;
- but exact-reference evidence is absent or ambiguous.

PesaTrack must not guess in that situation, but the relationship should not remain invisible either.

PR #88 adds the same manual safety principle already used for debt relationships:

**automatic intelligence may propose; uncertain financial truth requires an explicit user decision.**

### Activity -> Links integration

A new relationship-review kind is added:

- `INVESTMENT_SETTLEMENT`.

Executed investment events with plausible unresolved cash movements now appear in Activity -> Links.

The review card shows:

- the instrument;
- executed BUY or SELL;
- stated execution value;
- execution time;
- plausible cash candidates;
- why each candidate is plausible;
- `Use as investment settlement`;
- `None of these settled it`.

Submitted investment orders are never settlement-review candidates.

### Provider-neutral candidate rules

A new pure `InvestmentSettlementReviewPolicy` defines eligibility.

A candidate requires:

1. the investment execution has a positive stated value;
2. transaction amount exactly matches that value;
3. BUY corresponds to an outgoing EXPENSE;
4. SELL corresponds to an incoming INCOME;
5. the transaction is not protected by user classification, exclusion, Smart Rule, another relationship, debt linkage, or another investment settlement;
6. either:
   - an exact external reference is shared; or
   - the transaction is inside the bounded ten-minute review window.

Important distinction:

- exact-reference ambiguity can remain reviewable even outside the time window;
- amount/time similarity alone **never auto-links** the investment;
- amount/time only creates a candidate for the user.

The production policy contains no broker/provider-name routing.

### Durable user decisions

Schema version is now **24**.

`investment_events` adds:

- `settlement_user_decision`;
- `settlement_confidence`;
- `settlement_reason`.

If the user chooses a transaction:

- the candidate is rebuilt and revalidated before the write;
- the canonical money movement is reclassified through the existing investment-settlement path;
- category becomes INVESTMENT;
- direction becomes TRANSFER;
- relation remains INVESTMENT_BUY or INVESTMENT_SELL;
- evidence source becomes `investment_user_confirmed`;
- evidence confidence becomes `USER_CONFIRMED`;
- the investment event records the linked transaction and remembered decision.

If the user chooses `None of these settled it`:

- a durable keep-unlinked decision is stored;
- automatic investment settlement no longer retries that event.

### Re-import protection

A duplicate/re-imported execution no longer bypasses manual review decisions.

The automatic `tryLinkEvent` path now reloads the event through the same eligibility gate used by automatic settlement:

- already linked events are not relinked;
- events with a saved manual settlement decision are not automatically reconsidered.

Therefore a later SMS import cannot silently overwrite `None of these settled it`.

### Existing exact-reference automation remains

PR #88 does not weaken or replace PR #87.

When one exact shared reference + matching value + correct direction uniquely proves settlement, automatic settlement still occurs without asking the user.

Manual review is only the safety valve for unresolved/ambiguous cases.

### Regression coverage

A pure unit-test suite now protects:

- exact-reference ambiguity can be reviewed outside the time window;
- same-value/correct-direction candidates inside the ten-minute window are reviewable;
- same-value candidates outside the window without a shared reference are rejected;
- wrong cash direction is rejected;
- protected transactions are rejected;
- amount mismatch is rejected.

Existing investment semantic/settlement and Financial Intelligence regression tests remain in force.

Code head `20afb890f6ae883348adff37be0eace2560ef3f0` passed Android CI run #357:

- entity-enrichment Worker validation: passed;
- Android compilation: passed;
- Android unit tests: passed;
- debug APK assembly: passed.

### Deliberately outside this slice

This work does not add:

- authoritative brokerage position/balance baselines;
- market-price valuation or unrealized P/L;
- corporate actions;
- dividend settlement;
- broker API synchronization;
- provider-specific fee/tax arithmetic;
- automatic amount/time-only investment settlement.

### Recommended real-device validation

Use a controlled history window and verify:

1. exact-reference investment settlement still happens automatically;
2. executed BUY with same-value outgoing cash but no exact shared reference appears in Links rather than auto-linking;
3. executed SELL with same-value incoming cash appears in Links;
4. choosing a candidate reclassifies the cash movement once and links the event;
5. choosing `None of these settled it` removes the review and remains remembered after re-import;
6. submitted orders never appear as settlement reviews;
7. user-classified/Smart Rule/debt-linked transactions are never offered as investment settlement candidates.

Permanent rule remains:

**financial language establishes investment execution; exact evidence may establish settlement automatically; ambiguous settlement requires an explicit user decision.**


## Repository audit and cleanup — 2026-10-01

A repository-wide handoff audit was performed from current `main` after PR #104.

### Verified state

- Current main head at audit start: `3e39179c67666ee992e19bbc3490706750a83cd0`.
- Android CI run `36877863453` passed on that head.
- Worker contract validation passed.
- Android unit tests and debug APK assembly passed.
- The repository contains 163 tracked files: 95 production Kotlin files, 48 Kotlin unit-test files, 4 Android resource files, 2 GitHub workflows, project documentation/build files and the entity-enrichment Worker.
- Home, Activity, Plan, accounts, debts, budgets and transaction details are connected to the local source of truth.
- Goals and Goal Details remain intentional preview UI. Do not connect them by treating unspent cash as saved money.
- The online entity-enrichment boundary still rejects sensitive descriptors and no audit evidence showed raw SMS logging or raw SMS upload to the Worker.

### Cleanup applied on audit branch

Branch: `audit/repository-cleanup-2026-10-01`.

The cleanup intentionally avoids changing financial semantics. It:

- upgrades deprecated GitHub Actions runtime dependencies used by Android CI and Worker deployment;
- removes the nullable deque-access warning in the notification listener;
- makes mixed SQLite bind-argument arrays explicit for Kotlin 2.6 compatibility;
- removes unnecessary non-null assertions in Home, transaction details, onboarding, review grouping and debt-balance update code;
- preserves the legacy `LUNCH` migration value without directly referencing the deprecated enum entry;
- migrates directional Compose icons to AutoMirrored variants and moves `LocalLifecycleOwner` to lifecycle-runtime-compose 2.9.4, retaining compileSdk 36 compatibility;
- moves the data-heavy Activity, Plan, Accounts/Debts and Budget read/rebuild paths off the Compose main thread, and moves Activity review/reconciliation writes to `Dispatchers.IO`;
- adds workflow concurrency so superseded Android CI and Worker deployment runs can be cancelled instead of consuming CI capacity in parallel;
- refreshes the repository README so it describes the actual SQLite/Financial Intelligence V2 architecture instead of the old bootstrap state.

### Audit findings intentionally not converted into automatic fixes

- Several large files remain candidates for later refactoring: `SmartOnboardingSetupScreen.kt`, `ActivityScreen.kt`, `TransactionStore.kt`, `DebtStore.kt`, `HomeScreen.kt` and `FinancialMessageIngestionCoordinator.kt`. Their size is a maintainability concern, but splitting them during a cleanup-only pass would create unnecessary regression risk.
- Goals/Goal Details still contain preview amounts/dates by design.
- Profile is still a presentation-only surface, but the repository cleanup removed the hard-coded personal name and disables the unfinished Edit Profile action instead of exposing a dead button.
- Settings rows without destinations are now rendered as informational rows without click affordances; only wired destinations show chevrons/click behavior. Privacy copy now accurately states that core records stay local while optional enrichment uses sanitized business descriptors.
- Compose AutoMirrored icon deprecations were cleaned up, and `LocalLifecycleOwner` now uses the lifecycle-runtime-compose API. Any remaining compiler warnings after CI should be treated as focused compatibility cleanup rather than mixed into financial-semantics changes.
- There are 48 JVM unit-test files but currently no `app/src/androidTest` instrumentation tests. Real-device validation remains important for permission flows, notification capture, onboarding/back navigation, database migrations, responsive Compose behavior and lifecycle behavior.
- The entity-enrichment Worker intentionally exposes a public `/v1/entity/resolve` route. The provider secret remains server-side and the descriptor privacy filter is intact, but the endpoint can still be abused for quota/cost consumption. Before broad distribution, add a Cloudflare rate-limiting binding or equivalent edge rate limit; do not put a shared secret in the APK.
- The Android activity is still portrait-locked. Treat any orientation/tablet policy change as a focused responsive-UI task rather than silently changing it during cleanup.

Permanent audit rule: prefer small evidence-backed corrections over broad refactors when changing ingestion, accounting, relationship reconciliation or payday logic.


## M-Shwari debt + ownership confirmation follow-up — 2026-10-01

This follow-up was driven by real-device screenshots/messages after the repository audit.

### M-Shwari semantic correction

- Treat an M-Shwari loan approval whose proceeds are deposited into M-PESA as a debt drawdown whose proceeds account is M-PESA. Do **not** model that message as a withdrawal from M-Shwari savings or Lock Savings.
- M-Shwari regular savings, M-Shwari Lock Savings and M-Shwari Loan remain separate financial identities.
- Loan-product identity is resolved from message evidence, not merely from the SMS thread/sender title. This is important because an M-Shwari loan can appear in an M-PESA message thread.
- Generic debt grammar stays provider-agnostic; the provider registry supplies product identity only when the message contains enough evidence.

### Unknown-principal safety rule

A disbursement message can report net proceeds after excise duty/fees without exposing the exact outstanding principal. PesaTrack must not manufacture the loan balance from that net cash amount.

When a confirmed debt drawdown exists but no explicit outstanding balance has been observed:
- keep the debt visible;
- display the outstanding value as pending/unknown;
- exclude the unknown amount from total-debt arithmetic;
- replace it once a later explicit loan-balance observation arrives.

### Regression coverage

Added real-message/regression coverage for:
- M-Shwari approval in an M-PESA thread;
- M-Shwari loan balance wording using both `balance is` and `balance of`;
- M-Shwari partial repayment funded from M-PESA;
- contextual liability resolution so `M-Shwari` in debt grammar maps to `NCBA_MSHWARI_LOAN`, not the savings account.

### Ownership-review UX fix

The Activity ownership dialog now:
- enters a visible Saving state immediately after Yes/No;
- disables repeated taps while persistence/reconciliation runs;
- closes immediately after a successful ownership decision;
- runs the optional historical SMS re-import only after the dialog is closed;
- keeps the dialog open and shows a user-visible error if persistence fails.

Do not infer additional M-Shwari Lock Savings transfer/balance grammar from reconstructed balances alone. Add or adjust those rules only from concrete Lock Savings message evidence.


## Brain v3 Phase 2 — Kenya core corpus increment 1 — 2026-10-01

Blueprint guardrail: `docs/PESATRACK_BRAIN_V3.md` remains the implementation order. Do not jump to the Identity Graph, personal learning or Home redesign until the Kenya core corpus milestone is deliberately closed.

### Corpus expansion

- Message Pack grows from 11 to 21 active templates.
- Added evidence-backed M-Pesa shapes for merchant payment, completed reversal, credited reversal, external-account transfer, cash withdrawal, cash deposit and airtime purchase.
- Added M-Shwari savings credit-from-M-Pesa and Lock Savings release-to-M-Shwari shapes.
- M-Shwari loan-balance template remains one stable ID and advances to revision 2 for `Sh`, `Ksh`, `Kshs` and `KES` variants.
- Added a sanitized KCB M-PESA declined-loan-repayment shape from a published message example.
- Every active Message Pack template must now be represented by at least one sanitized golden fixture.
- `fixtures-v2.json` adds additional variants for several existing core templates rather than assuming one sentence proves a shape.

### KCB M-PESA product knowledge

Official KCB product mechanics are represented separately from ordinary M-Pesa and ordinary KCB bank accounts:

- `KCB_MPESA_SAVINGS` = KCB M-PESA deposit/savings asset.
- `KCB_MPESA_LOAN` = KCB M-PESA liability.
- KCB M-PESA loan proceeds fund the KCB M-PESA deposit/savings account first, not ordinary M-Pesa.
- The KCB M-PESA sender profile anchors observed KCB M-PESA account balances to the savings/deposit account.

### Failed-action correctness rule

Financial Intelligence V2 now understands a provider-agnostic `FAILED_ACTION` semantic intent for wording such as declined, rejected, unsuccessful, failed or insufficient funds.

Permanent rule:
- a failed/declined financial attempt must not create payment, transfer, withdrawal, deposit, reversal, drawdown or repayment ledger events;
- explicit observed balances contained in that message may still be persisted;
- the message remains semantically understood and can be shown in diagnostics.

This rule was added because a real/public KCB M-PESA repayment-declined example contains both the phrase `loan repayment` and an amount, which would be unsafe to interpret as money movement.

### PesaLink status

Official PesaLink/KCB documentation confirms that PesaLink is an inter-bank transfer network and that one transfer can generate both bank and PesaLink confirmations. This supports PesaTrack's existing cross-message correlation architecture.

No concrete PesaLink Message Pack template is added in this increment because the official documentation describes fields/behavior but does not provide enough exact current SMS wording. Wait for a sanitized real/public message fixture rather than inventing syntax.

Evidence references are recorded in `docs/message-corpus-sources.md`.


## Brain v3 Phase 2 — bank corpus + Message Pack candidate bridge — 2026-10-01

This increment continues the PDF/Brain-v3 order. It does not start Identity Graph, personal learning or the Home redesign.

### Coverage

The Kenya Message Pack grows from 22 to 35 active templates.

Public-template-backed shapes are added for:

- I&M Bank payment;
- KCB Bank credit, app receipt, card purchase, paid-to, salary credit, M-Pesa transfer variants and PesaLink send;
- PesaLink sent/received network confirmations;
- Standard Chartered card purchase.

Source provenance is recorded in `docs/message-corpus-sources.md` and `docs/third-party-reference/sms-transaction-parser.md`.

### Architectural correction

A Message Pack match is not considered sufficient intelligence by itself.

`MessagePackCandidateBridge` now supplies a V2 action candidate only when:
1. generic V2 extraction found no action;
2. the matched template has explicit amount evidence;
3. the matched template has the explicit counterparty/source/destination required for that action type.

The bridge does not classify or persist transactions. FinancialRelationshipResolver and FinancialEventClassifier remain the accounting authority.

PesaLink remains a transfer-network observation and must not be promoted into a fake owned account when no source/destination bank account is explicit.


## Brain v3 Phase 2 — DTB Till Moja corpus increment — 2026-10-02

- Added two DTB Till Moja Message Pack shapes: payer confirmation and merchant receipt.
- Message Pack count advances from 35 to 37 active templates.
- DTB identity continues to come from the generic Kenyan bank directory as `KENYAN_BANK_DTB`.
- Both shapes are backed by sanitized fixtures and canonical Payment/Receipt integration tests.
- Evidence provenance is documented in `docs/message-corpus-sources.md`.
- Keep general Co-op, Absa, NCBA and Stanbic transaction messages on V2 fallback until exact message bodies are available.


## Brain v3 Phase 3 — Identity Graph foundation — 2026-10-02

The blueprint has moved from message-language coverage to durable identity understanding. Phase 2 remains open for new evidence-backed bank shapes, but missing exact Co-op/Absa/NCBA/Stanbic SMS bodies are not a reason to invent templates.

### Database

Schema advances from v27 to **v28** with four new local-first tables:

- `identity_nodes`
- `identity_aliases`
- `identity_links`
- `identity_observations`

`identity_observations.transaction_id` is the idempotence key so historical synchronization cannot count the same ledger transaction repeatedly.

### Identity model

The graph distinguishes:

- SELF
- PERSON
- BUSINESS
- BILLER
- FINANCIAL_INSTITUTION
- FINANCIAL_ACCOUNT
- UNKNOWN

Aliases can be NAME, PHONE, ACCOUNT_ID, ACCOUNT_IDENTIFIER, PROVIDER_KEY or SMS_SENDER.

Evidence records whether an identity came from user confirmation, account storage, a confirmed financial entity, transaction counterparty evidence, the local entity catalogue, remote enrichment or a message source.

### Safe learning rules

- Enabled money accounts are modeled as owned financial accounts and linked from SELF using `OWNS`.
- A valid user-account phone identifier can seed a user-confirmed SELF phone alias.
- Known local entities such as Naivas/Kenya Power/KCB can retain their catalogue type.
- An unknown name remains UNKNOWN regardless of frequency.
- A repeated name without stronger evidence is a provisional cluster only.
- Phone evidence can distinguish two counterparties sharing the same name.
- Own-account transfer evidence may create a question candidate, never automatic self-identity.
- Repeated unknown counterparties may create a classification question; frequency itself is not the classification.

### Integration

After a canonical transaction is successfully inserted, `FinancialMessageIngestionCoordinator` performs a bounded Identity Graph sync.

Graph synchronization is intentionally isolated from accounting: graph errors are non-authoritative metadata failures and must not roll back correct financial ingestion.

Data Health runs a larger history backfill and reports identity nodes, aliases, confirmed self aliases, pending questions and still-unclassified identities.

### Next increment

Build the actual Identity Review experience and persist explicit answers:

- Yes, this is my phone.
- Yes, this is my transaction name.
- Person / Business / Biller / Financial institution / Not sure.

Do not start personal category learning or the Home redesign before those identity decisions are safely reviewable and durable.


## Brain v3 Phase 3 — Identity Review — 2026-10-02

Identity Graph questions are now reviewable through a dedicated Data Health flow.

### Database

Schema advances from v28 to **v29** with `identity_review_decisions`.

### Review semantics

- `CONFIRM_SELF_PHONE` and `CONFIRM_SELF_NAME` can be confirmed as SELF through the existing graph confirmation path.
- **Not mine** records a durable SELF rejection for that identity. It suppresses future SELF phone/name prompts without forcing an external type.
- **Not sure yet** records a deferred decision with the current observation count. The same question stays hidden until new evidence increases.
- `CLASSIFY_COUNTERPARTY` supports user-confirmed Person, Business, Biller and Financial institution.
- A classification decision clears obsolete review decisions for that identity.
- A SELF confirmation clears obsolete review decisions for that identity.

### Product boundary

The review screen is under Data Health/More, not Home. No transaction category is learned or applied in this increment, and no Smart Rule is generated.

Next: Phase 4 personal learning, built on durable identity + review evidence rather than raw name repetition.


## Brain v3 Phase 4 — Personal Learning increment 1 — 2026-10-02

Phase 3 is complete through durable Identity Review. The implementation has now moved into Phase 4 without starting the Home redesign.

### Implemented

- Database schema advances from v29 to **v30**.
- Added `identity_category_preferences` for explicit remembered category preferences tied to Identity Graph nodes.
- Added `personal_learning_applications` for explainable auto-application provenance.
- Existing exact-name / exact-phone Smart Rules remain unchanged and have priority.
- Choosing **Remember** now also teaches the resolved Identity Graph identity when the chosen category is an ordinary expense category.
- High-confidence or user-confirmed identity matches may auto-apply that remembered category.
- Medium-confidence identity matches surface a Transaction Details suggestion and require confirmation.
- Low-confidence identity matches remain in normal review.
- **Only this one** deliberately does not create a reusable personal-learning preference.
- Structural categories and accounting states are outside this first learning increment: transfer, income, debt/loan, savings, investment, cash, uncategorized and Other are not identity-learned.
- USER_CLASSIFIED, EXCLUDED, Smart-Rule-controlled and linked financial records are not overwritten.
- Data Health shows learned category identity count and auto-applied learning count.
- Pure policy tests cover high / medium / low confidence and structural-category guardrails.

### Guardrail

Personal learning is downstream of deterministic transaction understanding and Identity Graph resolution. It must never become a second parser, infer categories from raw name frequency, override an explicit user decision, or change debt/transfer/accounting semantics.

### Next

Continue Phase 4 with correction/revocation semantics and conflict handling for remembered identity preferences. Do not begin Product Simplification/Home redesign until the personal-learning authority and correction paths are durable.


## Brain v3 Phase 4 — Personal Learning increment 2 — 2026-10-02

Phase 4 personal learning now has a reversible correction path.

Implemented:

- Auto-learned transactions expose human-readable provenance in Transaction Details.
- Provenance includes stable identity label, alias/evidence path, confidence and prior confirmation count.
- Correcting an auto-learned ordinary expense updates the identity's learned category.
- Correcting it into a structural accounting category (for example Transfer) removes the old learned expense preference.
- **Keep this, forget future learning** deletes the reusable preference without rewriting the current transaction.
- **Undo & forget** deletes the reusable preference and safely reopens the current item as Needs Review / Other.
- Medium-confidence suggestions now say why the identity matched.
- Explicit user classifications and Smart Rules remain authoritative.
- Added policy tests for correction-versus-forget behavior.

Phase 4 is now ready to hand off into Phase 5 Product Simplification.

Start Phase 5 with the unified Needs Attention question model and Activity hierarchy. Do not remove the underlying duplicate/link/identity/unsupported-message capabilities; hide their technical implementation behind one human review surface.


## Phase 5 Product Simplification — increment 1 — 2026-10-02

Phase 4 is complete through reversible personal-learning correction/revocation. Phase 5 has started.

Implemented:

- Primary bottom navigation is now Home / Activity / Plan / Accounts / More.
- Community moved under More.
- Historical import is exposed under More instead of daily Home.
- Goals moved into the Plan hierarchy.
- Accounts is now a real primary tab rather than a Home detail route.
- Activity replaces the three technical Review / Duplicates / Links controls with one expandable Needs Attention surface.
- Needs Attention aggregates category review, possible duplicates, relationship questions, account ownership, identity review, and unsupported-message coverage.
- Activity filter language is simplified to All / Needs review / Spending / Income / Transfers.
- The primary Activity surface no longer exposes the raw financial-source feed control.
- Home no longer shows the Review / Import / Rules / Goals quick-action row.
- Home shows a compact Needs Attention card only when its current transaction snapshot has review work.

Guardrail:

Do not delete the underlying duplicate/link/identity/Message Lab capabilities. Product Simplification hides their implementation details behind human questions; it does not weaken the evidence or accounting model.

Next:

Continue Phase 5 with the Home information hierarchy from the blueprint and then reduce the large Home/Activity composables into smaller feature modules.


## Phase 5 Product Simplification — increment 2 — 2026-10-02

The Home information hierarchy now follows the product blueprint.

Implemented:

- Home order is Available this cycle -> Needs Attention -> Today -> Where money went -> Money picture -> Recent activity -> One useful insight.
- Available this cycle keeps deterministic pay-cycle math and shows cycle timing context instead of duplicating daily metrics inside the hero.
- Needs Attention is conditional and now counts multiple open review sources, not only category review.
- Today has trusted spend, safe daily pace and remaining protected commitments.
- Where money went is reduced to the top three trusted categories; budget management routes into Plan.
- Money picture combines observed wallet freshness, confirmed account count and active debt summary without inventing missing balances.
- Recent activity shows up to five canonical transactions, suppresses the second side of unresolved possible-duplicate pairs, and visibly marks Needs Review items as uncertain.
- Home Needs Attention opens Activity with the unified inbox already expanded; Recent activity → See all opens the normal timeline.
- Home keeps exactly one insight card.
- Plan now exposes Budget and Goals as child destinations.
- Accounts now exposes active debt and debt details as part of the account hierarchy.
- Home no longer loads monthly budget records for a separate daily budget card.
- New modular Home components live in `HomeOverviewComponents.kt`.

Next Phase 5 work:

- remove/relocate remaining dead legacy Home presentation helpers;
- continue decomposing Activity/Home files;
- improve Accounts with savings/investment grouping and balance freshness where evidence exists.


## Phase 5 Product Simplification — increment 3 / structural completion — 2026-10-02

Phase 5 has completed its structural product-hierarchy goal.

Cleanup performed:

- Removed legacy Home Debt Overview, Wallets & Balances, wallet-row and Payday Picture presentation paths that were replaced by the new seven-block Home.
- Removed obsolete Home-only helpers/imports tied to those cards.
- `HomeScreen.kt` was reduced from roughly 2,346 lines after the hierarchy migration to roughly 1,600 lines.
- Active Phase 5 Home cards remain in `HomeOverviewComponents.kt`.

Current primary product hierarchy:

- Home: Available this cycle, conditional Needs Attention, Today, Where money went, Money picture, Recent activity, one insight.
- Activity: canonical timeline + unified Needs Attention.
- Plan: budget + recurring commitments/pay cycle + goals.
- Accounts: confirmed wallets/banks/savings plus active debt entry.
- More: Data Health, Import, Community, Profile, Settings.

The deterministic financial engine, Identity Graph, personal learning, debt accounting and message corpus remain unchanged by this cleanup.

Next build-order phase after this is Phase 6 / long-tail coverage. Do not regress the Phase 5 hierarchy while adding new providers or products.

## Phase 6 Long-tail Coverage — increment 1 — 2026-10-02

Phase 5 product simplification is structurally complete. Phase 6 has started with adjacent-finance Message Pack coverage.

Implemented:

- Kenya Message Pack expands from 37 to 45 active templates.
- Added Etica payment, redemption and M-Pesa withdrawal shapes.
- Added Evopay card-parking and LPN-parking shapes.
- Added Globalpay card-purchase shape with fee extraction.
- Added Sanlam Money Market Fund credit and top-up shapes.
- Added sanitized `fixtures-v6.json` golden corpus.
- Added provenance tests for investment-specific Etica/Sanlam shapes.
- Added structured PAYMENT provenance/extraction coverage for Evopay.
- Investment-specific event kinds are deliberately not bridged into ordinary payment/income/transfer candidates.
- Public Etica USD extraction is provenance-only; no KES conversion or ledger posting is invented.

Evidence trail is documented in `docs/message-corpus-sources.md`.

Next:
- evidence-backed bank/app-notification shapes for remaining institutions;
- recurring/subscription intelligence after corpus expansion;
- preserve Phase 5 Home/Activity hierarchy while adding coverage.


## Phase 6 Long-tail Coverage — increment 2 / recurring intelligence — 2026-10-02

Phase 6 now includes a deterministic recurring-payment suggestion layer.

Implemented:

- Database schema advances from v30 to **v31** with `recurring_pattern_decisions`.
- Added pure `RecurringPatternPolicy` and models.
- Added `RecurringPatternStore` over the existing canonical transaction history, Identity Graph and Smart Rules.
- Monthly candidates require at least three trusted observations and every interval must fall within 24-38 days.
- Candidates older than 60 days or with more than 35% amount variation are rejected.
- HIGH confidence requires at least four observations, 27-33 day median cadence, <=10% amount variation and <=6 days interval spread.
- Structural/accounting categories are excluded: income, transfer, debt, cash, savings and investment. Other/Uncategorized are also excluded.
- Existing Smart Rules, linked events, reversals, Needs Review and excluded transactions do not produce suggestions.
- SELF and owned financial-account identities do not produce recurring suggestions.
- Plan now shows up to three **Possible recurring payments**.
- Suggestions show cadence, typical median amount, expected next date and confidence.
- Nothing is added automatically.
- **Add to plan** creates an exact identity-backed `OTHER_RECURRING` Smart Rule with the median amount as its planned amount and rebuilds the existing pay-cycle commitment model.
- **Not recurring** is durable. The same pattern remains suppressed until at least two additional observations arrive.
- Added pure policy regression tests for strong monthly cadence, medium cadence, frequent-merchant rejection, volatile-amount rejection and stale-pattern rejection.

Phase 6 remains in progress. Continue provider/app-notification/SACCO/lender coverage only from concrete sanitized/public message evidence; do not invent provider wording from product documentation.

## Phase 6 Long-tail Coverage — increment 3 / bank account alerts — 2026-10-02

Phase 6 bank coverage has another evidence-backed increment.

Implemented:

- Kenya Message Pack expands from 45 to **48 active templates**.
- Added Co-operative Bank compact account credit and debit alert shapes.
- Added Equity account-credit alert shape.
- Added account reference, amount, date, balance and reference extraction.
- Added `fixtures-v7.json` sanitized golden corpus.
- Added provenance regression coverage for Co-operative Bank and Equity account alerts.
- Evidence comes from an ISC-licensed public Kenyan SMS-finance sample/parser repository and is documented as **PUBLIC_SAMPLE_IMPLEMENTATION**, not bank-verified live wording.
- New shapes use `ACCOUNT_CREDIT` / `ACCOUNT_DEBIT` event kinds so Message Pack provenance cannot invent a counterparty or silently turn a generic account alert into an ordinary payment/receipt.
- No ledger arithmetic, pay-cycle math, debt balances, personal-learning decisions or Phase 5 navigation were changed.

Continue Phase 6 with additional banks, SACCOs, lenders and bank-app notification wording only from concrete sanitized/public message evidence.


## Phase 6 Long-tail Coverage — bank + SACCO notification corpus — 2026-10-02

The latest Phase 6 coverage increment is rebased on top of the merged Co-op/Equity account-alert work.

Implemented:
- Kenya Message Pack grows from 48 to **53 active templates**.
- Added Fundilima SACCO withdrawal provenance.
- Added generic SACCO member-deposit provenance.
- Added generic bank-to-M-Pesa credit provenance.
- Added a confirmed bank-app bill-payment shape.
- Added a generic account-credit/balance-notice shape.
- Added sanitized `fixtures-v8.json`.
- Added router/interpreter regression tests.
- Ambiguous SACCO/account-transfer shapes remain provenance-only and do not become ordinary income/expense/transfer ledger entries merely because their wording was recognized.
- Confirmed bill payment may expose PAYMENT semantics because the message itself clearly states the completed action.

Evidence source and sanitization rules are recorded in `docs/message-corpus-sources.md`.

Phase 6 remains in progress. The source-aware app-notification routing work should be reconciled next so future app-only shapes can constrain matches by Android package/channel evidence.


## Phase 6 Long-tail Coverage — source-aware notification routing — 2026-10-02

The previously green source-aware app-notification implementation has been replayed on top of the latest Phase 6 main so newer Co-op/Equity and bank/SACCO corpus work is preserved.

Implemented:
- Message Pack templates support optional `sourcePackages` and `sourceKinds`.
- FinancialMessageInterpreter and KenyaMessagePack routing accept source package/channel evidence.
- FinancialMessageIngestionCoordinator forwards notification source metadata.
- Source-constrained templates fail closed when package/channel evidence is absent or mismatched.
- Source-constrained matches outrank sender-only matches.
- Parse provenance records when package/channel evidence participated.
- Unsupported Message Lab shapes retain safe source package/channel metadata locally.
- Source metadata contributes to unsupported-shape identity.
- Developer fixture export and Data Health expose the source context.
- Notification listener UI label is generalized to PesaTrack financial notification listener.
- Existing unconstrained SMS templates remain backward compatible.
- Current 53-template Kenya Message Pack corpus is preserved.

Next Phase 6 work can safely add app-only bank notification templates once concrete package + sanitized notification evidence is available.


## Phase 6 Long-tail Coverage — increment 5 / financial institution identities — 2026-10-02

The 53-template bank/SACCO corpus is already on main. Long-tail identity coverage now extends beyond the original KCB/Equity/Branch/Tala seeds.

Implemented:

- Added `EntityKind.SACCO`.
- SACCO catalog entities map to `IdentityNodeKind.FINANCIAL_INSTITUTION`.
- Added canonical local identities and conservative aliases for Co-operative Bank of Kenya, NCBA Bank Kenya, Absa Bank Kenya, Stanbic Bank Kenya, Diamond Trust Bank Kenya, Family Bank, Stima DT Sacco and Mwalimu National Sacco.
- Added source documentation in `docs/entity-catalog-sources.md`.
- Added tests for Stima Sacco resolution, DTB alias resolution, no raw-message-only bank identity match, and SACCO Identity Graph mapping.
- New financial-institution seeds have no default expense category, are not auto-review eligible and do not match from arbitrary raw message text.
- No SMS wording was invented and no ledger/category authority changed.

Next Phase 6 increment should reconcile source-aware app-notification routing and Message Lab provenance so app-only shapes can be learned safely.


## Phase 6 Long-tail Coverage — increment 7 / verified app-package identity — 2026-10-02

Phase 6 now recognizes the source institution of selected Android banking-app notifications from verified package IDs.

Implemented:

- Added `FinancialAppSourceRegistry`.
- Verified package identities cover KCB Mobile, Equity Mobile, Co-op Bank, NCBA NOW, Absa Kenya, Stanbic Bank Kenya and DTB 24/7.
- The ingestion coordinator prefers verified package identity for source diagnostics/Message Lab labeling, then falls back to existing title/language resolution.
- Bank-package resolution returns `AccountOwnershipHint.UNKNOWN`; package evidence never claims the user owns an account.
- Package identity does not alter semantic parsing, category logic, ledger arithmetic or recurring-learning behavior.
- Unknown packages remain unresolved.
- Added pure tests for all verified package mappings, normalization and unknown-package fail-closed behavior.
- Evidence is recorded in `docs/entity-catalog-sources.md`.

Next Phase 6 work should add provider-specific app-notification wording only from concrete sanitized/public examples, or continue evidence-backed lenders/SACCOs that are still missing.


## Phase 6 Long-tail Coverage — increment 8 / coverage by source — 2026-10-02

Phase 6 now has an evidence-driven source coverage view.

Implemented:

- Added pure `FinancialCoverageAnalysis`.
- Added `FinancialSourceCoverageSnapshot`.
- Financial diagnostics can now aggregate up to the recent local observation window by source/channel.
- App notifications group by package + source kind; imported/SMS messages group separately by source identity.
- Each source reports total observations, understood count/percent, template matches and unsupported count.
- Coverage gaps sort ahead of already-covered sources.
- Data Health shows up to eight source coverage cards.
- The view remains local and uses existing safe diagnostic metadata; no raw message body is added to diagnostics.
- Added regression tests for package grouping, app-vs-SMS separation and unsupported-first ordering.

Use this view to choose the next bank/SACCO/lender corpus increment from actual unsupported evidence rather than adding speculative provider wording.

## Phase 6 Long-tail Coverage — increment 9 / digital-lender app source identity — 2026-10-02

Phase 6 now recognizes verified Branch and Tala Android app packages as financial source identity.

Implemented:

- Branch package: `com.branch_international.branch.branch_demo_android`.
- Tala package: `com.inventureaccess.safarirahisi`.
- Generalized `FinancialAppSourceProfile` so verified non-bank financial apps do not have to pretend to be entries in `KenyanBankDirectory`.
- Existing bank-package behavior is preserved.
- Branch/Tala source references use `AccountType.OTHER`, unknown ownership and unknown asset/liability nature.
- Ingestion diagnostics and Message Lab can now attribute unsupported Branch/Tala app notifications to the correct institution even if the visible notification title is generic.
- Added regression tests for names, provider keys, conservative type/nature, normalization and known-package checks.
- Evidence/URLs are recorded in `docs/entity-catalog-sources.md`.

No Branch/Tala message wording or debt semantics were invented.

Continue Phase 6 from real Coverage-by-source gaps / redacted Message Lab evidence. Add provider-specific lender templates only when an observed sanitized/public message shape is available.

## Phase 6 Long-tail Coverage — increment 10 / I&M + SACCO app sources — 2026-10-02

Verified package-source coverage now also includes:

- I&M OTG Personal KE — `com.imbank.digital.retail.app`;
- MSTIMA / Stima DT Sacco — `com.mode.stima`;
- MwalimuHELA / Mwalimu National Sacco — `skyworld.mwalimu`.

Behavior:

- I&M uses the existing bank source-reference path.
- Stima/Mwalimu use conservative direct source identities with `AccountType.OTHER`, unknown ownership and unknown asset/liability nature.
- Coverage by source / Message Lab can therefore attribute unsupported notifications correctly without inferring account ownership or transaction meaning.
- Added regression tests and app-store evidence documentation.

Continue Phase 6 from real unsupported source coverage. Do not create SACCO/bank notification templates from app-store feature descriptions alone.

## Phase 6 Long-tail Coverage — increment 11 / more bank app sources — 2026-10-02

Verified Android source identity now additionally covers:

- SC Mobile Kenya — `com.scb.breezebanking.ke`;
- Family Bank PesaPap — `com.mode.familykenya.ui`;
- Ecobank Mobile — `com.ecobank.mobileapp5`;
- SBM Mfukoni — `com.mode.chasetest.ui`.

These use the existing bank-directory source-reference path with BANK/ASSET source convention and UNKNOWN ownership.

Coverage by source and Message Lab can therefore group unsupported app notifications under the correct institution without relying on notification-title wording.

Added regression tests and official Play-store evidence documentation.

Continue Phase 6 from the highest unsupported source groups visible in Data Health; notification templates still require real sanitized/public message evidence.


## Phase 6 Long-tail Coverage — increment 9 / source drill-down — 2026-10-02

Phase 6 Data Health can now move directly from a coverage gap to the relevant local evidence.

Implemented:

- Coverage-by-source cards with unsupported observations are tappable.
- Selecting a source filters Message Lab to that source's unsupported redacted shapes.
- App/SMS evidence remains separated by the same stable source-key logic used by coverage analysis.
- Selected source state is visible in the Message Lab heading and can be cleared with **Show all**.
- Added **Copy redacted source fixtures** to export up to 25 safe fixtures as one JSON array.
- Individual redacted fixture copy remains available.
- `MessageLabStore` supports source-filtered retrieval and batch fixture export.
- `FinancialCoverageAnalysis.sourceKey` is now a reusable pure normalizer.
- Added regression tests for package/channel normalization and display-name fallback.

No transaction meaning, ownership, category, ledger arithmetic, Identity Graph decision, recurring commitment or raw-message upload behavior changes.

Use this workflow to choose the next bank/SACCO/lender template from actual unsupported device evidence instead of speculative provider wording.


## Phase 6 Long-tail Coverage — increment 12 / Message Lab auto-resolution — 2026-10-02

Phase 6 now automatically retires stale unsupported Message Lab shapes after the same structure becomes understood.

Implemented:

- Added pure `MessageLabShapeIdentityFactory`.
- Shape identity uses redacted body + sender + normalized package/channel.
- Sensitive amount/date/time/name/account changes collapse into the same structural shape.
- App and SMS/imported channels remain separate.
- New financial diagnostics persist the privacy-safe message-shape ID.
- `MessageLabStore.markUnderstood(...)` removes a previously stored unsupported shape when a new observation of that same shape is now recognized/semantically understood.
- Prior diagnostics carrying that same shape ID are changed from unsupported to `RESOLVED_BY_CURRENT_COVERAGE`, so Coverage-by-source improves instead of waiting for old records to age out.
- Retired occurrence count is persisted locally.
- Data Health shows both **Resolved by current coverage** and **Resolved Message Lab occurrences**.
- Added regression tests for sensitive-value stability, package separation and channel separation.

Important boundary:

PesaTrack does not attempt to replay already-redacted fixtures through live extractors because placeholders such as `<AMOUNT>` can invalidate regex extraction. Resolution happens only when a new real observation arrives and is understood by the current interpreter.

Continue Phase 6 from Coverage-by-source and Message Lab evidence.

## Phase 6 Long-tail Coverage — increment 13 / lender corpus — 2026-10-02

Phase 6 now includes a larger lender/product Message Pack corpus from the project-supplied Kenyan Financial SMS Parsing Dictionary.

Implemented:

- Kenya Message Pack expands from **53 to 62 active templates**.
- Added M-Shwari fee-bearing loan receipt, Timiza disbursement, Tala disbursement + due notice, Branch disbursement + due notice, Zenka disbursement, OKash disbursement and Hustler Fund split disbursement/savings shapes.
- Added `fixtures-v9.json` golden corpus.
- Added regression tests requiring due notices to remain provenance-only rather than ordinary Payment events.
- Added structured extraction checks for M-Shwari fee/repay amount and Hustler Fund M-Pesa/savings split.
- Evidence is documented as **USER_RESEARCH_REFERENCE** because the supplied dictionary is parameterized internal reference material rather than verified current production wording.
- No provider-specific Kotlin parser branch is added.
- Financial Intelligence V2 remains authoritative for debt drawdown/repayment/account semantics.

Continue Phase 6 from source coverage gaps and reviewed redacted evidence.


## Phase 6 Long-tail Coverage — increment 15 / Zenka + Timiza app sources — 2026-10-02

Added verified Android source identity for:

- Zenka — `com.zenkafinance.microloans` -> `KENYAN_LENDER_ZENKA`;
- Timiza — `ke.co.absa.digitalwallet` -> `KENYAN_FINANCIAL_APP_TIMIZA`.

Both are source-only identities with OTHER account type, UNKNOWN ownership and UNKNOWN financial nature.

Reason:
- Zenka is a verified Kenyan lender app but package identity alone is not a drawdown/repayment signal.
- Timiza is multi-product, so its package cannot be modeled as a loan account merely because it offers loans.

Added regression tests for names, provider keys, normalization and conservative semantics. Evidence URLs are recorded in `docs/entity-catalog-sources.md`.

This increment changes diagnostics/Message Lab source attribution only; no ledger or debt semantics change.


## Phase 6 Long-tail Coverage — increment 14 / lender debt bridge — 2026-10-02

The evidence-backed lender Message Pack can now feed unambiguous drawdowns into the existing canonical debt engine.

Implemented:

- Added stable liability provider identities for Timiza, Tala, Branch, Zenka, OKash and Hustler Fund.
- Added stable Timiza account identity for explicit "your Timiza account" proceeds.
- Added Message Pack debt enrichment for `DEBT_DRAWDOWN`.
- Template fee evidence fills missing generic fee candidates.
- Template `REPAY_AMOUNT` / `TOTAL_DUE` can fill missing observed outstanding-after values.
- Explicit M-Pesa/Timiza proceeds can fill missing proceeds-account evidence.
- Existing generic debt candidates are merged with Message Pack evidence rather than replaced.
- Added canonical debt-event tests for Branch, Timiza, M-Shwari and OKash.
- Added guardrail tests proving Tala due notices remain non-movement and Hustler Fund split drawdowns remain provenance-only.
- Added registry tests for stable lender identities/funding behavior.

No repayment reminder, lender offer, or split savings allocation is turned into a ledger movement by this increment.


## Phase 6 Long-tail Coverage — increment 16 / investment semantics — 2026-10-02

Phase 6 now bridges sufficiently explicit investment Message Pack evidence into the existing provider-neutral investment ledger.

Implemented:

- Added `MessagePackInvestmentSemanticBridge`.
- A deterministic matched financial Message Pack now passes the generic relevance gate so known investment/debt shapes cannot be discarded before their guarded semantics run; unknown text still uses the normal relevance gate.
- Sanlam Money Market Fund credit with PRODUCT + UNITS + AMOUNT becomes a HIGH-confidence executed BUY investment observation.
- Units are converted to microunits; monetary value remains KES minor units.
- Existing `InvestmentStore` remains authoritative for persistence, settlement linking and observed position delta.
- Sanlam amount-only top-up remains provenance-only.
- Etica redemption/withdrawal remains provenance-only because current evidence does not establish unit quantity safely.
- Etica USD redemption remains provenance-only; no currency conversion or KES posting is invented.
- All Message Pack `INVESTMENT_*` shapes suppress ordinary PAYMENT / RECEIPT / TRANSFER classification.
- Provenance-only investment shapes are marked as understood confirmations instead of ordinary spending/income.
- Added explicit `blocksLegacyTransactionParsing` support to financial interpretation.
- Provenance-only investment and debt shapes now stop legacy fallback even when no ordinary event exists.
- Historical import no longer counts safely handled provenance-only shapes as duplicate transactions.
- Added regression coverage for Sanlam fund credit/top-up, Etica KES/USD withdrawal and debt due-notice fallback blocking.

Continue Phase 6 from reviewed Coverage-by-source / Message Lab evidence and from investment/debt cases only where the current canonical models can preserve the financial meaning without guessing.


## Phase 6 Long-tail Coverage — increment 17 / core fixture depth — 2026-10-02

Phase 6 corpus quality now has an explicit regression floor.

Implemented:

- Added `fixtures-v10.json` with 15 additional sanitized core-flow examples.
- M-Pesa send/receive/PayBill, M-Shwari transfer/loan and Fuliza drawdown/repayment shapes now each have at least three golden fixtures.
- Added `MessagePackCorpusHealthTest`.
- Every active template must have at least one golden fixture.
- Fixture IDs must remain unique and fixtures may only reference active templates.
- The ten core ambiguity templates cannot drop below three fixtures.
- The normal router/extraction fixture test now includes fixture corpus v10.

This hardens the most accounting-sensitive flows first. Do not meet fixture-count targets by inventing bank/SACCO wording; additional long-tail fixture depth must come from real sanitized/public evidence or value variants of already-supported wording structures.


## Phase 6 Long-tail Coverage — increment 18 / Tier-A bank fixture depth — 2026-10-02

Phase 6 corpus quality now extends the 3-fixture regression floor beyond the core wallet/debt ambiguity set.

Implemented:

- Added `fixtures-v11.json` with **33 sanitized variants**.
- The 17 currently supported Co-op / DTB / Equity / I&M / KCB / Standard Chartered bank shapes now each have at least three golden fixtures.
- Added a new CI corpus-health assertion for those Tier-A bank templates.
- Router/extraction fixture tests now load fixture corpus v11.
- New examples vary only sanitized values such as amount, date, reference, masked account and placeholder counterparties while preserving already-supported wording structure.
- No new bank message wording, transaction semantics or provider-specific Kotlin parser branch is introduced.

This implements the blueprint's recommended 3-10 fixture depth more broadly without fabricating unsupported bank messages.

Continue Phase 6 from concrete Coverage-by-source / Message Lab evidence for any new institution or notification shape.


## Phase 6 Long-tail Coverage — increment 19 / adjacent-finance fixture depth — 2026-10-02

Phase 6 fixture-depth hardening now covers the adjacent-finance shapes with guarded debt/investment/savings semantics.

Implemented:

- Added `fixtures-v12.json` with **33 sanitized variants**.
- 17 supported M-Shwari extended-product, Etica, Sanlam, Branch, Tala, Timiza, Zenka, OKash and Hustler Fund templates now have at least three golden fixtures.
- Router/extraction regression tests load fixture corpus v12.
- Added a CI corpus-health floor for these adjacent-finance templates.
- Variants change only sanitized amounts, fees, dates, references, balances, account labels, unit amounts and placeholder counterparties while preserving already-supported wording.
- No new provider wording or parser semantics were introduced.

This continues the blueprint's recommended 3-10 fixture depth without fabricating unsupported long-tail messages.


## Phase 6 Long-tail Coverage — increment 20 / full corpus fixture floor — 2026-10-02

The active Kenya Message Pack now has a universal minimum fixture-depth baseline.

Implemented:

- Added `fixtures-v13.json` with **36 sanitized variants**.
- Completed three-fixture coverage for the final 18 one-example templates.
- All **62 active Kenya Message Pack templates** now have at least three golden fixtures.
- Added a universal CI corpus-health assertion: every active template must remain at or above the three-fixture floor.
- Router/extraction regression tests now load fixture corpus v13.
- Existing focused core, Tier-A bank and adjacent-finance fixture-depth checks remain.
- No new provider wording, semantic bridge or parser branch was introduced.

Future new templates must therefore arrive with enough sanitized fixture depth to keep the full-corpus floor green. New wording still requires concrete public/sanitized device evidence rather than product-page assumptions.


## Phase 6 Long-tail Coverage — increment 21 / Brain v3 acceptance gates — 2026-10-02

Phase 6 now has explicit CI acceptance thresholds derived from the redesign blueprint.

Implemented:

- Added `BrainV3AcceptanceGateTest`.
- Every supported Message Pack fixture is evaluated through the financial relevance decision boundary.
- Deterministic template recognition or generic financial relevance counts as correct financial acceptance.
- Supported-corpus financial acceptance must remain >=95%.
- Canonical NON_FINANCIAL controls must remain rejected.
- Every fixture-declared structured field is measured against extracted output.
- Aggregate declared-field extraction accuracy must remain >=90%.
- Existing strict per-fixture router/extraction tests remain in place, so the percentage gates do not weaken individual regression protection.

The current active Kenya pack remains 62 templates with a universal three-fixture minimum.

Future Phase 6 corpus growth must come from concrete Coverage-by-source / Message Lab evidence rather than speculative provider wording.


## Phase 6 Long-tail Coverage — increment 22 / duplicate-accounting safety gate — 2026-10-02

Phase 6 now has an explicit duplicate/mirror accounting acceptance gate.

Implemented:

- Added `DuplicateAccountingAcceptanceGateTest`.
- Known high-evidence mirror fixtures must all auto-link.
- Covered strong evidence includes shared external reference, same transaction code, complementary account path, shared identity, complementary direction, and coded/codeless very-close copies.
- Known distinct fixtures must never auto-link.
- False-link guardrails cover amount conflicts, different transaction codes, stale/far-apart observations, reversals, same-source codeless timing, and weak codeless cross-source similarity.
- Ambiguous codeless cross-source observations are required to remain review candidates through `PossibleDuplicatePolicy` while staying non-auto-linked.

This makes the blueprint's near-zero known duplicate-accounting regression goal executable in CI without changing the production matcher.

Readiness consolidation in the same increment:

- Brain v3 relevance accuracy now uses labeled Foundation + canonical financial corpora.
- Every supported Message Pack fixture must have deterministic expected-template provenance and a human-readable reason.
- Canonical financial fixtures must have zero legacy-parser dependency.
- The overlapping standalone readiness PR was intentionally superseded by this consolidated gate.


## Phase 6 Long-tail Coverage — increment 23 / structural completion — 2026-10-02

Phase 6 now closes the remaining static Smart Rule explainability/reversibility gap.

Implemented:

- Added pure `SmartRuleDecisionExplanation`.
- Transaction Details names the applied Smart Rule and explains the class of condition that matched.
- Explanations deliberately do not echo stored phone numbers or saved message phrases.
- Added RuleStore lookup by ID.
- Added durable `setEnabled(...)` support.
- Applied enabled rules expose **Disable rule for future** from Transaction Details.
- Disabling a rule leaves the current transaction classification/provenance intact and prevents future application.
- Disabled applied rules are visibly labeled.
- Added pure privacy/explanation regression tests.

Phase 6 static acceptance baseline is now structurally complete:
- 62 active Kenya templates;
- >=3 sanitized fixtures per template;
- source-aware SMS/app-notification coverage loop;
- bank/SACCO/lender/investment long-tail coverage;
- recurring-payment suggestion intelligence;
- >=95% relevance and >=90% structured extraction gates;
- zero canonical legacy-parser dependency;
- explainable template provenance;
- duplicate-accounting safety gate;
- reversible personal learning and Smart Rule automation.

The only blueprint target not certifiable from static CI is the real-user outcome target of <~5% ordinary daily transactions needing category review after a month. Measure that from actual local Data Health after sufficient usage; do not fake it with synthetic fixtures.

Future corpus additions remain evidence-driven via Coverage by source / Message Lab.

## Post-Phase-6 outcome validation — 30-day review burden — 2026-10-02

The six-phase Brain v3 build order is structurally complete. The next implementation step is not a speculative Phase 7 or template-count expansion; it measures the one blueprint outcome that static CI cannot certify.

Implemented:

- Added pure `ReviewBurdenAnalysis`.
- Added a rolling 30-day outcome snapshot over real local ledger history.
- The denominator is ordinary spending only.
- The manual-review numerator includes transactions still in `NEEDS_REVIEW` plus transactions that required explicit `USER_CLASSIFIED` handling, so completed reviews do not disappear from the burden measurement.
- Transfers, income, debt, savings, investments, reversals, excluded rows, salary-cycle candidates and unresolved duplicate pairs are excluded from this category-review KPI.
- Data Health now shows ordinary-spending count, manual-review count, eligible-history days and the resulting percentage.
- The blueprint's "<~5% after a month" target stays explicitly provisional until 30 days of eligible history exist.
- A short sample can never be shown as a completed success target.
- Added pure regression tests for numerator behavior, structural exclusions, duplicate/reversal exclusions, rolling-window behavior and 30-day maturity.

This is measurement only. It does not change parsing, Identity Graph authority, Smart Rules, personal learning, category semantics, ledger arithmetic or Message Pack routing.

Use the metric with real device history. If the mature rate remains above the blueprint target, inspect the actual Needs Attention / Identity / Message Lab evidence causing user intervention before changing parsing or learning behavior.

## Real-device accuracy pass — unsupported noise + debt balance reconciliation — 2026-10-02

Real-device feedback after the six-phase Brain v3 build exposed two product-quality gaps that static corpus gates did not catch.

Implemented:

- Added a stricter unsupported-message retention boundary for Message Lab / "Not understood".
- The broad financial relevance gate remains intentionally recall-friendly for interpretation, but weak one-signal matches such as isolated `credit`, `loan`, `balance` or investment language no longer become persistent unsupported-financial diagnostics without corroborating evidence.
- Known/recognized and semantically understood messages remain diagnosable.
- Historical import still locally inspects SMS in the selected window; that is necessary to decide relevance without a brittle sender whitelist. Irrelevant messages are not promoted into the financial ledger or unsupported-learning pool.
- Liability balance observations now propagate from the generic financial interpretation path into `DebtStore`, so a later observed loan balance can update the debt UI.
- Debt repayments now honor an explicit `outstandingAfterMinor` as an observed balance instead of always estimating by subtraction.
- Generic partial-repayment grammar now carries a matching explicit outstanding balance when the same message states it.
- Added regression tests for unsupported-message false-positive suppression and partial repayments with a stated remaining balance.

This pass is provider-neutral. Do not fix future real-device gaps by adding provider-specific branches unless the financial mechanics genuinely require provider metadata. Prefer message-shape evidence, generic semantics and durable account identity.

## Real-device M-Shwari repayment shape — 2026-10-03

Added a real-device M-Shwari repayment message shape based on sanitized user-provided wording:

`<CODE> Confirmed. Loan of Ksh<AMOUNT> repaid from M-PESA on <DATE> at <TIME>. Loan balance is Ksh<OUTSTANDING>. Transaction cost Kshs <FEE>`

Implemented:

- new Message Pack template `ke.mshwari.loan.repaid-from-mpesa`;
- stable product identity `NCBA_MSHWARI_LOAN`;
- variable date/time extraction rather than hard-coding the observed date;
- structured extraction for transaction code, repayment amount, M-Pesa funding source, date/time, outstanding loan balance and transaction cost;
- Message Pack -> debt-repayment bridge so the generic phrase `Loan balance` resolves to M-Shwari Loan instead of an anonymous liability;
- outstanding `0.00` marks the debt fully repaid; a non-zero observed balance remains authoritative;
- three sanitized value/date variants in `fixtures-v14.json`;
- Message Pack active-template baseline is now 63, all with >=3 golden fixtures;
- regression coverage confirms the repayment amount, M-Shwari liability identity, M-Pesa funding account, zero balance, transaction code and variable occurred-at date.

The user's real transaction code was not stored in repository fixtures; regression data uses sanitized synthetic codes.

## Data Health financial-history reprocessing — 2026-10-03

Real-device validation now has an explicit recovery path for data that was originally interpreted by an older parser build.

Implemented:

- Data Health can reprocess the last 1, 2 or 3 months of SMS already on the device.
- Reprocessing uses the same current Financial Intelligence / Message Pack ingestion path rather than a second parser.
- Existing ledger/event dedupe remains authoritative, so replay is non-destructive and can add newly understood records without cloning already-known movements.
- Added a reconciliation-only diagnostic mode. Replay does not append duplicate Data Health observations and does not inflate Message Lab occurrence counts.
- Reprocessing can retire old Message Lab shapes that are now understood or no longer qualify as a supported financial candidate under the current relevance policy.
- Historical import now reports SMS scanned, current-brain understanding, newly inserted ledger/events, preserved duplicates, refreshed existing state and pending review events.
- Debt accounts are snapshotted before and after replay. The reconciliation report shows changed liability balances and total outstanding before/after.
- Data Health can request READ_SMS directly when the permission is missing.
- Reprocessing respects observed debt timestamps: older balance messages cannot overwrite a newer observed balance.
- Added pure regression coverage for the 1–3 month reprocess window and debt-state diff analysis.

Recommended real-device validation after installing a build containing this change:

1. Open More -> Data Health.
2. Choose 1 month first and run Reprocess financial history.
3. Confirm the recent M-Shwari repayment resolves the M-Shwari Loan balance to the stated observed balance (0.00 for the supplied full-repayment example).
4. Review the reconciliation report and the remaining Message Lab shapes.
5. Expand to 2 or 3 months only if older financial history needs repair.

Do not treat a large SMS scanned count as a failure. The inbox is inspected locally to determine relevance without a brittle sender whitelist; only current financial interpretations affect financial state.

## Message Lab user triage — financial vs non-financial — 2026-10-03

Real-device validation showed that the remaining Message Lab needs an explicit user feedback loop rather than a passive unsupported-message list.

Implemented:

- Each unsupported redacted Message Lab shape can now be marked **Financial** or **Not financial**.
- Decisions are stored locally by stable message-shape identity.
- **Not financial** suppresses that unrecognized shape from future unsupported/Not understood retention.
- User-ignored non-financial observations are removed from active financial coverage/source metrics instead of being counted as parser failures.
- Ignored shapes remain locally reviewable in Data Health with **Undo decision**; they are not destructively deleted.
- **Financial** preserves a shape as confirmed evidence even if a later heuristic-only relevance pass would otherwise consider it too weak.
- Confirmed-financial fixtures export with explicit user-confirmation metadata.
- The user decision affects only the unsupported diagnostic boundary. It does not bypass or block the actual financial interpreter/ledger.
- If a later parser genuinely recognizes a previously ignored shape, the stronger current interpretation remains authoritative and can retire the old Message Lab triage state.
- Added a distinct diagnostic state for user-ignored non-financial observations so "resolved by coverage" is not falsely inflated.
- Added pure policy regression tests for default heuristic behavior, confirmed-financial retention, non-financial suppression, undo-compatible behavior, and stronger interpretation authority.

Recommended workflow:

1. Reprocess 1 month from Data Health.
2. Review only the remaining Message Lab shapes.
3. Mark obvious OTP/promotional/service/non-money wording as **Not financial**.
4. Mark genuinely financial but unsupported wording as **Financial**.
5. Use the confirmed financial shapes as the next evidence-driven Message Pack / generic-grammar corpus.

This keeps the broad interpreter recall-friendly while letting the user's real device teach PesaTrack which unsupported shapes are noise.

## Message Lab confirmed-financial evidence queue — 2026-10-03

The real-device feedback loop now prioritizes unsupported shapes that the user explicitly confirms are financial.

Implemented:

- Added deterministic Message Lab evidence prioritization.
- Confirmed-financial shapes rank ahead of unconfirmed unsupported shapes.
- Within the same decision class, higher occurrence count ranks first, then newer last-seen time.
- Data Health now loads the prioritized unsupported queue rather than a purely chronological list.
- Data Health shows a dedicated **Confirmed financial evidence** card when confirmed unsupported shapes exist.
- All confirmed financial shapes can be copied as one sanitized JSON fixture bundle for parser/Message Pack work.
- Exported evidence includes the redacted shape, source context, occurrence count and explicit user-confirmed-financial flag.
- Source-specific fixture export remains available.
- Added pure ranking/filter regression tests.

This does not auto-create parser templates. Confirmed evidence is a prioritization signal only; implementation still requires reviewing the actual redacted financial mechanics before adding Message Pack or generic grammar coverage.

## Message Lab relevance explanations — 2026-10-03

The unsupported-message workflow now explains why each shape was admitted into Message Lab instead of showing only "Not understood".

Implemented:

- Message Lab stores the local financial relevance score and the exact relevance signals present when an unsupported shape is observed.
- Signals include currency amount, financial action, account context, debt context, investment context, financial confirmation, purchase context, and any detected security/promotion/solicitation/telecom-noise evidence.
- Data Health now shows a short human-readable **Why PesaTrack flagged this** explanation under each unsupported redacted shape.
- User-confirmed financial and user-ignored non-financial shapes get explicit explanations of the user's decision.
- Older shapes that predate relevance evidence display a prompt to reprocess recent history rather than inventing an explanation.
- Reconciliation-only history replay refreshes relevance evidence for existing Message Lab shapes without incrementing occurrence counts.
- Relevance evidence is included in sanitized developer fixture exports, making confirmed-financial bundles more useful for future parser work.
- Stable shape identity is now computed before the unsupported decision so an existing user decision remains addressable even if the heuristic score changes in a later build.
- User triage still affects only the unsupported diagnostic layer; recognized financial semantics remain authoritative.
- Added pure explanation regression tests.

Recommended real-device workflow remains: reprocess one month, inspect the remaining Message Lab items, use the explanation to mark obvious noise as Not financial, confirm genuine misses as Financial, then use the confirmed bundle as the next parser corpus.

## Message Lab semantic evidence labels — 2026-10-03

The confirmed-financial evidence queue can now capture what an unsupported financial message actually means, without turning the label into automatic accounting.

Implemented:

- Confirmed financial Message Lab shapes can be labeled as:
  - Payment / spending
  - Money received
  - Transfer
  - Cash withdrawal
  - Cash deposit
  - Loan received
  - Loan repayment
  - Balance update
  - Savings / investment
  - Other financial
- Semantic labels are stored locally by stable message-shape identity.
- Only shapes explicitly confirmed as financial can receive a semantic label.
- Choosing the same label again clears it.
- Clearing the overall Financial/Not financial decision also clears the semantic label because the shape decision record is removed.
- Confirmed evidence with a semantic label ranks ahead of unlabeled confirmed evidence in the parser-work queue.
- Confirmed-financial JSON exports now include both the stable semantic label and its display label.
- Data Health shows how many confirmed financial shapes already have a meaning label.
- The UI explicitly states that this label is evidence for future parser work only; it does not create, edit or classify ledger transactions.
- Added queue regression coverage so meaning-labeled confirmed evidence remains highest priority.

This is intentionally an evidence boundary, not a parser shortcut. Future parser/Message Pack work should use the redacted wording + relevance evidence + user-confirmed financial status + semantic label together before adding production extraction semantics.

## Message Lab parser readiness — 2026-10-03

The evidence loop now distinguishes a merely unsupported shape from a parser-work candidate with enough user-confirmed context to review next.

Implemented:

- Added explicit parser-readiness states:
  - needs financial confirmation;
  - needs meaning label;
  - labeled single observation;
  - evidence ready.
- A shape is called **evidence ready** only when it is:
  - explicitly confirmed financial by the user;
  - given a semantic meaning label;
  - observed at least twice as the same stable redacted shape.
- The repeated-observation floor is intentionally small and is not treated as permission to auto-create a production parser.
- Data Health now shows how many confirmed shapes still need a meaning label and how many are evidence ready.
- Each confirmed shape shows its current readiness state.
- Evidence-ready shapes can be exported as a dedicated sanitized parser-candidate bundle.
- Exported fixtures include the existing relevance evidence, user-confirmed-financial status, semantic label and parser-readiness state.
- Added pure regression coverage for each readiness state, aggregate readiness counts and filtering of evidence-ready candidates.

Guardrail:

Parser readiness is a development prioritization signal only. It does not create templates, alter ledger rows, infer amounts/directions, or bypass Message Pack/generic grammar review. Production support still requires implementing and testing the actual financial mechanics represented by the redacted wording.

## Data Health local Message Inspector — current parser-readiness baseline — 2026-10-03

The real-device evidence loop now includes a one-message diagnostic tool on top of the current semantic-label and parser-readiness implementation.

Implemented:

- Added **Data Health -> Inspect one message**.
- A user can paste an SMS body and optional sender/title and run it through the exact current Financial Intelligence + Message Pack stack.
- Inspection is local and ephemeral; this tool does not persist the raw pasted text.
- The result shows whether the message is recognized, semantically understood, unsupported financial evidence, or outside the unsupported-financial boundary.
- The UI exposes parse route, matching template/revision, relevance score/signals, extracted field names, transaction code/date when present, interpreted event summaries, observed balances and semantic intent hints.
- The inspector explicitly reports whether the current build would still place the message in Message Lab.
- Added **Copy sanitized diagnostic**. The export uses the same local redaction boundary and intentionally omits raw transaction values.
- The sanitized exporter is pure Kotlin so privacy assertions run in local JVM tests rather than depending on Android JSON stubs.
- Added regression coverage using the real-device M-Shwari repayment shape, weak loan-marketing language and sanitized-export privacy assertions.

Recommended use:

1. Reprocess one month first so stored state is current.
2. For any remaining suspicious or Not understood SMS, paste one copy into Message Inspector.
3. If it says **Recognized** but app state is still wrong, investigate reconciliation/account identity rather than adding another parser template.
4. If it says **Not understood** and would enter Message Lab, use the sanitized diagnostic plus the Financial/Not financial, semantic-label and parser-readiness workflow as concrete evidence.
5. If it says **Not financial / ignored**, do not add provider-specific parsing without stronger evidence.

This tool is diagnostic only. It does not write transactions, balances, Message Lab decisions, semantic labels or parser-readiness state.

## Message Lab review-only parser drafts — 2026-10-03

The real-device evidence loop now turns an evidence-ready unsupported shape into a structured parser draft without activating production parsing.

Implemented:

- Added `MessageLabParserDraftGenerator`.
- Draft generation is available only for `EVIDENCE_READY` shapes: user-confirmed financial, meaning-labeled, and observed at least twice.
- Drafts propose:
  - a stable draft ID;
  - sender/source context;
  - the user-confirmed semantic label;
  - a suggested Message Pack event kind using the existing repository vocabulary where possible;
  - literal candidate anchors extracted from the redacted shape;
  - placeholder field candidates such as `TRANSACTION_CODE`, `AMOUNT`, `BALANCE`, `OUTSTANDING`, `FEE`, `COUNTERPARTY`, `SOURCE`, `DESTINATION`, `DATE`, and `TIME`;
  - the redacted example and occurrence count;
  - explicit manual-review warnings.
- Field-role inference uses the nearest redacted placeholder context rather than provider-specific rules.
- Drafts are always `active: false` and `requiresManualReview: true`.
- Drafts intentionally do **not** generate production extraction regexes or product identities.
- Data Health now exposes:
  - **Copy parser draft bundle** for all evidence-ready shapes;
  - **Copy review-only parser draft** on each evidence-ready Message Lab item.
- Added pure regression coverage using the M-Shwari repayment redacted shape, transfer destination context, readiness gating, and inactive/review-only output.

Guardrail:

A parser draft is a development artifact only. It must never be loaded as an active Message Pack template. A developer still needs to review the financial mechanics, choose product identity/account semantics, write extraction regexes, add at least three sanitized golden fixtures, and pass the existing relevance/extraction/duplicate-accounting gates before production activation.

## Message Lab implementation review gate — parser-draft baseline — 2026-10-03

The real-device evidence workflow now separates **evidence ready**, **review-only parser draft**, and **approved for parser implementation**.

Final flow:

`confirmed financial -> meaning labeled -> repeated evidence -> review-only draft -> implementation review -> parser work`

Implemented:

- Added implementation-review states for evidence-ready shapes:
  - **Needs more examples**
  - **Ready for parser work**
- Review decisions are allowed only when the shape is already `EVIDENCE_READY`.
- Review-only parser drafts remain available before approval, so the developer can inspect proposed anchors/field roles first.
- Changing or clearing the semantic meaning automatically clears any previous implementation review.
- Clearing the overall Message Lab decision removes the implementation review with the local shape decision record.
- Data Health distinguishes:
  - evidence-ready;
  - awaiting implementation review;
  - needs more examples;
  - ready for parser work.
- Existing evidence-ready and review-only draft exports remain available.
- Added a stricter **implementation-ready bundle** containing only shapes explicitly marked Ready for parser work.
- Developer fixture exports include `implementationReview` and `readyForParserWork`.
- The evidence queue prioritizes implementation-approved candidates ahead of unreviewed candidates, while candidates explicitly deferred for more examples are ranked lower.
- Added pure regression tests for review eligibility, review counts, stale-review rejection, and queue ordering.

Guardrail:

Neither an evidence-ready shape nor an inactive parser draft is equivalent to production support. Even **Ready for parser work** is a development decision only. A production parser still requires reviewed mechanics, product/account identity decisions, extraction regexes or generic grammar, at least three sanitized golden fixtures where applicable, and the normal relevance/extraction/duplicate-accounting/reconciliation gates.

## Message Inspector privacy + source-decision correction — current implementation-review baseline — 2026-10-03

Follow-up review of the local Message Inspector found two important edge cases. This corrective pass keeps the diagnostic tool useful without weakening privacy or borrowing a Message Lab decision from the wrong ingestion channel.

Implemented:

- Sanitized export now redacts **generic Financial Intelligence evidence spans**, not only values caught by the generic redactor.
- Generic fallback amounts such as an unqualified `575.00` balance are replaced using the interpreter's own `FieldEvidence` span before export.
- Balance, event and metadata evidence are all included in the redaction pass.
- Overlapping evidence spans are merged before replacement so repeated/overlapping evidence cannot corrupt offsets or expose part of a value.
- Message Pack extracted values receive a second redaction layer after normalized generic-evidence redaction.
- Sanitized export always emits `<SENDER>`; a pasted phone number or arbitrary sender string is never copied.
- The inspector can use an existing Message Lab Financial / Not financial decision only when the stored sender/body match resolves to exactly one source-specific shape.
- If identical redacted wording exists in more than one source channel, the saved decision is treated as ambiguous and is **not** borrowed across channels.
- The UI explains when a saved decision is ambiguous and shows an unambiguous saved Financial/Not financial choice and semantic meaning when available.
- `wouldEnterMessageLab` uses the same `MessageLabDecisionPolicy` as real ingestion whenever an unambiguous saved decision exists.
- Added regression tests for generic-fallback balance privacy, raw sender privacy, confirmed-financial retention, ambiguous-source decision isolation and the pure source-match resolver.

This remains diagnostic only. It does not create or edit transactions, debt balances, Message Lab decisions, semantic labels, implementation-review state or parser-readiness state.

## Message Lab conservative shape families — current privacy + implementation-review baseline — 2026-10-03

Real-device evidence now groups small presentation variants without changing or replacing exact Message Lab shape identities.

Implemented:

- Added a conservative `familyId` alongside the existing exact `shapeId`.
- Exact shape identity remains authoritative for user Financial / Not financial decisions and semantic meaning labels.
- Family normalization covers only low-risk presentation variation:
  - case;
  - punctuation;
  - whitespace;
  - common currency spellings such as KES / Ksh / Kshs / Sh;
  - M-PESA / M PESA, M-Shwari / M Shwari and PesaLink spacing/hyphen variants.
- Source kind, source package and normalized sender remain part of the family fingerprint, so unrelated institutions/channels do not collapse together.
- Older stored shapes without a family ID receive a deterministic family ID at read time; no destructive migration is required.
- Message Lab now computes exact-shape occurrence count, family occurrence count and family wording-variant count.
- Shapes explicitly marked **Not financial** do not contribute recurrence evidence to a financial family.
- Parser readiness can satisfy the repeated-evidence floor from conservative family recurrence while still requiring the exact reviewed shape to be user-confirmed financial and meaning-labeled.
- Evidence-ready parser work is deduplicated by **family + semantic meaning**.
- Review-only parser drafts include family ID, family recurrence and variant count.
- The implementation-review gate is also family-aware:
  - one family + meaning is counted as one parser task;
  - a Ready for parser work decision prevents duplicate sibling tasks;
  - a Needs more examples decision does not reappear as an unreviewed sibling task.
- Evidence ranking uses family-supported recurrence while preserving the stronger implementation-review priority.
- Data Health shows family recurrence when multiple wording variants support the same conservative family.
- Developer fixture exports include family evidence.
- Added regression coverage for family equivalence/separation, family-supported readiness, family ranking, parser-draft family evidence and implementation-review deduplication.

Guardrail:

Shape families are evidence grouping only. They never copy Financial / Not financial decisions or semantic labels to sibling variants, never alter ledger state, and never activate parsers. Material wording changes still produce different families.

## Provider-neutral bank debit fallback — 2026-10-03

Real-device Message Lab work exposed a generic banking gap: PesaTrack could understand an account debit when a beneficiary/counterparty was extractable, but explicit bank-account debit messages without a beneficiary could remain unsupported.

Implemented:

- Added a provider-neutral explicit account-debit candidate for wording such as an owned `a/c`, `acct` or `account` being debited by a KES/Ksh amount.
- Branded account wording such as `Your <bank> a/c ...` is accepted without adding a bank-specific parser branch.
- If a beneficiary/counterparty is present, the existing payment relationship remains authoritative.
- If the message proves an owned bank account was debited but does not identify who received the money, PesaTrack now emits a medium-confidence **unresolved movement** tied to the observed owned account.
- The fallback deliberately does **not** invent a merchant/person or convert the movement into a categorized expense.
- Masked account references are preserved as evidence when present.
- Existing balance extraction remains independent, so an available/current balance can still reconcile the observed bank account.
- Added regression coverage for:
  - branded Equity-style account wording without a counterparty;
  - beneficiary-present precedence;
  - generic unknown-bank account debit wording.

Guardrail:

This is generic financial grammar, not an Equity-specific template. Exact Message Pack templates should still be added only from real sanitized wording evidence. The fallback is intended to move explicit bank debits from **Not understood** into a safe reviewable financial movement without fabricating transaction meaning.

## Unresolved debit direction preservation — 2026-10-03

Follow-up review of the provider-neutral bank debit fallback found a legacy-storage mismatch: explicit account-debit wording was semantically outgoing, but unresolved canonical movements were still persisted through the legacy `OTHER` type, whose default direction is neutral.

Implemented:

- Added a semantic direction hint to unresolved financial movements.
- Explicit account-debit fallback now carries `OUTGOING`.
- Generic unresolved movements keep no direction hint.
- The canonical unresolved draft preserves the hint.
- The ingestion coordinator maps:
  - `OUTGOING` -> expense direction;
  - `INCOMING` -> income direction;
  - no hint -> existing neutral behavior.
- The legacy transaction-storage bridge accepts an optional direction override.
- Smart Rules still outrank the semantic hint.
- Own-account-transfer handling still outranks the override.
- Added regression coverage proving explicit bank debits are outgoing while generic unresolved messages do not invent a direction.

Guardrail:

A direction hint says only which way money moved. It does not invent a counterparty, category, transfer destination, or merchant identity. Explicit debit-without-beneficiary rows remain Needs Review until further evidence or a user rule resolves their meaning.

## Provider-neutral bank credit fallback without counterparty — 2026-10-03

Follow-up to the explicit bank-debit fallback adds the safe incoming mirror for bank messages that prove an owned account was credited but do not identify who sent the money.

Implemented:

- Added a provider-neutral explicit account-credit candidate for wording such as:
  - `Your <bank> a/c ... has been credited with KES <amount>`;
  - `KES <amount> has been credited to your account ...`.
- The fallback resolves the observed destination as the user's owned bank account from the message source / balance context.
- The resulting unresolved movement carries the semantic direction hint `INCOMING`.
- Existing receipt wording with an explicit `from/by <counterparty>` still wins and produces the normal higher-confidence Receipt event.
- Debt, reversal, cash, transfer and other already-known relationships still outrank this fallback.
- The fallback does not invent a sender, employer, merchant or transfer source.
- Account reference evidence is preserved when present.
- Existing balance extraction remains independent, so the same message can update the observed account balance.
- Added regression coverage for branded bank credit, amount-first generic bank credit, and counterparty-present precedence.

Guardrail:

Incoming direction is the only asserted movement meaning. An account credit without a counterparty remains unresolved / Needs Review; it is not automatically called salary, transfer, refund or any other income category without additional evidence.

## Provider-neutral card/POS purchase + ATM withdrawal fallback — 2026-10-03

Real-device banking coverage now handles common card/POS and ATM wording that does not explicitly say an account was debited.

Implemented:

- Added generic card/POS purchase recognition for wording such as:
  - `Purchase of KES <amount> at <merchant> using your card ...`;
  - `Your card ending <digits> was used for KES <amount> at <merchant>`;
  - `KES <amount> card purchase at <merchant>`.
- Card purchases reuse the existing payment relationship and produce merchant-route payments; no provider-specific parser branch is added.
- Existing fee extraction remains authoritative, so a transaction fee in the same message is preserved on the payment.
- Explicit card/POS/ATM wording now counts as bank-source evidence when resolving an owned message-source account.
- Added generic ATM withdrawal recognition for explicit cash-withdrawal wording tied to the user's account, with optional ATM/location counterparty.
- ATM withdrawals reuse the existing cash-movement relationship rather than becoming generic expenses.
- Added a shared non-posted movement guard. Wording such as **declined**, **failed**, **unsuccessful**, **rejected**, **pending**, or **not successful** does not create card/ATM ledger events.
- Added regression coverage for:
  - successful card purchase;
  - card purchase with transaction fee;
  - declined card purchase;
  - ATM cash withdrawal with observed bank balance;
  - failed ATM withdrawal.

Guardrail:

The fallback requires explicit posted-movement wording and an amount. It does not turn promotional card messages, authorization offers, declined attempts, or generic ATM references into transactions. Provider-specific Message Pack templates should still be added only from repeated real-device evidence where exact wording/mechanics justify them.

## Provider-neutral bank cash deposit fallback — 2026-10-03

The generic bank cash-movement grammar now handles explicit deposit wording that does not follow the older amount-first format.

Implemented:

- Added provider-neutral recognition for wording such as:
  - `Cash deposit of KES <amount> into your account <ref> at <branch>`;
  - `KES <amount> cash deposited into your account <ref> at <branch>`.
- The message sender/title is used as the owned bank destination when the wording proves a bank cash deposit.
- Existing cash semantics remain authoritative:
  - cash is the source asset;
  - the owned bank account is the destination;
  - branch/location is optional counterparty context;
  - an observed bank balance in the same message still reconciles independently.
- The shared non-posted movement guard applies here too, so failed, unsuccessful, rejected or pending deposit attempts do not create ledger events.
- Added regression coverage for a successful bank cash deposit with observed balance and an unsuccessful deposit attempt.

Guardrail:

This fallback requires explicit cash-deposit wording tied to the user's bank account. It does not reinterpret generic account credits as cash deposits and does not invent a depositor when none is stated.

## Safe bank balance abbreviations — 2026-10-03

Generic bank reconciliation now understands common abbreviations for spendable/current balance without collapsing distinct accounting balance concepts.

Implemented:

- The message-source balance extractor now recognizes:
  - `Available Balance`;
  - `Available Bal`;
  - `Avail Balance`;
  - `Avail Bal`;
  - `Avl Balance`;
  - `Avl Bal`;
  - `Current Balance / Current Bal`;
  - `Closing Balance / Closing Bal`;
  - the existing plain `Balance` wording.
- Optional `is` or `:` separators remain supported.
- `Avail` and `Avl` are excluded from generic named-account balance parsing so the same balance is not duplicated as a fake account named “Avail”.
- Abbreviated balance observations still resolve through the financial message source, so known banks update the correct owned bank account.
- Added regression coverage for abbreviated balances on outgoing bank debits and incoming bank credits.

Safety boundary:

- `Ledger Bal` and `Book Bal` are intentionally **not** treated as the same balance as Available/Current/Closing balance. They can differ from spendable balance and PesaTrack does not yet model balance types strongly enough to collapse them safely.
- A regression test locks this distinction so coverage expansion cannot silently overwrite the user's usable balance with a ledger/book balance.

## Plain bank balance shorthand — 2026-10-03

Follow-up to safe bank balance abbreviations adds the common plain `Bal` shorthand without collapsing distinct ledger/book balance concepts.

Implemented:

- Generic bank reconciliation now recognizes plain:
  - `Bal: KES <amount>`;
  - `Bal Ksh <amount>`;
  - case variants such as `BAL`.
- `BAL` is excluded from generic named-account labels so the shorthand cannot become a fake account identity.
- Plain `Bal` still resolves through the financial message source, matching the behavior of `Balance`, `Available Bal`, `Current Bal`, and `Closing Bal`.
- Added debit and credit regression coverage.

Safety boundary:

- `Ledger Bal` and `Book Bal` remain intentionally unsupported as usable/current balance.
- The extractor still requires the balance shorthand to appear as its own balance phrase, rather than matching arbitrary words ending in `bal`.

## Post-transaction New Bal / New Balance wording — 2026-10-03

Generic bank balance reconciliation now recognizes the common post-transaction wording `New Bal` and `New Balance`.

Implemented:

- Recognizes:
  - `New Bal KES <amount>`;
  - `New Bal: KES <amount>`;
  - `New Balance Ksh <amount>`;
  - `New Balance: Ksh <amount>`;
  - case variants.
- Uses the same owned message-source account resolution as plain `Balance`, `Bal`, Available/Avail/Avl, Current and Closing balance wording.
- Added debit and credit regression coverage.

Safety boundary:

- The change only adds `new` as a qualifier directly before `Bal` / `Balance`.
- It does not reinterpret `Ledger Bal`, `Book Bal`, loan balances, or arbitrary uses of the word `new`.

## Account-anchored DR / CR bank abbreviations — 2026-10-03

The generic bank parser now closes a relevance/interpretation mismatch for common statement-style debit and credit abbreviations.

Implemented:

- Recognizes posted debit wording such as:
  - `A/C XX1234 DR KES <amount>`;
  - `Acct XX1234 DR: KES <amount>`;
  - branded account prefixes such as `Your <bank> a/c ... DR ...`.
- Recognizes the corresponding `CR` forms as incoming account credits.
- `DR` produces the same safe unresolved outgoing movement as an explicit account debit when no counterparty is stated.
- `CR` produces the same safe unresolved incoming movement as an explicit account credit when no counterparty is stated.
- Masked account references are preserved as evidence.
- Existing balance extraction remains independent, so `Bal`, `New Bal`, Available/Current/etc. can reconcile the account in the same message.
- The shared non-posted guard blocks pending, declined, failed, rejected or unsuccessful DR/CR attempts.
- Added regression coverage for outgoing DR, incoming CR, balance reconciliation, bare-DR rejection, and pending-DR rejection.

Safety boundary:

- A bare `DR KES <amount>` or `CR KES <amount>` without explicit account context does not create a ledger movement.
- DR/CR does not invent a counterparty, merchant, transfer destination, category or salary/refund meaning.
- These movements remain unresolved / Needs Review unless stronger relationship evidence exists.

## Provider-neutral loan payment-received repayment fallback — 2026-10-03

Debt interpretation now handles lender repayment confirmations that prove both a received payment and the resulting loan balance even when they do not use the exact phrase "loan repayment".

Implemented:

- Added provider-neutral repayment wording for shapes such as:
  - `We have received your payment of KES <amount>. Your loan balance is KES <balance>.`
  - `Payment of Ksh <amount> has been received. Your loan balance is Ksh <balance>.`
- A plain sender-scoped `Your loan balance is ...` can identify the liability through the message source (for example `BRANCH` -> `Branch Loan`) without adding a Branch-specific parser branch.
- An explicit resulting balance is authoritative:
  - balance `0` -> full repayment;
  - non-zero balance -> partial repayment.
- Existing named-loan balance wording remains authoritative when the message explicitly names the loan product.
- The fallback requires explicit loan context. An ordinary service message such as `We have received your payment of KES ...` without loan wording/balance does not become debt.
- Added regression coverage for sender-scoped partial repayment, zero-balance full repayment and non-loan payment-received rejection.

Guardrail:

The sender is used only to resolve the liability when the message itself proves a loan repayment/balance relationship. The fallback does not invent a funding account, repayment source, fee, or lender-specific mechanics.

## Data Health history-reprocess freshness — 2026-10-03

Data Health now remembers whether existing SMS history has been reconciled after the currently installed app update.

Implemented:

- Added a local `FinancialHistoryReprocessStateStore`.
- After a successful 1–3 month history replay, PesaTrack records only:
  - the successful replay timestamp;
  - the selected replay window in months.
- Data Health compares the last successful replay timestamp with Android's package `lastUpdateTime`.
- The history card now shows:
  - **Recommended** when no successful replay has been recorded on this installation;
  - **Recommended** when the APK was updated after the last successful replay;
  - **Up to date** when history was reconciled after the current app update.
- A successful replay refreshes the Data Health state immediately and clears the recommendation for the current installed build.
- Added pure policy tests for first-run, app-updated-after-replay, replay-after-update and equal-timestamp cases.

Privacy / behavior boundary:

- This state contains no SMS content, transaction values, account identifiers or financial history.
- Reprocessing remains user-triggered; PesaTrack does not silently rescan the inbox in the background.
- An app update recommendation is intentionally conservative: a UI-only build can also trigger the reminder, but this is preferable to leaving old financial state on an older parser after intelligence/debt fixes.

## Provider-neutral standalone bank fee postings — 2026-10-03

Generic banking interpretation now handles explicit posted bank charges that are themselves the financial movement, instead of leaving them in Message Lab or storing them as an ambiguous generic debit.

Implemented:

- Added explicit standalone-fee recognition for posted wording where the message proves:
  - a fee/charge amount;
  - that the amount was charged, debited or deducted;
  - the charge applies to the user's bank account.
- Supported generic wording includes examples such as:
  - `Monthly service charge KES <amount> has been debited from your account ...`;
  - `Your account ... was charged KES <amount> as monthly maintenance fee`.
- Expanded fee descriptor extraction to common provider-neutral labels such as service, maintenance, account, ledger, SMS, mobile-banking, card and ATM fees/charges.
- Future/informational fee notices such as `will be charged` are explicitly non-posted and do not create ledger movements.
- Existing declined/failed/pending movement protection also applies.
- A standalone fee is resolved only when no unexplained monetary amount remains. This prevents the fee amount from stealing a message whose primary movement is still unsupported.
- Added a dedicated `ResolvedFeeRelationship` and `FeeEventFact`.
- Standalone fee events map to the existing canonical `FeeDraft`; they do not fabricate a merchant/counterparty.
- Legacy transaction persistence now accepts canonical `FEE` at the compatibility boundary.
- The ingestion bridge stores a standalone fee exactly once as:
  - direction: Expense;
  - category: Fees;
  - review state: Auto classified;
  - account: the observed owned bank account.
- Deterministic category/review overrides remain below Smart Rules and do not overwrite user-classified or excluded existing rows during reprocessing.
- The local Message Inspector now shows standalone `FEE` events.
- Added regression coverage for:
  - posted service charge;
  - account-first maintenance fee;
  - canonical fee-draft mapping;
  - future fee notice rejection;
  - informational fee rejection;
  - legacy storage compatibility.

Guardrail:

This fallback requires explicit posted-charge semantics and an owned bank-account context. A fee amount mentioned in pricing, marketing, a future notice, or another still-unexplained movement does not become a standalone expense.

## Shared non-posted guard across generic movements — 2026-10-03

Generic movement interpretation now applies the same non-posted safety boundary consistently across older and newer financial grammars.

Implemented:

- Extended the existing shared `nonPostedMovementAttemptRegex` guard to generic:
  - transfer-from;
  - transfer-to;
  - sent/payment;
  - paid-to;
  - beneficiary account-debit;
  - external transaction-credit;
  - airtime purchase;
  - receipt-from;
  - credited-from;
  - account debit without a counterparty;
  - account credit without a counterparty.
- Existing card/POS, ATM, cash movement and DR/CR guards remain unchanged.
- Pending / declined / failed / unsuccessful / rejected / not-successful / could-not-complete wording therefore cannot become a posted money-movement event merely because an older generic regex also matches.
- Balance extraction stays independent. A non-posted movement notification may still carry an observed account balance; the guard suppresses the movement, not unrelated reconciliation evidence.
- Added end-to-end regression coverage for pending generic bank debit, pending bank credit, pending beneficiary debit, pending counterparty receipt, pending account transfer and failed sent payment.

Guardrail:

The safety boundary deliberately favors missing/Needs Review over inventing a posted transaction. Reprocessing a newer build can recover a movement later if stronger posted evidence is added, while a false posted ledger entry is harder to unwind safely.

## Pending-action semantic alignment — 2026-10-03

The semantic layer now describes explicitly pending financial movements consistently with the non-posted candidate safety boundary.

Implemented:

- Added a conservative pending-action phrase detector for:
  - `pending`;
  - `awaiting processing/completion/confirmation`;
  - `being processed`;
  - `in progress`.
- When those phrases accompany a movement intent such as payment, receipt, transfer, loan drawdown/repayment, reversal/refund or fee, the semantic role is now `PENDING_ACTION` rather than `NEW_EVENT`.
- The interpreter suppresses movement events whenever the semantic role is `PENDING_ACTION`, in addition to the existing failed-action veto.
- The underlying semantic intent is preserved. For example, a pending transfer remains semantically a transfer; only its accounting role is pending.
- Independent balance observations remain available for reconciliation even when the associated movement is pending.
- Balance/information messages are not turned into pending movement actions merely because they contain wording such as `no pending transactions`.
- Existing submitted investment orders remain `PENDING_ACTION` as before.
- Added regression coverage for pending transfers, pending account credits with observed balances, and the balance-only exception.

Guardrail:

Pending status is not evidence that money moved. PesaTrack therefore keeps the semantic meaning for explanation/diagnostics but never persists the movement until posted/settled wording is observed.

## Message Inspector -> sanitized financial evidence — rebased on pending-action safety — 2026-10-03

The local Message Inspector can now turn a manually pasted unsupported financial SMS into one explicit sanitized Message Lab evidence sample without creating accounting state.

Implemented:

- When Inspector says a pasted message would still enter Message Lab, Data Health shows **Add as financial evidence**.
- The workflow stores only the normal locally redacted shape plus relevance score/signals; raw pasted SMS is not persisted by this workflow.
- Saved evidence is explicitly marked **Financial**.
- If the pasted wording already matches exactly one stored Message Lab shape, that existing shape is reused and confirmed instead of creating a duplicate.
- If the same redacted wording exists in multiple source channels, the Inspector does not copy a source-specific decision across them; it creates a separate `manual_message_inspector` shape.
- A new manual-inspector shape starts with occurrence count 1.
- Repeated taps on the same manual evidence do not increment occurrence count. Manual pasting therefore cannot manufacture the repeated-evidence threshold used by parser readiness.
- Confirming an existing ignored shape restores its unsupported diagnostic state where applicable.
- Existing semantic-label / implementation-review metadata on a reused shape is preserved.
- Manual evidence respects the same bounded Message Lab storage cap.
- Added pure policy regression tests for unique reuse, ambiguous-source separation, new manual targeting and stable occurrence counts.

Guardrail:

Manual Inspector evidence is development evidence only. It never creates a transaction, balance, debt update, parser template, Smart Rule, account ownership decision, or repeated-observation credit. Production parsing still requires the normal semantic-label, evidence-readiness, implementation-review and tested parser gates.

## Message Inspector inline semantic labeling — 2026-10-03

The manual-evidence workflow now keeps the next evidence step inside the Message Inspector instead of forcing the user to find the same shape again lower in Data Health.

Implemented:

- When an inspected unsupported message is already confirmed as financial, the Inspector shows **What does this message mean?** directly in the inspection result.
- The same semantic evidence labels used by Message Lab are available inline:
  - Payment / spending
  - Money received
  - Transfer
  - Cash withdrawal
  - Cash deposit
  - Loan received
  - Loan repayment
  - Balance update
  - Savings / investment
  - Other financial
- Saving a new manual financial-evidence sample immediately binds the Inspector to the exact stored shape ID, including the separate manual shape used when source-channel identity is ambiguous.
- Re-analyzing an already confirmed unique stored shape also restores the exact shape ID, so existing meaning can be changed or cleared inline.
- After a manual save, an earlier ambiguous inspection state is replaced by the newly created/selected exact evidence shape.
- Selecting or clearing meaning refreshes the prioritized Message Lab queue and the Inspector's saved-meaning display immediately.
- Editing sender/body or clearing the Inspector clears the bound stored-shape target so a label cannot accidentally be written to stale evidence.

Guardrail:

Inline meaning remains evidence only. It does not create, edit, categorize, or reconcile ledger transactions. Changing meaning also keeps the existing Message Lab rule that clears stale implementation-review approval until the updated evidence is reviewed again.

## Message Inspector inline parser readiness + implementation review — 2026-10-03

The Inspector now shows the rest of the existing evidence pipeline after Financial confirmation and semantic meaning, without making the user find the same Message Lab card again.

Implemented:

- Inspector binds to the exact stored evidence shape already used for inline semantic labeling.
- After a meaning label exists, Inspector shows the existing parser-readiness state:
  - needs financial confirmation;
  - needs meaning;
  - need another real example;
  - evidence ready.
- The readiness calculation reuses `MessageLabParserReadiness.stateFor` exactly; no Inspector-specific threshold was added.
- Inspector shows exact occurrence count and family-supported recurrence when applicable.
- A single manually pasted sample remains a single observation and explicitly tells the user that another real occurrence is needed.
- When evidence is ready, Inspector exposes the existing implementation-review choices:
  - **Needs more examples**
  - **Ready for parser work**
- Review selection/clearing goes through the existing `MessageLabStore.setImplementationReview` / `clearImplementationReview` path.
- Semantic meaning changes continue to clear stale implementation review through the existing store rule.
- The prioritized Message Lab queue refreshes immediately after review changes, so Inspector and Message Lab remain consistent.

Guardrail:

Readiness and implementation review remain development-state signals only. They do not activate a parser, create extraction regexes, write ledger rows, change balances/debt, infer ownership, or bypass Message Pack/generic-grammar tests.

## Message Lab parser implementation work packets — 2026-10-04

The evidence workflow now produces a complete developer handoff only after a shape family is explicitly approved **Ready for parser work**.

Implemented:

- Added `MessageLabImplementationPacketGenerator`.
- Work packets are generated only from implementation-reviewed, evidence-ready families.
- Each packet contains:
  - family and representative shape identity;
  - user-confirmed semantic meaning;
  - family occurrence and wording-variant counts;
  - the existing review-only parser draft;
  - every active sanitized family variant, including exact occurrence count, decision, meaning, relevance score and relevance signals;
  - a generic implementation checklist;
  - a semantic-specific validation checklist;
  - permanent parser/accounting/privacy guardrails.
- Ignored non-financial variants are excluded from implementation packets.
- The packet does not generate active regexes, product identities, ownership, ledger rows or parser activation.
- Semantic-specific checks cover debt drawdown/repayment, transfers, payments, receipts, balances, savings/investments and cash movements.
- Common validation requirements include:
  - positive family-variant coverage;
  - structured extraction;
  - negative informational/promotional wording;
  - pending/declined/failed suppression;
  - duplicate/mirror accounting;
  - history reprocessing idempotency;
  - Message Inspector provenance.
- Data Health now exposes **Copy implementation work packets** beside the existing thinner implementation-ready fixture export.
- Added regression tests proving:
  - unapproved evidence cannot produce a packet;
  - approved packets include all active family variants but exclude ignored noise;
  - packets remain explicitly manual/non-active;
  - one representative is emitted per reviewed family+meaning while sibling variants remain inside the packet;
  - transfer and debt packets carry the appropriate safety validation.

Recommended developer flow:

`real device evidence -> confirm financial -> label meaning -> repeated family evidence -> review-only draft -> Ready for parser work -> implementation work packet -> implement/test parser -> reprocess -> verify device state`

Guardrail:

**Ready for parser work is still not production approval.** The implementation packet is a handoff artifact. Production support still requires reviewed financial mechanics, deterministic extraction/generic grammar, product/account identity only where proven, >=3 sanitized golden fixtures where applicable, and the existing relevance/extraction/duplicate-accounting/reconciliation gates.

## Message Inspector inline parser handoff — 2026-10-04

The Inspector now completes the full evidence-to-developer flow without forcing the user to find the same shape again lower in Message Lab.

Implemented:

- When the bound Inspector evidence reaches `EVIDENCE_READY`, the Inspector exposes **Copy review-only parser draft** directly beside the implementation-review controls.
- When the same shape/family is explicitly marked **Ready for parser work**, the Inspector additionally exposes **Copy implementation work packet**.
- The implementation packet uses the same family-aware generator as the Data Health bundle and includes:
  - the approved representative;
  - active sanitized family variants;
  - parser draft;
  - implementation checklist;
  - validation checklist;
  - permanent parser/accounting/privacy guardrails.
- Added a pure `MessageInspectorParserHandoffPolicy` so developer actions are gated by the same readiness/review rules rather than UI-only conditions.
- Added regression coverage for:
  - no saved evidence;
  - single-observation evidence;
  - evidence-ready but unreviewed evidence;
  - Ready for parser work;
  - Needs more examples.

Final inline Inspector path:

`paste/inspect -> save sanitized financial evidence -> label meaning -> gather repeated evidence -> review implementation -> copy draft/work packet`

Guardrail:

The Inspector never exposes an implementation work packet before both evidence readiness and explicit **Ready for parser work** review are satisfied. Copying a draft or work packet is still a development handoff only and does not activate parsing or write accounting state.

## Message Lab sanitized resolution archive — 2026-10-04

The real-device parser feedback loop now preserves proof when previously unsupported evidence becomes understood instead of silently deleting that history.

Implemented:

- Added a bounded local archive of the latest 50 resolved Message Lab shapes.
- Before an active unsupported shape is retired, PesaTrack preserves only its existing sanitized evidence:
  - exact shape ID and conservative family ID;
  - safe sender/source channel metadata;
  - redacted body;
  - exact/family occurrence counts;
  - prior Financial/Not-financial decision where applicable;
  - semantic meaning label;
  - implementation-review state.
- Resolution provenance is recorded from the current interpretation:
  - `TEMPLATE` vs `GENERIC_FALLBACK`;
  - Message Pack template ID/revision when present;
  - event kind and product identity when present;
  - existing provenance reason.
- A shape retired only because it no longer qualifies as unsupported evidence is recorded separately as `NO_LONGER_UNSUPPORTED`; it is not falsely reported as a parser fix.
- Active Message Lab evidence and its decision metadata are still removed after successful retirement, so resolved work does not remain in the active queue.
- Data Health now shows **Resolved Message Lab evidence** with the sanitized shape, parser route/template, prior meaning, and recurrence evidence.
- Added pure policy tests for:
  - template-based parser resolution;
  - generic semantic resolution;
  - relevance-only retirement;
  - preservation of sanitized evidence/review state.

Feedback loop:

`unsupported real message -> evidence/review -> parser or generic-grammar improvement -> reprocess/new occurrence -> resolved archive with provenance`

Guardrail:

The archive contains no raw SMS body, transaction code, amount, phone number, email, account number or unredacted counterparty. It is diagnostic proof of coverage improvement, not an accounting ledger and not parser activation authority.

## Resolved Message Lab coverage regression checks — 2026-10-04

The sanitized resolution archive can now be replayed against the current interpreter to detect likely coverage regressions after future parser changes.

Implemented:

- Added `MessageLabResolutionRegressionChecker`.
- Data Health -> Resolved Message Lab evidence now has **Check resolved coverage**.
- The checker never reconstructs the original SMS. It replaces redacted placeholders with deterministic safe synthetic values such as a fake transaction code, amount, date/time, account reference, phone number and counterparty.
- Synthetic replay is passed through the existing local `FinancialMessageInspector`; it does not write transactions, balances, debts, Message Lab decisions or history.
- Archived parser resolutions are classified as:
  - **Still covered** when the current build still understands the replay through the recorded route/template;
  - **Covered by a different route** when coverage remains but provenance changed;
  - **Possible regression** when a previously parser-resolved shape is no longer understood and would return to Message Lab;
  - **Inconclusive** when sanitized evidence is insufficient to prove either coverage or regression.
- Shapes archived only because they no longer qualified as unsupported evidence are checked separately:
  - **Still retired** when they remain outside Message Lab;
  - **Possible regression** if they would become unsupported financial evidence again.
- Data Health shows an aggregate stable / possible-regression / inconclusive summary and per-shape current replay status.
- Added regression tests for deterministic synthetic replay, the real M-Shwari repayment template, relevance-only retirement, route changes and a simulated parser regression.

Guardrail:

A synthetic replay warning is diagnostic evidence only. It is not proof that a real transaction was lost and must not automatically change parser code or accounting state. Any **Possible regression** should be confirmed with a real sanitized device example or Message Inspector evidence before implementation.

## Message Lab resolution meaning agreement guard — 2026-10-04

The sanitized resolution archive now verifies that parser coverage does not contradict earlier user-confirmed meaning before active Message Lab evidence is retired.

Implemented:

- Current parser meaning is derived from deterministic event facts plus semantic intents.
- The archive compares that current meaning with the prior Message Lab semantic label.
- Clear agreement allows normal retirement/archive.
- Broad `Other financial` evidence or interpretations with no comparable meaning remain `UNKNOWN` rather than being called conflicts.
- A clear mismatch blocks retirement. The active unsupported evidence and its review metadata remain in Message Lab, and the diagnostic is not marked covered.
- Archived records preserve the previous user meaning, current resolved meaning labels and meaning-agreement status.
- Data Health shows **Current meaning** and **Meaning check** for archived parser resolutions.
- The guard remains compatible with the resolved-coverage regression checker; regression diagnostics and agreement diagnostics coexist.

Safety example:

A shape previously confirmed as **Loan repayment** is not allowed to disappear merely because a later parser recognizes the wording as an ordinary **Payment**. The evidence stays active until parser semantics agree or the mismatch is explicitly reviewed.

Guardrail:

This comparison is used only to protect evidence retirement. It does not turn user labels into accounting authority and does not create, edit or reclassify ledger state.

