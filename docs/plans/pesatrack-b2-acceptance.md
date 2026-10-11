# B2 acceptance evidence

Base: d5e90b0153d77f62d8e549e199e74abf8adc37a1 (main #340). One cohesive B2 branch.

- Pure Kotlin/JUnit policy validation: 25 tests passed locally (name, identity and owned-transfer policy). Regression sensitivity check against the original name implementation produced the expected two failures for reordered names and discarded extra tokens. This check happened after implementation because the portable toolchain became available later; it is not claimed as a pre-edit red run.
- Android database/UI tests authored: explicit ownership migration and no backfill; rejection/disable/revision changes; atomic account creation rollback; supported older encrypted backups and current roundtrip; matching stale snapshots, protected corrections, twins, repeated decisions; rejection/confirmation through canonical ingestion replay; observed balance anchors and correct earlier reconstruction; staggered settlement refused; decision persistence rollback; actual Compose stale refresh and distinct confirm/reject/defer controls.
- Timing limit: only identical recorded timestamps can be confirmed in this bounded adapter. Staggered settlement needs account-leg timing support and remains separate. Snapshot reconstruction before confirmation is deliberately uncertain; confirming a real transfer can improve historical reconstruction, while native observed balances must stay unchanged.
- Local combined Gradle attempt initially failed before compilation because the sandbox temporary path broke JDK Unix sockets. A local TCP fallback allowed startup; dependency acquisition was slow. Exact final full-build outcome remains pending.
- CI required: full unit/lint/APK/instrumentation compilation, Worker and Python financial contracts, plus actual emulator instrumentation in the same requested B2 PR workflow. Pending exact-head evidence; do not infer success from the baseline or from authored tests.
- PDF API/dependency research only. CSV preview/import remains available. No text-PDF extractor, scanned OCR, password handling or payslip-deposit logic shipped.
- Physical Redmi, real bank layouts, password-protected PDFs, scanned inputs and wider release acceptance remain unverified.

Independent source review: all important findings resolved (native balance preservation, reference meaning, timing and staggered-twin ambiguity). Approved conditional on exact-head CI/emulator success. Local Python laboratory: 30 tests passed; Worker contract/resource tests passed. Generated 2,432 invented monetary variants and 232 contract cases for full verification.

PDF sources: [Android PdfRenderer.Page text API](https://developer.android.com/reference/android/graphics/pdf/PdfRenderer.Page); [PdfBox-Android upstream](https://github.com/TomRoush/PdfBox-Android). Native extraction's API35 requirement does not cover the app's API24 minimum. A third-party parser requires separate layout/memory and dependency review.
