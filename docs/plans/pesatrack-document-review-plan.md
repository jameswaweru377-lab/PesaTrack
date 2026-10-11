# Document review implementation plan

Execution: inline under superpowers:executing-plans; one fresh whole-branch reviewer at the end.

Spec: pesatrack-document-review-design.md. Kotlin/Compose/SQLite/native PdfRenderer; minimum app API24, text feature API35. Instrumentation prohibited by current user instruction.

- [x] Write JVM negative tests for bounded PDF preview: unsupported API, empty/mixed pages, byte/page/text limits, payroll source cannot authorize cash, expected salary never posts.
- [x] Run new tests to observe red, implement PDF review policy and cash adapter gate, then observe green.
- [x] Add private isolated extraction service and cancellable client; guard Application initialization. No ledger write interface in extractor.
- [x] Integrate transient PDF preview in existing statement sheet; keep CSV path and duplicate review unchanged; add clear actual-receipt explanation.
- [x] Wire authority contract to selected statement and manual salary writes; reject disabled/missing salary account; retain canonical replay.
- [x] Compile Android tests without running them; run one combined unit/lint/APK validation after all edits.
- [ ] Review complete diff; batch corrections, update tracker/current state, create one PR and rely on one final branch CI.
- [ ] Merge only green/reviewed current head with no active main run; verify main build/publication. Record remaining API24/PDF-layout/device limitations.

## Local evidence and rulings

Final frozen-source combined run: `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin`, successful 11 October 2026. JVM 813 cases, zero failures/errors/skips with optional export enabled. Python laboratory 30/30 passed. No connected tests, emulator, signing or workflow edits. Source review approved after fixing stalled-provider cancellation. Review identified provider responsiveness as a device acceptance limit; UI timeout no longer joins blocked I/O, cancellation is flagged immediately, source close/provider cancellation are requested off the UI thread, and late results cannot initiate extraction.

Ruling: Android15+ native text preview is the first E1 slice; API24–34 extraction remains pending because adding an older port and its cryptographic dependencies without a separately assessed compatibility/security corpus would enlarge this bundle. Native text availability does not prove bank/payroll table interpretation. No new runtime dependency.

Ruling: the local Windows JDK's Unix-domain pipe failed before tests; used a workspace-only TCP-pipe workaround, never shipped or committed. Earlier local lint errors were explicit API-guard errors, corrected. An interrupted/stale lint analysis was superseded by the successful frozen-source run above. CI is authoritative for the committed Linux build.
