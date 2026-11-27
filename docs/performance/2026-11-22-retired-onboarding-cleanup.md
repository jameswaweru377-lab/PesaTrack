# Retired onboarding illustration cleanup — 22 November 2026

Task: #712  
Chat owner: CHAT-ONBOARDING-CLEANUP  
Base: `ee8c436d93851355b44b381007ffdb0a58dbf18a`

## Result and scope

Remove unused illustration code left behind by the compact cafe onboarding.
The current welcome, local profile/PIN, financial-SMS consent, scan and account
confirmation screens are unchanged. Financial processing, stored data,
permissions, offline Python/PDF processing, signing and workflows are unchanged.

## Evidence before removal

Default-branch GitHub code searches at the base commit found:

- `PremiumOnboardingHero`, `PremiumFeatureRow`, `PremiumInfoPanel`,
  `PremiumHistoryTimeline` and `PremiumRangeOption`: declarations only in
  `PremiumOnboardingVisuals.kt`.
- `ApprovedReferenceHero`: its declaration in that file, plus negative
  assertions in `tools/test_premium_ui_contract.py`; no call sites.
- `PremiumWelcomeHero`: its declaration only in `PremiumWelcomeHero.kt`.
- `onboarding_analysis_hero`: a negative source assertion only.
- `onboarding_privacy_hero` and `onboarding_welcome_hero`: referenced only
  inside the unused `ApprovedReferenceHero`.

Negative assertions are retained. Existing cafe, Nairobi and wallet images
remain; in particular, `wallet_welcome` is still referenced by shared
`ReferenceConceptComponents.kt`.

| Removed file | Source bytes |
| --- | ---: |
| PremiumOnboardingVisuals.kt | 12,979 |
| PremiumWelcomeHero.kt | 4,425 |
| onboarding_analysis_hero.webp | 122,374 |
| onboarding_privacy_hero.webp | 114,046 |
| onboarding_welcome_hero.webp | 134,704 |
| **Total** | **388,528** |

The three images total 371,124 bytes. These are repository file sizes,
not measured APK savings. APK compression, generated resources, compiled code,
alignment and signing affect the final download size. No installed-size or
startup-speed improvement is claimed.

## Validation

The prior successful main Android CI run was
[45012759491](https://github.com/jameswaweru377-lab/PesaTrack/actions/runs/45012759491),
at `5576038f10bfe1644852973b4be62e8a6cf6847d`; it is baseline evidence only.
This cleanup still requires its own exact-head Android CI JVM tests, lint,
debug APK assembly and existing source/security checks before merge.
No instrumentation or emulator tests are requested or run.
Real Redmi visual and signed-update acceptance remains unverified.
Older source-only UI contracts contain historical design expectations and
are not evidence of current on-device rendering.

## Separate existing work

- #670: Accounts carousel/startup cleanup. No changes to its paths here.
- #549: release R8/resource shrinking. Do not duplicate its build/workflow work.
- #600: dependency updates. No Gradle changes here.

This is a bounded removal of confirmed unused onboarding files, not a
whole-repository unused-code certification. Larger dependency/native-runtime
savings require separate measurement and compatibility verification.
