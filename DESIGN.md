# PesaTrack Design Standard (current direction)

**Status:** Approved product direction, documented 30 October 2026. This is a design contract, **not** a declaration that every screen complies.

PesaTrack is a local-first Kenyan financial companion. Trustworthy money interpretation and clear uncertainty are more important than decoration. Preserve approved Home, Activity, Plan, More, Statements and transaction-detail work unless a later explicit approval replaces it.

## Visual direction

- **Launch in light mode.** The active app entry point currently forces the light Compose theme; a stored theme setting must not be described as proof that dark-mode switching is active.
- **Brand:** indigo/purple, light off-white canvas, white/soft cards, restrained gradients, dark readable text. Teal, amber and rose are semantic accents, not a second competing brand.
- **Navigation:** exactly four visible bottom tabs: **Home · Activity · Plan · More**. Accounts remains reachable inside other destinations; it is not a fifth bottom tab. Do not restore the center plus button or nested legacy bottom navigation.
- **Layout:** compact information hierarchy, a consistent grid, generous but controlled breathing room. A prominent financial summary is followed by supporting details; do not pile decorative cards on top of essential information.
- **Content:** amounts are always sourced from verified local financial state. State the meaning and period of a figure. Gross statement cash movement is *not* income or spending, and self-transfers must not inflate totals.

## Existing source of truth in code

| Concern | Current implementation | Rule for future changes |
|---|---|---|
| Material color scheme | `app/src/main/java/com/kedevjimmy/pesatrack/ui/theme/Color.kt` and `Theme.kt` | Reuse `MaterialTheme.colorScheme` / named theme tokens rather than new screen-local hex values. |
| Common spacing / shape tokens | `ui/theme/PesaDesignTokens.kt` | Reuse existing named tokens. Do not silently change global radii or padding to match an audit *proposal* without visual approval. |
| Shared UI pieces | `ui/components/PesaComponents.kt` and other `ui/components/` files | Prefer shared cards, rows, headers, status pills and loading/empty/error patterns over one-off copies. |
| Bottom navigation | `ui/screens/HomeReferenceDesign.kt` (`ReferenceHomeBottomBar`) | Keep Home / Activity / Plan / More, even though `MainTab.ACCOUNTS` remains an internal route. |
| App launch theme | `MainActivity.kt` | Light launch is intentional; do not promise fully verified theme switching. |

Known current light palette tokens include `AuroraIndigo` `#5B55E7`, `Porcelain` `#F6F7FB`, `SurfaceLight` `#FFFFFF`, `Ink` `#171B2C`, `Slate` `#667085`, `AuroraAqua` `#14B8A6`, `Amber` `#F0B74A` and `Rose` `#EF5F78`. Use the definitions in code, not copied hexadecimal values in new screens. `PesaDesignTokens` currently defines 18dp horizontal padding, 16dp card content padding, and 22dp card radius. The audit's illustrative 16dp page margin / 16dp radius values are **suggestions for future consistency work**, not authorization to overwrite approved layouts.

## UI and accessibility acceptance

1. A screen must have one clear primary action; secondary actions remain visually quieter.
2. Show honest loading, empty, partial-evidence and error states; no mock account balances or fake completed features.
3. Keep money formatting and signs consistent across Home, Activity, Plan and Statements. Clearly distinguish expenses, transfers, loans, refunds, gross movement and uncertain evidence.
4. Put date range, account scope, units, and any exclusions near the figures they qualify. Charts require readable labels and accessible text equivalents.
5. Avoid redundant headers, duplicated month selectors, repeated navigation bars, decorative rings with no purpose, and overlong nested cards.
6. Use appropriate system insets, font scaling, TalkBack descriptions, contrast and at least a 48dp effective touch target for interactive controls.
7. Compare changed screens to the **latest user-approved** reference image; do not treat historical mockups or the 7 October dark-mode proposal as current authority.
8. Preserve existing financial behavior when polishing UI. Any formula change needs separate deterministic tests and explicit scope.

## What this document does **not** complete

The 29 October whole-system audit found many hard-coded colors, unscaled dimensions and inconsistent UI patterns. The existence of `Color.kt`, `PesaDesignTokens.kt` and this document does not mean those have been cleaned up. Findings **U1, U3 and U4 remain open** until source changes, screenshot/accessibility checks and finance tests pass. See [audit remediation tracker](docs/AUDIT_REMEDIATION_TRACKER.md).

Historical context: [`docs/DARK_MODE_V2_APPROVED.md`](docs/DARK_MODE_V2_APPROVED.md) records an older emerald/charcoal concept and must not override this standard.
