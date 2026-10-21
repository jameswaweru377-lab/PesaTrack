# Approved Home visual reference — implementation audit

**Reference:** User-provided PesaTrack light Home screenshot, 21 October 2026.
**Scope:** Home only. This is a reference implementation, not a redesign or new financial formula.

## Top-to-bottom audit

| Reference area | Implementation | Source |
|---|---|---|
| Native status/safe-area | Android status bar remains system-rendered; content uses statusBarsPadding | AppScaffold.kt |
| Wordmark | Existing PesaTrack P-logo + single bold dark wordmark | HomeReferenceDesign.kt |
| Bell and purple dot | Round bordered white button with small purple indicator, linked to financial-attention review | HomeReferenceDesign.kt, PesaTrackApp.kt |
| Round J profile | Lavender avatar circle with user's first initial; opens profile | HomeReferenceDesign.kt |
| Greeting | Dynamic time-of-day greeting and real user's first name; unchanged subtitle | HomeReferenceDesign.kt |
| Hero | Deep purple/blue gradient, cyan overlay, white heading, balance, month chevron, eye visibility toggle, white Review plan and translucent Accounts pills | HomeReferenceDesign.kt |
| Three-column This month | White rounded card; incoming (mint), outgoing (pink), budget remaining (blue); icon bubbles, dividers, comparisons, progress bar | HomeReferenceDesign.kt |
| Needs attention | Pale peach rounded card, orange document icon, category-review count and right chevron | HomeReferenceDesign.kt |
| Upcoming | White rounded parent, purple See all, side-by-side lavender bill and mint salary mini-cards, calendar icons, due subtitles and amounts | HomeReferenceDesign.kt |
| Recent activity | White rounded parent, purple See all; up to four icon-coded transaction rows, times, amounts, separators and right chevrons | HomeReferenceDesign.kt |
| Navigation | Floating-feel white rounded bar, Home purple gradient, Activity/Plan/Accounts/More gray icon+label; OS draws the bottom gesture indicator | AppScaffold.kt, HomeReferenceDesign.kt |

## Financial integrity and responsive rules

The reference image uses sample KES amounts from April 2025. Those numbers
are used **only in UI contract tests**, not as a production balance, income,
spend, loan, or salary. Installed PesaTrack continues to derive all displayed
values from the current user's existing financial records. If the source data
is missing, the UI says “Unavailable”, “Not set” or “Not scheduled” instead of
fabricating income, bills, or budget remaining. Buttons retain real navigation.

Cards follow the reference shape, spacing, gradient and layout. On smaller
Android phones the Home content scrolls rather than compressing text into
unreadable sizes. Operating-system status icons and the home gesture indicator
are Android-native; they are not replaced by fake iOS artwork.

## Final acceptance / outstanding checks

- [x] Source-code section-by-section visual hierarchy and labels audited
- [x] Home-specific top and bottom bars avoid redesigning other screens
- [x] Buttons route to real app destinations, hero eye works
- [x] Data stays live; screenshot fixture amounts limited to UI tests
- [ ] Screenshot captured from real Android build and compared pixel-by-pixel
- [ ] Light-mode small-phone and large-phone visual checks
- [ ] TalkBack contrast and dynamic-font check
- [ ] Real-device complete financial-navigation test

CI compiles Android UI tests, but emulator instrumentation is not run in the
default workflow. Do not claim pixel-perfect device verification without
capturing the rendered Compose screen.
