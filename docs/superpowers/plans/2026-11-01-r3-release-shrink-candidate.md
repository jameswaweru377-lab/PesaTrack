# R3: measured release R8 experiment

Source finding: R3 (P1). Tracker: issue #548, CHAT-AUDIT-R3.
Starting main: `bd5562fd941d56ac766054d16f5ab51956b977e5`.

## Intended change

- Turn on R8 minification and resource shrinking in the `release` build type.
  Keep `debug` unchanged and preserve package ID, API 24, ARM64, Python 3.13,
  signer, version code, backup schema and all financial semantics.
- Begin with standard Android optimized keep defaults and reflective metadata.
  Gson is currently used with `JsonParser`/`JsonObject`, not app-model
  reflection; Kotlin callers reference Chaquopy APIs directly. Compose and
  libraries provide consumer rules. Do not add broad guessed keep rules.
- Build an unsigned release at the PR's exact pre-change main SHA in a
  detached worktree, then the candidate head in the same CI environment.
  Verify package metadata and that every Chaquopy asset survives; output a
  JSON byte-size comparison, no unsigned APK artifact.

## Tests and gates

1. Test-first source guards: `tools/test_r3_release_shrinking.py` fails on
   the pre-change release configuration and absent comparison workflow.
2. Implement shrink configuration, minimal ProGuard file, comparison script
   and PR-only GitHub workflow. Run local tool/tracker tests, security scan,
   YAML parse and diff checks.
   Local `./gradlew :app:assembleRelease --stacktrace` reached task creation
   but failed before compilation: this container's Java 17 runtime has no
   `javac`/`JAVA_COMPILER` capability. No local APK bytes were measured.
3. Open **DRAFT** PR, collect exact-head Android CI, PR Secrets and R3
   unsigned size run. Investigate any R8 missing classes, packaging drift,
   archive parity failure or release build failure before reporting a result.
4. **Stop before merge**. The existing `main` workflow immediately publishes
   signed rolling APKs; an unsigned compile cannot prove runtime safety.
   Arrange a protected same-signature signed candidate and R1/T1 acceptance
   recorded in `docs/acceptance/R3_RELEASE_SHRINKING_GATE.md`.

Rollback: keep the previous compatible signed APK and revert the isolated
R3 PR before deploying, if acceptance fails. Never downgrade a user's live
database or uninstall the app to test a signer mismatch. No phone was
available in this environment; device status is NOT RUN.
