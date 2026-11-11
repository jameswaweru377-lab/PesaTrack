# Nano-first private shopping scans

## Intent

Understand unstructured shopping lists and receipts without a network upload, while preserving the current offline scanner on devices without Gemini Nano.

## Scope and safety

- A chosen photo is first read by existing bundled ML Kit OCR.
- Where Android AICore reports Gemini Nano **available**, ask it to turn OCR text into candidate shopping-item lines; show the candidate lines in the existing editable review dialog.
- Otherwise (unsupported, model not installed, failure, empty/invalid response), use the untouched OCR transcript as today. Never block a scan on AI availability.
- Display which processor supplied the draft. No automatic downloads or hidden network calls. Device model download, where supported, is a separate user action.
- Preserve explicit user confirmation, local-only processing, no retained source photo/transcript, same encrypted-backup format, and no Activity/ledger/account/budget changes.
- Require the model to leave ambiguous prices unknown. Keep its untrusted output bounded and under user review.
- Android API 24–25 must keep working through OCR-only fallback. No additional runtime permissions.

## Approaches considered

1. **Nano-only:** rejected; many Android phones, including the owner's Redmi Note 14, are not listed as supported.
2. **Always standard OCR:** reliable accessibility but weak semantic interpretation.
3. **Nano-first with offline fallback (selected):** better prospects on supported hardware without breaking the current experience; model responses remain suggestions.

## Acceptance

- Available Nano gives a proposed, editable candidate draft; standard OCR remains available when unavailable or failing.
- Invalid or hallucinated output never writes to the local shopping list, and canceled scans do nothing.
- No data transfer or new storage contract; exact support and extraction accuracy remain unverified until tested on a compatible phone.
