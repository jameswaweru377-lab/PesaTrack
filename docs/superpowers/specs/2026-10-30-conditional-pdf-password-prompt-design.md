# Conditional PDF password prompt (30 October 2026)

## Intent

PesaTrack should not ask for a PDF password when a selected M-Pesa or bank statement opens without one. A genuinely protected PDF should request its opening password privately and retry on an incorrect password.

## Scope and constraints

- Retain existing on-device, bounded PDF readers and full financial verification before any statement row can be saved.
- Attempt local full PDF verification with an empty password on selection. This avoids a duplicate parsing pass for ordinary PDFs.
- Only the PDF engine's explicit password-rejected code may trigger a secure, masked password dialog; malformed or unsupported PDFs must retain their normal errors.
- An incorrect password reopens the secure prompt with a helpful, generic retry message; no password, file contents, account identifiers, or exception details may appear in logs, analytics, or saved state.
- Preserve CSV import, account selection, review confirmation, bank ownership checks, and existing M-Pesa/Equity layouts. No new network call or permission.

## Approaches considered

1. **Ask every time (current):** simple but interrupts every unprotected PDF.
2. **Separate PDF-encryption preflight:** detects protection before import, but reads and opens every normal PDF twice.
3. **Attempt full read with empty password, prompt only on precise password failure (selected):** reuses the already required verification, minimizes work for normal PDFs and preserves all admission safeguards.

## Acceptance

- A valid unprotected PDF reaches the existing review flow without a password dialog.
- An encrypted PDF needing a password opens a secure dialog only after a classified password rejection; the correct password can reach review.
- A wrong password can be retried, and a malformed/unsupported PDF never masquerades as a password error.
- Existing financial and privacy protections remain unchanged; real-phone validation is separately required.
