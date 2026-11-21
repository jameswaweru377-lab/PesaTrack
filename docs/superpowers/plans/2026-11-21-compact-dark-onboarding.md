# PesaTrack compact dark onboarding implementation

## Outcome

Replace first-run onboarding with an approved photographic splash, one mandatory local profile/PIN/financial-SMS disclosure form, an automatic full-black import screen, and a borderless dark institution selector. Successful selection enters Home directly. Keep SMS access behind both explicit saved consent and the Android runtime grant; never infer ownership from names or a detected message.

## Tasks

1. Add a source-contract test that fails on the old flow; update it as each native screen becomes reachable.
2. Bundle the approved photographic background without burned-in text, use the current Home P mark, and replace the interactive splash.
3. Add a validated offline profile/PIN page with disclosure checkbox, synchronous persisted consent, and no skip; wire automatic Android READ_SMS request. Import all available financial SMS by default when granted; decline gracefully if permission is denied.
4. Replace the analysis panel with a full-black canvas, centered progress ring and receipt-and-coins illustration; request POST_NOTIFICATIONS only after import.
5. Restyle the search and top-five detected-institution list without borders; save explicit checked ownership and enter Home directly.
6. Run focused source tests, local unit/build if available, perform final diff review. Do not run instrumentation.

## Verification

- `python3 -m unittest tools.test_compact_onboarding_contract`
- `python3 -m unittest tools.test_premium_ui_contract` (update superseded design assertions)
- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest --offline` if Android SDK/Gradle cache supports it.
