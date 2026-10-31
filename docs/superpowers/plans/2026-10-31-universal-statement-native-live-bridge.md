# Universal Statement Native Live Bridge Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Connect the owner-approved native, read-only universal statement recognizer to the live Statements screen, while proving that an unfamiliar PDF cannot reach the verified archive until independently authenticated full-document evidence exists.

**Architecture:** Reuse the existing isolated Android 35+ native PDF text/geometry service, the Slice A `UniversalStatementReadOnlyProbe`, and the existing bank archive admission boundary. Add only an in-memory, explicitly provisional preview for previously unsupported layouts; leave recognized Equity/M-Pesa paths untouched. Prepare a strict admission handoff and a separately gated first-bank pilot rather than pretending an unsupported issuer can be verified from table arithmetic alone.

**Tech Stack:** Kotlin, Jetpack Compose, Android 35+ `PdfRenderer` via existing isolated process, JUnit 4, existing SQLite statement archive. No new dependencies, permissions, network, OCR, or database schema.

**Spec:** `docs/superpowers/specs/2026-10-31-universal-statement-safe-auto-import-design.md` (owner-approved Option 2, native approach). This plan covers the **first production connection** of Slice B; the first auto-saveable unfamiliar issuer family remains gated on independent issuer and full-document proof plus device acceptance.

## Global Constraints

- PR #492 (Slice A) is still a draft, unmerged dependency. Start the implementation branch at its exact reviewed head or a main branch containing it; do not duplicate its commits or silently merge it.
- Never call `StatementBankArchiveStore.save` with `StatementProbeResult.Provisional`, guessed issuer metadata, account suffixes, inferred totals, or any other unverified document.
- Known Equity `unverified_issuer`, ownership, currency, password, balance, or printed-total failures must **not** be retried through a weaker archive path.
- Native positioned text is supported only on Android API 35+; do not quietly substitute the Python extractor on older devices for arbitrary unknown banks.
- M-Pesa, Equity, existing CSV, Activity balances, statement categorization, source-coverage selection, and approved Home/Activity/Plan/More UI remain unchanged.
- Never store or commit real statement text, account identifiers, screenshots, passwords, or fixtures. Keep a selected PDF on-device and use invented test evidence.
- Preserve the current `statement_sources_v2` atomic/idempotent SQLite writer and `StatementBackupArchive` schema contract; no new tables or migrations in this slice.
- Follow `AGENT_WORKING_METHOD.md`: inspect affected files, tests first, scoped changes, focused tests before full CI, no instrumentation without specific authorization, no merge/release without owner acceptance.

## Review Focus

1. **Incomplete coordinate transport:** all pages have extractable text, but one page's `positionedComplete` is false. Task 1 must block the entire preview.
2. **Unsupported native API:** an Android 34 device must show an explicit unsupported message and perform no fallback or save. Tasks 1 and 3 test this.
3. **Recognized Equity issuer rejected:** `unverified_issuer` must remain a terminal refusal, not become a generic candidate eligible to save. Task 2 tests this routing.
4. **Wrong selected account:** a provisional preview must not be mistaken for proven ownership; no imported source appears in Statements. Task 3 tests reset-on-account-change and disabled saving.
5. **Malicious/reordered source:** two PDF pages whose geometry is truncated, duplicated, or whose later rows break balances must yield no candidate or partial persistence. Tasks 1 and 2 test this.

---

### Task 1: Strict native PDF-to-probe transport

**Files:**
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/domain/statements/StatementNativeProbeTransport.kt`
- Create: `app/src/test/java/com/kedevjimmy/pesatrack/domain/statements/StatementNativeProbeTransportTest.kt`
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/documents/OfflineNativeBankProbeReader.kt`

**Interfaces:**
- `enum class NativeProbeBlockReason { UNSUPPORTED_DEVICE, INCOMPLETE_GEOMETRY, EMPTY_OR_SCANNED_DOCUMENT, UNRECOGNIZED_LAYOUT, UNVERIFIED_MOVEMENTS, PDF_READ_FAILURE }`
- `sealed class NativeProbePreparation { data class Ready(val pages: List<StatementProbePage>) : NativeProbePreparation(); data class Blocked(val reason: NativeProbeBlockReason) : NativeProbePreparation() }`
- `object StatementNativeProbeTransport { fun prepare(api: Int, preview: PdfTextPreview): NativeProbePreparation }`
- `sealed class NativeBankProbeResult { data class Provisional(val result: StatementProbeResult.Provisional) : NativeBankProbeResult(); data class Blocked(val reason: NativeProbeBlockReason) : NativeBankProbeResult() }`; define this and both `NativeProbeBlockReason` and `NativeProbePreparation` in the pure-domain `StatementNativeProbeTransport.kt` file, not the Android reader.
- `object OfflineNativeBankProbeReader { suspend fun inspect(context: Context, uri: Uri, password: String): NativeBankProbeResult }`. Map transport, `PdfFailure`, and `StatementProbeReason` only to fixed enum reasons; rethrow an explicit PDF password failure for the existing retry UI.

