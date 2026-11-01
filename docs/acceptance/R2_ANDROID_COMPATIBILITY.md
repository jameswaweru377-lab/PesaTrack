# R2 — Android ABI and embedded Python decision record

Status: **ARM64-only beta documented; expanded device support not approved**.
Last source check: 1 November 2026, `main@d4cc31f`.

## Verified installation boundary

`app/build.gradle.kts` sets `minSdk = 24` and restricts native packaging to
`abiFilters += listOf("arm64-v8a")`. An install needs **both** an Android 7.0+
operating system and a system supporting the `arm64-v8a` ABI. An ARM64-capable
processor running a **32-bit-only Android image** does not meet this gate.

The current APK cannot be installed on devices exposing only `armeabi-v7a`
(32-bit ARM), only x86/x86_64 without usable ARM64 compatibility, or Android
versions below API 24. These are compatibility statements, **not** an estimate
of how many Kenyan phones or current PesaTrack users are affected.

The Android build uses Chaquopy plugin **17.1.0**, embeds **Python 3.13**, and
explicitly pins `pdfplumber==0.11.9` for offline statement extraction. Its
transitive Python dependencies are not individually pinned in
`app/build.gradle.kts`; a release-build dependency inventory is needed before
claiming a complete reproducible Python package set. Financial PDFs remain
on the device; no online processing is introduced.

The successful rolling `ci-apk` publication for `main@d4cc31f` contained two
signed APK assets, each **51,350,983 bytes** (GitHub release asset metadata).
The publisher verifies the two names refer to identical APK bytes; they are
**not** independent ABI builds. No Python-free, ABI-split, or ARMv7 scratch
release was built in this review, so **Chaquopy's isolated byte contribution
is unknown**, not 51 MB.

## Options and consequences

| Option | Size impact | Data/correctness and rollout risk |
| --- | --- | --- |
| **Keep and document ARM64-only** (current beta recommendation) | No build change; retain measured APK size until next build | Existing signed-update path is unchanged; 32-bit-only devices stay excluded. |
| Add `armeabi-v7a` alongside ARM64 | Unknown increase until a dual-ABI signed scratch build; extra Python/native libraries may be substantial | Confirm Chaquopy 17.1.0/Python 3.13 wheel support first; test PDF extraction, memory, app lock, encrypted restore and signed upgrade on real ARMv7/API24 hardware. |
| Replace embedded Python with native parsing | Unknown potential decrease until an equivalent scratch build is measured | Large statement-parser rewrite; complete-page verification, duplicates and money totals must remain identical on offline fixtures and device. |

Do not remove Android 7 support solely to improve a scanner score. ABI
support and Android security-patch policy are different decisions.

## Evidence required for a wider install promise

1. Count *actual* installs and refused installs by supported ABI, Android API
   version and model/OS. Ask beta testers or use voluntary on-device
   diagnostics; do **not** add telemetry or upload personal information.
2. Identify any intended beta tester with ARMv7-only or 32-bit-only Android.
3. Record acceptable signed full/update APK size and low-storage constraints.
4. For any proposed ABI expansion, compare signed APK bytes and run the
   synthetic M-Pesa/Equity, backup/restore, lock and update acceptance journeys
   on each newly supported ABI. Preserve signing identity and versionCode.

**Decision gate:** keep current ARM64-only distribution until these counts
and a tested alternative exist. Wider hardware acceptance remains **NOT
VERIFIED**; no real device was used for this record.
