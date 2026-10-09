# Audit integration acceptance

This branch combines independently reviewed audit changes for verification. It is not merged or released. The notification-source approval implementation is excluded because its source publication was rejected by automatic review; its local review package and PR297 describe the pending action.

## Required automated evidence

- Full Kotlin JVM tests, lint and debug APK build for the exact combined SHA.
- Python laboratory and financial contract tests regenerated from invented fixtures, with no SMS inbox or personal ledger inputs.
- Android connected tests on API24,35,36, including populated v33-to-v34 migration, repayment chronology/unknown balances/atomic correction, affirmative refund provenance, shared budget totals, recovery failure/retry, resource limits, history cancellation and read-error handling.
- Original-state/manual corrections, exclusions and locks survive replay; unknown or estimated-zero debt does not become settled without observed confirmation; representable signed money boundaries remain available, unrepresentable totals stay unavailable.
- Existing native modern backup/PIN derivation and compatible API24 SHA256 derivation pass the fixed independent vectors and encrypted fixture; no encryption-format or parameter downgrade.
- Informational invented 3,000/9,000-row persistence/replay/query measurements retained with commit SHA. Measurements have deterministic financial assertions and no timing threshold.
- Release preflight verifies its immutable workflow SHA before signing; PR verification uses no signing secrets. Wrapper and distribution checksums verified.

## Integration decisions

Keep schema34 debt provenance migration and recovery-before-database-access callback together. Keep the newest affirmative refund guard with checked totals and budget readers. Keep new Worker resource tests in both normal and release verification. Notification trust preferences are not integrated until publication approval and its complete tests pass.

## Limits and external gates

Old backup schemas are deliberately refused before mutation until a staged migration is implemented; current live database migration is tested separately. No real data was inspected/exported. No physical-phone, TalkBack, battery, permission-revocation, process-death or power-loss result is claimed.

Main must require Android CI build/instrumentation checks and block unreviewed direct pushes; authenticated administrator access is currently unavailable. Signing needs the existing repository keys and an explicitly chosen tested release commit. No replacement keys, merge, store upload or APK publication occurred for the audit fixes.

The owned-transfer remembered-rule feature in issue286 remains a separate accounting feature after these safety gates. Its approved acceptance requires exact confirmed-owned evidence, explicit preview/consent, atomic provenance and reversible automatic-only undo; ordinary name/category matching is insufficient.
