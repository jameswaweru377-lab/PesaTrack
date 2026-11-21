# Approved compact dark PesaTrack onboarding

**Date:** 21 November 2026
**Status:** Owner-approved for implementation, superseding the earlier cream/coffee proposal and PR #701's first-run experience.

## Native sequence

1. **Photographic splash** — Nairobi café image approved by the owner, full-bleed, with the **current Home geometric brown P mark** centered, PesaTrack serif wordmark, and one native Get started button. No old black-and-green logo, benefit cards, or Skip.
2. **Your money, your space** — continuous dark PesaTrack surface. Required full name, M-PESA mobile number, six-to-eight-digit local PIN, and matching PIN confirmation. An independent unchecked financial-SMS disclosure checkbox is part of this form, not a separate screen. Start scanning commits the profile, PIN and consent locally before Android permission is requested. No online account registration, OTP or Not now.
3. **Android READ_SMS** — native system grant. Grant proceeds to scanning; denial safely skips SMS access and allows supported institutions to be searched manually. No repeated consent page.
4. **Reading your messages** — the entire background is black, with no enclosing panel or secondary Progress card. The receipt-and-coins icon stays in the center of an honest indeterminate circular ring. “Importing transactions…” and subsequent real processing phases sit immediately under the heading, above the “Finding M-PESA and bank transactions…” description. Import all financial SMS still available on-device. No date picker, completion button or fictitious percentage.
5. **Android notification prompt** — after the import finishes, ask for `POST_NOTIFICATIONS` with the native Android popup where supported. Denial does not block account selection. Notification-listener access is distinct and is not requested by a fictitious runtime popup.
6. **Which accounts are yours?** — dark canvas, searchable **borderless** dark rows, borderless search input, radio-shaped multi-select controls, and up to five suggestions ranked by transaction evidence (not promotions, sender mentions, or balance-only events). Search includes other supported institutions. All selections initially empty; only the user can confirm ownership. At least one institution must be selected for Confirm my accounts. No “I'll choose later” or extra completion screen. Home opens after ownership writes finish successfully.

## Safety and compatibility

- The supplied name is a local matching hint; no particular name or number is hardcoded. Phone normalization accepts Kenyan formats and stores the normalized hint. The M-PESA identifier is linked to the user's wallet **only if** the user explicitly confirms M-PESA ownership on the account screen.
- Matched names alone must never turn separate transactions into own-account transfers. Keep existing trusted identifiers, reconciliation rules, debit/credit semantics, replay detection and balances intact.
- Never import SMS without successfully persisted in-app consent and Android `READ_SMS` permission. Preserve original event dates and duplicate prevention. No new remote data flow.
- Missing or denied SMS access leaves a searchable manual institution directory, without inventing balances or default-owned accounts.
- Preserve the existing encrypted-backup restore screen, PIN verification and recovery path, non-first-run Home and settings, and signed-upgrade data.
- No instrumentation execution. The owner will review the visual and permission flow on a physical Redmi device later.
