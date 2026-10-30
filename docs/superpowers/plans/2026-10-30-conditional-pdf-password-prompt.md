# Conditional PDF Password Prompt Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Import unprotected PDFs without a password dialog; ask and retry only for verified password-required PDFs.

**Architecture:** The existing local PDF readers already attempt opening with a supplied password. Make the M-Pesa engine classify its password failures like the bank extractor, translate that one stable result into a typed Kotlin exception, and let the shared Compose import flow auto-attempt with an empty password before opening the secure password dialog.

**Tech Stack:** Android Kotlin/Compose, Chaquopy, embedded pdfplumber/PDFMiner, JVM/JUnit and Python/unittest.

**Spec:** `docs/superpowers/specs/2026-10-30-conditional-pdf-password-prompt-design.md`

## Global Constraints

- Do not weaken source verification, account ownership, review confirmation or financial write gates.
- Never persist or log PDF passwords, document text or private identifiers.
- Preserve CSV behavior, M-Pesa imports, bank imports and existing dialog security flags.
- Work from current `main` in an isolated branch without modifying other chats' branches.

## Review Focus

- Encrypted PDF with incorrect password must allow a safe retry instead of falling back to generic rejection.
- PDF with malformed bytes must show normal import failure, not request password.
- Unprotected PDF must not be fully parsed twice.
- Imported bank statement must still enforce owned-account verification.
- CSV and user-edited account identifier state must not be lost by the PDF change.

---

### Task 1: Detect a password opening failure consistently

**Files:** Modify `app/src/main/python/mpesa_pdf_engine.py`, `app/src/main/java/com/kedevjimmy/pesatrack/documents/OfflineMpesaPdfReader.kt`, `OfflineBankTableReader.kt`; create `app/src/main/java/com/kedevjimmy/pesatrack/documents/StatementPdfOpeningPassword.kt`; test the two existing synthetic Python contracts and a new JVM policy test.

**Interfaces:** `StatementPdfOpeningPassword.requireUnlocked(status: String, reason: String)` throws `StatementPdfOpeningPasswordRequiredException` solely on `blocked/pdf_password_rejected`.

- [ ] Add failing Python synthetic password-error test to M-Pesa extractor, check red.
- [ ] Add failing JVM behavior test for exact password error classification versus other errors, check red.
- [ ] Add whitelisted PDFMiner password-error mapping to M-Pesa Python engine and shared Kotlin classification in both readers.
- [ ] Verify Python and JVM tests green.

### Task 2: Conditional UI prompt

**Files:** Modify `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementImportScreens.kt`.

**Interfaces:** On PDF selection run the same reader/ownership verification with an empty password; on typed password exception preserve URI and show masked secure dialog; on wrong password show retry hint, and otherwise show normal sanitized errors.

- [ ] Refactor existing dialog import into a single helper used for initial no-password attempt and password retry, keeping all bank and M-Pesa checks.
- [ ] Confirm no password dialog appears for normal PDFs and only typed error opens it; preserve CSV and account-number editor.
- [ ] Run focused synthetic tests and full repository Python suite, then Android lint/unit/debug build in CI; review diff, PR and merge after checks pass if safe.
