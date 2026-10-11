# A2 / E1 document review bundle

Authorized by the user's 11 October instruction to continue pending tasks, batch safe changes, and never run instrumentation. Base: f4264f2.

Extend existing salary and statement flows without a database migration. Manual received salary remains real income; expected salary remains planning. Use the A1 authority contract at manual-salary and selected-statement write boundaries, preserving existing replay and duplicate safeguards. Validate that the salary's current receiving account is enabled before a write. UI explicitly distinguishes recording received salary from expected payday.

Add an optional PDF text preview to Import statement, also usable to inspect a payslip. Use Android's native API35 extractor in an exported=false isolated service with no ledger access or financial uploads. The application must skip backup/database initialization in this process. Copy user-selected content to a private ephemeral seekable file, cap 2 MB, 40 pages and 100,000 extracted characters, allow one extraction, timeout after 45 seconds, close descriptors and remove temporary files. Preview is memory-only; no inferred transactions or payroll balances are persisted. Report empty pages (possibly scanned), protected/corrupt files, and older Android versions honestly. No new library/permission or OCR.

Bank/M-Pesa layout adapters, payroll extraction, pre-Android15 text extraction and a general evidence-link repository remain separate work. A selectable PDF can be read without its table being reliably interpreted. Do not advertise comprehensive PDF import yet.

Validation: deterministic JVM policies and negative cases, unit suite/lint/APK assembly, Android-test compilation only; no emulator execution. One whole-branch review and PR build before merge; check no main workflow active before merge and wait for main publication before the next bundle.
