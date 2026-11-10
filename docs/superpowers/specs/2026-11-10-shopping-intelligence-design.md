# Shopping photo scanning, suggestions and history — design

## Intent
Make the existing Plan shopping lists faster to fill each month, using a selected photograph or prior locally saved lists. This is planning, never automatic spending.

## Goals and boundaries
- Select an existing photo through Android's photo picker. Run bundled ML Kit Latin text recognition locally without requiring camera/storage permissions, data upload or a separate model download.
- Show extracted text in an editable review before adding any items. Parse simple item/quantity/price lines conservatively; ignore totals and malformed lines. Label estimates as unverified, never financial transactions.
- Suggest commonly repeated items from local lists, excluding names already in the current list. Price shown as historical estimate, but suggestions added with unknown price until the user enters a new one.
- Show monthly planning history from saved shopping lists: item counts, checked counts and known/partial estimated totals. No inference that a checked item was purchased.
- Respect 36-list and 100-item caps, privacy and encrypted-backup compatibility. Save only approved ShoppingItem values via existing ShoppingListStore.

## Approaches
1. **Bundled offline OCR + deliberate review (chosen):** ~4 MB app growth, works without runtime model fetch, no new permission, editable and reversible planning data.
2. Unbundled ML Kit: smaller APK but needs Google Play Services model download; contrary to offline-first promise.
3. Cloud OCR: high data/privacy burden and internet dependency; not approved.

## UI
On list detail add `Scan photo` and `Suggested for you` panels. A photo opens a review dialog with editable recognized lines and a preview of valid items; one explicit action saves them. The overview shows a small monthly planning history summary, using the current approved card theme. Error, empty, scanning and partial-price states have visible copy. No live camera capture or image retention in this phase.

## Safety
- A scanned price is **only a proposed unit estimate**, and visible in review; if ambiguous the price is omitted.
- No OCR output is sent over the network, written to log, or persisted until the user adds reviewed items; selected photos are not copied into application storage.
- Do not edit PR #656, which overlaps ShoppingListsScreen. Work on a new branch based on main and rebase after #656 if necessary.
- Test domain parsing/suggestions/history including duplicates, over-cap, date grouping and invalid/missing prices. Android CI must verify actual compilation and unit tests; the owner tests on Redmi.
