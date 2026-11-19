# UI polish: Activity, Home, Plan, Accounts and Profile

Tracks #688 · Chat owner: CHAT-UI-POLISH-20261119

## Approved behaviour

- Keep the centered Home PesaTrack mark, wordmark and tagline untouched.
- Explain the available-money value in plain English, distinguishing incomplete account balances from complete balances.
- Use a slim expandable balance-review row, and compact its detailed cards while retaining all status, explanatory, menu, wallet and balance-update actions.
- Remove brown card outlines from the Home account tiles and Accounts balance list; maintain filled surfaces distinct from the dark canvas.
- Use `MaterialTheme.colorScheme` in Profile workspace/settings so light styling is preserved and dark text/cards are legible.
- Use a common small mark/divider/title header on Plan and Accounts, pinned by the scaffold. Activity keeps its search and period controls in a sticky list header. Its visible selected filter is smaller inside a 48dp tap target.
- Keep daily transaction net totals, filter keys, review behavior, the ledger and storage unchanged.

## Implementation / verification

1. Add focused tests for Home available-money copy and main-tab header ownership.
2. Change only the referenced Compose components and their shared header helper.
3. Check the diff and then run the existing Android PR CI checks: JVM tests, lint and APK assembly. The local Gradle distribution is not installed and the distribution download is blocked, so CI is the build/test authority.
4. Leave merging, signed APK publishing and phone validation for separately authorized work.
