# Complete-owned-identity transfer-review guard implementation

Date: 2026-11-08
Design: `docs/superpowers/specs/2026-11-08-full-owned-identity-review-guard-design.md`

1. Establish actual boundary and reproduce with focused synthetic tests
   in `StatementLegacyReviewIdentityPolicyTest` and
   `TransferReconciliationPolicyTest`. Commit test-only RED and verify CI
   fails for the intended assertions, not environment/toolchain errors.
2. Tighten `StatementOwnedAccountUniquenessPolicy.unambiguous`: return
   only records whose type/full identifier normalized successfully and
   whose normalized identity is not shared by multiple owned account IDs.
   Do not modify account stores, statement archives, posting, or totals.
3. Update `CURRENT_STATE.md` and this design note. Verify final exact
   PR head Android CI passes JVM tests, lint, debug APK and security checks.
4. Obtain an independent read-only code review against the diff, address
   any findings, and merge only if checks and latest base are safe.
   Verify merged `main` workflow and rolling Full + Update APK assets.
5. Record merge and test evidence on #601. N2 posting remains on HOLD.
