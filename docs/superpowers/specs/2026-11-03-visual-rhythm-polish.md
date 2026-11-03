# PesaTrack visual rhythm polish — 3 November 2026

## Intent

Refine the approved native Compose app without redesigning screens: calmer
typography, consistent 16dp content margins, proportional controls, quieter
cards, accessible touch targets and a lighter five-tab bottom bar. Keep the
purple brand and the distinctive financial hero artwork.

## Constraints

- Keep Home / Activity / Plan / Accounts / More and all existing destinations.
- Preserve financial text, amounts, accounting, transaction decisions and
  local-first privacy. Do not change release, signing, permissions or data.
- The Accounts refresh has merged through PR #604. Do not modify its
  Accounts/Debts/picker/details files in this pass; inherited
  typography/navigation can be reviewed in a later visual QA stage.
- Leave deliberately illustrated gradient hero surfaces and the approved
  Profile cover height alone.
- Keep primary actions at least 48dp high and bottom tab tap areas at least
  48dp high. Do not make secondary financial information smaller.

## Approach

1. Shared theme values and typography establish the visual rhythm for
   MaterialTheme-based screens and shared cards.
2. Refine the existing bottom-bar component: smaller selected treatment inside
   a full-size accessible tap target, no navigation changes.
3. Adjust a few demonstrated high-traffic local controls in Home, Activity,
   Plan and Bills without touching financial calculations or other owners'
   code. Reduce gratuitous elevation, never remove semantic status colours.

This is an incremental implementation slice of the cross-screen polish,
not a claim that all 100+ conditional Android screens have been device-reviewed.

## Acceptance

- Five tabs remain visible and navigate as before.
- Shared text roles use a readable, balanced weight hierarchy.
- Shared neutral cards have quieter elevation and consistent corners.
- Date/month/analytics controls remain legible and have >=48dp tap areas.
- No amount, balance definition, label meaning, app permission or storage logic
  changes.
- JVM test, Android lint and debug APK assembly pass in CI before merge.
- Redmi screenshots at normal and increased font scale remain necessary before
  declaring visual QA complete.
