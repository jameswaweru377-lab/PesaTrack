# N2 — Statement feature-admission gate

**Current decision: HOLD NEW STATEMENT FEATURES.** R1 physical-device acceptance
is `NOT RUN`, and the T1 persistence suite is not yet a required, executed
release-candidate gate. A planned test, green JVM build or uploaded preview APK
does not lift this hold.

This is a **development and release decision rule**, not an app feature or a
GitHub Action that automatically blocks PRs. The owner and reviewers must apply
it when deciding scope, approval and release readiness. It does not cancel or
rewrite another chat's existing draft PR.

## What stays on hold

- New bank/provider statement importers or broader unsupported-PDF admission.
- New automatic assurance/promotion paths, including treating a limited source
  as fully verified without the required independent evidence.
- New automatic posting, category projection, account matching or combined
  financial totals derived from statement rows.
- New statement archive formats, OCR or parser expansions presented as ready
  for general use before the gates below pass.

## What may continue safely

Reproduced defects in **already supported** M-Pesa and Equity import, existing
source visibility, crash recovery, privacy/security fixes, deterministic tests,
diagnostics, performance measurement and documentation. These still need a
focused PR, exact-head checks and applicable negative/replay/backup tests.
They must preserve current money meaning and assurance labels. A proposed
change that reclassifies money, raises an assurance tier or admits a new source
is **an expansion**, even if described as a bug fix: it needs its own explicit
owner decision and the appropriate financial evidence. Active draft work stays
draft until it meets its own gates; this checklist alone authorizes no merge.

## Evidence required before new features resume

Record a **single final candidate's full source SHA** and link all evidence.
Any `FAIL`, `NOT RUN`, missing link, or different source SHA keeps admission
**BLOCKED**. Re-run changed journeys if a later candidate changes relevant code.

| Gate | Required result | Current result |
|---|---|---|
| R1 signed physical-phone test | All five journeys and preflight PASS on a named device, using only disposable invented records | **NOT RUN** |
| T1 Android persistence emulator | An **executed** exact-SHA run with migration, backup/restore, ingestion atomicity, replay, debt and money-aggregate checks PASS | **NOT VERIFIED** |
| Candidate CI and artifact | Exact-SHA Android JVM, lint, build and relevant privacy/security checks PASS; signed identity and version continuity verified | Not a substitute for R1/T1 |
| Scope approval | Owner reviews the proposed *new* statement capability and its negative, duplicate and financial-correctness coverage after the gates above | **NOT GRANTED BY THIS CHECKLIST** |

Use [the R1 device record](R1_DEVICE_ACCEPTANCE_TEMPLATE.md) for M-Pesa PDF
import/replay, historical Equity PDF source, encrypted backup and legacy restore,
app lock/recovery, and release-signed in-place upgrade. R1 requires the actual
physical phone model, Android version, SHA, outcomes and safe evidence. The
T1 run must **execute**, rather than merely compile, its Android instrumentation
tests. The complete test and blocker names belong in the canonical T1 issue
and CI run, not in an unsupported claim in this checklist.

## Decision record for the next candidate

| Evidence | Value |
|---|---|
| Full candidate SHA | NOT RECORDED |
| R1 signed physical-device acceptance record / PASS | NOT RUN |
| T1 executed emulator run / PASS | NOT VERIFIED |
| Exact-SHA Android CI and signing continuity | NOT RECORDED |
| Proposed new statement capability and financial safety review | NOT PROPOSED |
| Owner's dated resume decision | **HOLD** |

References: [PesaTrack release process](../RELEASE_PROCESS.md),
[current state](../../CURRENT_STATE.md), and the N2 canonical tracker
[issue #537](https://github.com/jameswaweru377-lab/PesaTrack/issues/537).
