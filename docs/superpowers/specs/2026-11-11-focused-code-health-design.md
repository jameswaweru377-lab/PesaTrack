# Focused code health and startup visual consistency

## Intent

Prevent the old violet/blue startup appearance from flashing before the approved brown PesaTrack UI, while removing only conclusively dead UI and preserving all financial behavior. The task does not promise a speed gain without measurements.

## Constraints

- Keep offline Python/PDF processing, ML Kit shopping OCR, all local ledger and backup behavior, signing and APK update compatibility.
- Keep the app-lock, onboarding and startup navigation decisions unchanged.
- Do not broadly recolour other screens or remove active historical colour tokens. This task concerns startup surfaces only.
- Compare source and, when available, real-device startup behaviour before claiming success.
- The already isolated Accounts carousel deletion remains a separate change and needs CI verification.

## Approaches

1. **Selected: narrow startup alignment.** Use approved brown/cream visual tokens on the Android window and startup splash, and remove the startup-only old purple styling. Small change, low risk.
2. Whole-theme replacement. Recolour all MaterialTheme components throughout the app. Rejected for now: would change many screens without approved visuals or regression coverage.
3. Delete the splash and timer. Rejected for now: changes startup navigation and app-lock timing; measure first.

## What counts as success

- Startup window and composed splash no longer introduce violet/blue styling before the Home screen.
- App-lock, onboarding and Home route decisions are unchanged; the existing 650-ms splash lifetime remains until separately profiled.
- No changes to accounting, permissions, dependencies, images, PDF/OCR, storage, backup, signing or workflows.
- Relevant unit/static tests and CI build/lint must pass before merging; physical phone remains a separate verification gate.