- [ ] **Step 1: Write failing pure-JVM tests** `api34RefusesNativeProbe`, `missingPositionedPageOrFlagRefusesAllPages`, `oneIncompleteCoordinatePageRefusesAllPages`, and `completeTwoPagePreviewPreservesOriginalPageOrder`. Construct invented `PdfTextPreview` values, asserting the exact enum reason or ordered 1-based `StatementProbePage` values.
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests '*StatementNativeProbeTransportTest' --console=plain`; expect unresolved symbols or failing assertions first.
- [ ] **Step 3: Implement** `prepare` with page count 1–160, exact `pages`/`positionedPages`/`positionedComplete` lengths, all flags true, nonempty positioned pages, `PdfTextReviewPolicy.supported(api)` and existing fragment bounds. No attempt to guess missing geometry.
- [ ] **Step 4: Implement** the suspending reader using `PdfTextReviewReader.read(context, uri, password, includePositions = true)`, transport, and `UniversalStatementReadOnlyProbe.inspect` only. Return a `NativeBankProbeResult.Blocked` for transport/probe failure, `Provisional` only for all-page successes, and `StatementPdfOpeningPasswordRequiredException` for a typed `PdfFailure.PASSWORD`; never call a store or source-profile admission here.
- [ ] **Step 5: Rerun** transport, geometry, Slice A probe and interpreter unit tests; commit `feat(statements): adapt isolated native PDF geometry to read only probe`.

### Task 2: Typed fallback refusals; no verification downgrade

**Files:**
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/documents/OfflineBankTableReader.kt` (only stable blocked-reason transport; do not rewrite Python extraction)
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/domain/statements/StatementKnownImportReasonPolicy.kt`
- Create: `app/src/test/java/com/kedevjimmy/pesatrack/domain/statements/StatementKnownImportReasonPolicyTest.kt`

**Interfaces:** `object StatementKnownImportReasonPolicy { fun classify(reasonCode: String): StatementKnownImportOutcome }`; `class StatementBankInspectionBlocked(val reasonCode: String) : IllegalArgumentException(...)` in `OfflineBankTableReader.kt` exposes a stable safe reason only, preserving the current user-facing failure copy. Accept exactly `unverified_header`, `unverified_columns`, `unverified_physical_table` as layout-only outcomes. `unverified_issuer` maps to `ISSUER_REJECTED`, account identity and wrong-account reasons to `ACCOUNT_REJECTED`, balance/source/total mismatches to `FINANCIAL_PROOF_REJECTED`, password to `PASSWORD_REJECTED`, and all unrecognized/absent reasons to `OTHER_FAILURE`.

- [ ] **Step 1: Write failing tests** `unsupportedTableLayoutCanOnlyStartReadOnlyProbe`, `unverifiedIssuerNeverTriggersFallback`, `accountBalanceTotalsPasswordAndUnknownErrorsNeverTriggerFallback`. Assert the existing `StatementFallbackProbeGate.mayRunReadOnlyProbe` decision for each typed outcome.
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests '*StatementKnownImportReasonPolicyTest' --console=plain`; expect failure before the classifier exists.
- [ ] **Step 3: Implement** the allowlisted mapping and typed rejected-status bridge. `unverified_issuer`, unknown account, page coverage, money mismatch and unexpected status must never be `UNSUPPORTED_LAYOUT`; do not infer outcome from exception text displayed to users. Preserve the current known-bank error message.
- [ ] **Step 4: Run** the new tests, `StatementFallbackProbeGateTest` and `StatementBankAdmissionTest`; commit `fix(statements): type bank layout fallback refusal outcomes`.

### Task 3: Explicit provisional preview in the existing import UI

**Files:**
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementUniversalProbePreview.kt` (isolated Compose preview card)
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/domain/statements/StatementProvisionalSelection.kt` (pure current-account/current-file display gate)
- Modify: `app/src/main/java/com/kedevjimmy/pesatrack/ui/screens/StatementImportScreens.kt` (picker launch, in-memory state and reset only)
- Test: `app/src/test/java/com/kedevjimmy/pesatrack/domain/statements/StatementProvisionalSelectionTest.kt`

**Interfaces:** `data class StatementProvisionalSelection(val accountId: String, val documentToken: Long, val result: NativeBankProbeResult)`; `object StatementProvisionalSelectionPolicy { fun visible(selection: StatementProvisionalSelection?, accountId: String, documentToken: Long): NativeBankProbeResult? }`; `@Composable internal fun StatementUniversalProbePreview(result: NativeBankProbeResult, onDismiss: () -> Unit)`. It shows only page/row counts and a prominent **Not saved — verification incomplete** state; no Save control. No recipient of `StatementProbeResult.Provisional` may call `StatementBankArchiveStore.save`.

