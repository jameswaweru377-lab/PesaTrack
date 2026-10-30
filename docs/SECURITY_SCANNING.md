# Optional security scanning (performance-first)

PesaTrack keeps the current `Android CI` build, signing, publication and opt-in
instrumentation workflows unchanged. Security scanners run in **separate** GitHub
Actions workflows so they do not add build steps or dependencies to Android CI.

| Workflow | Trigger | What it does | Impact |
| --- | --- | --- | --- |
| `Security - PR Secrets` | Open/update PR | Checksum-verified Gitleaks CLI scans every commit between PR base and head | Independent job; 8-minute timeout |
| `Security - Kotlin CodeQL (Weekly or Manual)` | Monday 02:17 UTC or manually | Builds Kotlin for CodeQL analysis and uploads Code Scanning findings | Independent job; 30-minute timeout |
| `Security - Signed APK MobSF (Manual)` | Manual only | Downloads the current *release-signed* `ci-apk` `PesaTrack-Full.apk` and runs MobSF static analysis in a local-only Docker API | Independent job; 35-minute timeout; does not rebuild or publish an APK |

These timeouts are safety limits, **not measured durations**. All three consume
GitHub Actions minutes when executed. No scan automatically edits app code,
approves a PR, merges branches or changes the release APK. New checks should not
be made required in branch protection without a separate owner decision.

## Running scans

1. Gitleaks runs automatically on a new or updated PR, using an immutable
   version and checksum-verified executable. It has no access to release-signing
   secrets. It prints fully redacted detections to the job log and does not
   comment on the PR or upload findings; the explicit base-to-head Git range
   also covers PRs with more than 30 commits.
2. To run CodeQL on demand: **Actions → Security - Kotlin CodeQL → Run workflow**.
   On the default branch it also runs every Monday. Review results in
   **Security → Code scanning**. It uses real Kotlin compilation; an Android
   dependency or Gradle failure is a scan failure, not proof of security.
3. To inspect the current signed APK: **Actions → Security - Signed APK MobSF →
   Run workflow**. The scanner uses the rolling public APK already released,
   not a fresh build from the workflow's selected branch. Compare the SHA-256
   printed in the job summary with the APK being evaluated. The detailed JSON
   artifact is **disabled by default**, because this is a public repository:
   anyone with repository read access may be able to download findings. Only
   enable `include_detailed_report` if you understand that disclosure risk;
   opted-in reports are kept for three days. For confidential findings, run
   MobSF locally instead of uploading its full report to public Actions. Every
   successful scan shows only aggregated high/warning/hotspot counts and MobSF's
   heuristic score in the Actions job summary, not strings or finding details.

The MobSF container is pinned to an immutable image digest observed on
30 October 2026. Its port binds to loopback only, and it receives a temporary
per-run API key. Do not send live financial SMS, statement PDFs, local SQLite
databases, backups, signing keys or other personal data to these scanners.

## Reviewing findings

- Confirm an actual affected security boundary, not just a scanner warning.
- Give each confirmed issue the smallest fix and a regression test on a
  dedicated branch. Keep user financial data local and preserve backup format,
  package identity, signing and Android compatibility.
- For a leaked credential, rotate/revoke the credential. Removing it from the
  latest commit alone does not remove it from Git history.
- MobSF and CodeQL can return false positives or miss vulnerabilities; a clean
  scan is not a penetration test or device-acceptance result.
- Do not make raw MobSF findings publicly downloadable before reviewing their
  sensitivity; the manual workflow's report upload is explicitly opt-in.

## Configuration test

With PyYAML available, run:

```sh
python3 -m unittest tools.test_security_ci_config -v
```

This tests triggers, isolation, permissions, source-build mode, and reuse of
the published APK. A live GitHub Actions run is still required to verify third-
party scanner behavior. No Android instrumentation is enabled by these files.
