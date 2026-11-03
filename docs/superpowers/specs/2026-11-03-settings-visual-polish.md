# PesaTrack Settings visual polish — 3 November 2026

## Intent

Make the Settings landing and Money message access pages feel consistent with
the approved calm-fintech design, using existing icon bubbles, semantic tones,
typography and 48dp touch targets. This is a second independent visual slice
after the shared typography/navigation pass in PR #610, not the entire app audit.

## Constraints

- Preserve existing destinations, toggles, source approvals and Android intents.
- Do not change permission grants, persisted choices, data collection or money.
- Keep local-first wording true and the existing purple brand prominent.
- Use existing `IconBubble`, `pesaToneContainer`, `pesaToneContent` and
  `PesaDesignTokens`, without introducing new colours or dependencies.
- Shorter labels must still explain which SMS/bank/wallet access can be reviewed.

## Approach

1. Present Settings shortcuts as a quiet two-row group with violet/blue icon
   bubbles, consistent typography, and no redundant navigation explanation.
2. Use the same row style for money message access: violet SMS, blue bank-app
   notifications, green approved-source controls. Keep all click handlers.
3. Give secondary Android-settings and stop-access controls 48dp minimum hit
   areas and retain detailed consent guidance behind the existing disclosure.

## Acceptance

- Every previous action remains reachable and has the same callback.
- UI layout changes do not alter SMS/app consent state or notification access.
- Shared token usage and `git diff --check` pass; exact-head Android CI, lint,
  unit tests and APK assembly pass before merge.
- Redmi normal/large-text screenshots and TalkBack interactions still require
  owner-device review; no visual score is claimed from code inspection alone.
