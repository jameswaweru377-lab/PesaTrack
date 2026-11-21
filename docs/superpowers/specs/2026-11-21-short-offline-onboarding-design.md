# Short offline onboarding — approved design (21 November 2026)

## Intent

Replace the long first-run setup with an approachable, short PesaTrack onboarding informed by the owner's screenshots of SMS reading, import progress, backup restoration and account creation. Use the app's existing cream, coffee-brown and subtle-green light/dark theme; do not copy the example app's teal styling or device overlays.

## Experience

1. One welcome page: two benefits, one primary Get started button, optional Set up later.
2. One concise financial-SMS disclosure, with explicit on-device consent, Android's own READ_SMS dialog, and a no-SMS path. Keep the recent-history default and optional custom dates; do not expand the data-access scope.
3. A focused progress screen with an illustrated icon, informative status and an indeterminate progress indicator while financial messages are imported and local accounts are discovered. Never invent a percentage or claim M-Pesa is the only possible source.
4. A short list to let the user confirm *their own* discovered accounts. None is preselected; skip remains safe.
5. A short, reassuring completion screen followed by the existing mandatory local 6–8 digit app PIN setup. The PIN screen should be visually adapted to the reference's compact registration layout: a themed hero, two masked fields, one primary action. No phone/password registration or cloud identity.

Notification access is optional and belongs in Settings or a contextual prompt after entering the app; it must not block initial SMS import. A restoring-data progress screen appears only while a user explicitly restores an encrypted backup, never on ordinary startup. Android permission dialogs remain system-owned and are not reskinned.

## Safety and anti-goals

- Preserve PesaTrack's offline privacy, existing consent/revocation handling, READ_SMS-only scope, data stores, account ownership authority, deduplication, original transaction dates, backup/restore format and mandatory app-lock gate.
- Do not add a registration backend, password or phone-based identity, new permissions, telemetry, dependencies, signing changes or Android instrumentation.
- Retain custom historical import dates and the ability to continue without SMS; do not imply unavailable SMS records were imported.
- Responsive small-screen and large-font layout; at least 48dp touch targets, theme-derived colors, accessible progress status.

## Approaches considered

- **Chosen:** Adapt the existing Compose flow and local PIN setup, remove the duplicated welcome route and simplify the live screen components. Low behavioral risk, no new authentication layer.
- Replace the flow with a new separate navigation stack. More duplication and risk to SMS/ownership rules.
- Implement the reference's phone/password sign-up. Rejected by the owner in favor of local PIN and incompatible with offline-only local-first operation.

## Verification

Add focused regression checks around a single welcome route, accurate import progress, consent/skip persistence, real account review and existing PIN validation. Run source UI contracts locally and the existing GitHub Android workflow (JVM unit tests, lint, debug compilation). Physical Redmi visuals and restore/permission acceptance remain the owner's tests; no instrumentation.
