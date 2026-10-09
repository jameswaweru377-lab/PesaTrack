# Release gates

Both verification jobs and signing check out the immutable workflow commit SHA. Signing requires successful JVM/lint/debug/laboratory and Android emulator jobs for that same SHA. Invented corpora are generated and required before tests; no real messages or ledger inputs are used.

The checked-in official Gradle 9.5.0 wrapper has SHA256 `497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7`. Its distribution checksum is pinned to `553c78f50dafcd54d65b9a444649057857469edf836431389695608536d6b746`. Sources: https://gradle.org/release-checksums/ and https://docs.gradle.org/9.5.0/userguide/gradle_wrapper.html .

`gradlew.bat --version` was executed on Windows with Java 17 and reported Gradle 9.5.0. Android builds require an installed SDK or the CI runners.

Pull requests changing release infrastructure run verification without signing. Manual release runs verify first, then require the existing repository signing secrets. Artifacts include SOURCE_SHA.txt and checksums; signature checks remain required. This change does not publish to a store, merge code, create replacement signing keys, or claim that a signed release has been tested on a physical phone.

Repository administration must require Android CI build and instrumentation checks on main and prevent direct pushes. Signed artifacts and physical-device/store checks remain external gates until their actual evidence is available.