- [ ] **Step 1: Write failing tests** `wrongAccountHidesPreviousProbe`, `newFileTokenHidesPreviousProbe`, and `currentAccountAndFilePreserveStatus`. Check exact `StatementProvisionalSelectionPolicy.visible` results without any Compose/Android instrumentation.
- [ ] **Step 2: Run** the focused tests; require failure before the new policy/helper exists.
- [ ] **Step 3: Add** a clearly labeled `Preview unfamiliar PDF (read-only)` action for a selected, confirmed Bank account, with private-data-safe blocked reasons for unsupported API, scanned PDF, incomplete geometry and unreconciled tables. On Android 34 and lower, display the unsupported-device reason without launching extraction. Launch the existing document picker and invoke the new reader off the main thread on Android 35+. If a *known* bank import produces a typed `UNSUPPORTED_LAYOUT` and passes `StatementFallbackProbeGate`, offer the native read-only fallback; for all other errors, preserve the existing refusal. Clear the provisional result whenever the file/account changes or the sheet closes.
- [ ] **Step 4: Keep** the Equity and M-Pesa PDF buttons, password retry, save callbacks and existing CSV path exactly as-is. Do not display a success/"Imported" receipt for a provisional preview and do not add an archive or Activity save route.
- [ ] **Step 5: Rerun** focused tests plus Kotlin compile/lint; commit `feat(statements): expose safe read only unknown PDF preview`.

### Task 4: Verified admission handoff and first-bank rollout gate

**Files:**
- Create: `app/src/main/java/com/kedevjimmy/pesatrack/domain/statements/StatementFallbackAdmissionPolicy.kt`
- Create: `app/src/test/java/com/kedevjimmy/pesatrack/domain/statements/StatementFallbackAdmissionPolicyTest.kt`
- Preserve: `StatementBankAdmission.kt`, `StatementBankArchiveStore.kt`, `StatementArchiveDb.kt`, `StatementBackupArchive.kt` unless an identified regression requires an owner-reviewed separate fix.

**Interfaces:** `object StatementFallbackAdmissionPolicy { fun verify(probe: StatementProbeResult, verifiedProof: StatementDocumentProof?, profile: StatementSourceProfile?, account: MoneyAccount, confirmedIds: Set<String>, providerKey: String?): StatementAdmissionResult }`. It returns blocked for absent proof, unsupported provider, unmatched ownership/issuer, inconsistent source legs or unsupported format; calls the existing `StatementBankAdmission.verify` for every candidate that has a separately obtained document proof.

- [ ] **Step 1: Write failing tests** `noProofNeverAdmitsEvenPerfectBalances`, `unknownIssuerNeverAdmits`, `wrongAccountNeverAdmits`, `oneMissingPrintedPageTotalNeverAdmits`, `completeIndependentProofDelegatesToBankAdmission`, and `mutatedProbeTablesAfterProofRefuses`. Use invented identities and amounts only.
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests '*StatementFallbackAdmissionPolicyTest' --console=plain`; expect red first.
- [ ] **Step 3: Implement** the thin fail-closed bridge; check candidate/accepted rows and legs exactly match and never manufacture `StatementDocumentProof` from the probe. Keep it disconnected from `save` until a real first-provider contract produces independently verified issuer, full account, currency, printed totals, opening/closing anchors and region coverage.
- [ ] **Step 4: Rerun** admission and existing source-coverage tests; commit `feat(statements): gate universal candidates at verified admission boundary`.
- [ ] **Step 5: Record the explicit rollout stop:** no bank besides the currently source-validated Equity profile is importable by this fallback. Before a first real unknown-bank auto archive, get a privacy-preserving locally verified sample/issuer contract, source-format tests, a Redmi approval run, and owner's separate default-enable acceptance. Add that first-bank contract in a follow-on plan rather than issuing a guessed verified-source receipt.

### Task 5: Regression, branch review and delivery

**Files:**
- Modify: `CURRENT_STATE.md` (explain provisional live preview, no unknown-bank auto archive yet)
- Preserve: release/signing workflows, schema version, backup manifests, Activity/financial classification.

- [ ] **Step 1: Review the exact diff** for private document values, alternate save paths, API 34 behavior, cancellation, stale preview state, and permissions. Recheck concurrent PRs that touch Statements or Equity before editing those files.
- [ ] **Step 2: Run** focused tests and `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin --console=plain`; run existing Python contract checks if touched. Do not run heavy device instrumentation without new task authorization.
- [ ] **Step 3: Inspect** existing `StatementBankArchiveIntegrationTest`, `StatementArchiveBackupIntegrationTest`, and `StatementSourceCoveragePolicyTest` evidence for idempotency, overlap and restore regressions; no new schema/migration is planned, so no replacement backup table is added.
- [ ] **Step 4: Open** one isolated draft PR, wait for exact-HEAD CI and independent code review, and report a safe native preview versus unimplemented full-document proof honestly. Do not merge/release before owner and Redmi gate.

## Follow-on stage (not enabled by this plan)

Slice B's automatic archive of an unfamiliar institution requires a **validated real issuer document contract** before the above policy may be used to save. The first pilot must verify every source page and region, full account/issuer binding, printed totals and beginning/end balances, and then pass an on-device trial. Once those gates pass, the existing atomic archive should save on explicit file-selection intent without a second Save step and show a verified receipt. Preserve duplicate source hashing, existing overlap policy and encrypted backup. Unrecognized bank formats remain preview-only until that evidence exists; OCR remains a separate Slice C.
