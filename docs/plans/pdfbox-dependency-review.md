# PDFBox Android dependency review — 13 October 2026

This is a release-gate assessment, not a claim that an untrusted PDF parser is
vulnerability-free. PDFBox remains an explicit opt-in statement review path.

## Verified dependency facts

The [published Maven POM](https://repo.maven.apache.org/maven2/com/tom-roush/pdfbox-android/2.0.27.0/pdfbox-android-2.0.27.0.pom)
declares Apache-2.0 licensing and `bcprov-jdk15to18`, `bcpkix-jdk15to18` and
`bcutil-jdk15to18`, all version 1.72. No dependency version changes are made by
the financial-proof PR. The port's [release list](https://github.com/TomRoush/PdfBox-Android/releases)
still lists 2.0.27.0 as latest; do not equate that with current upstream PDFBox.

[Apache's security page](https://pdfbox.apache.org/security.html) lists
CVE-2026-23907 and CVE-2026-33929 for the **ExtractEmbeddedFiles example**, not
core PDFBox. PesaTrack calls `PDDocument.load` and text extraction, not that
example and not embedded-file export. This does not establish absence of other
malformed-document or dependency problems in the Android port.

Bouncy Castle 1.72 has published advisories, including
[PEMParser denial of service](https://github.com/bcgit/bc-java/wiki/CVE%E2%80%902023%E2%80%9033202).
[Its 1.78 release security notes](https://www.bouncycastle.org/resources/java-release-1-78-1-is-now-available-for-download/)
also describe fixes for crafted EC certificates and Ed25519 verification.
PesaTrack does not request PEM parsing, certificate/signature verification,
embedded-file extraction or TLS through PDFBox. Reachability through an
untrusted encrypted PDF has **not** been independently proven absent. Do not
blindly upgrade the port's crypto dependencies: binary/API compatibility and
Android password-encryption behavior must be checked against the selected
patched version. A maintenance plan remains required before default enablement.

## Existing containment and processing bounds

- No document upload, internet permission in the isolated UID, ledger access,
  OCR, image rendering, attachment export or new app permission.
- Original PDF: 8 MiB; 160 pages; 500,000 extracted characters; 32,000 words;
  5,000 statement rows. Parser runs off the UI thread. The existing 90-second
  reader timeout unbinds and kills the dedicated isolated process.
- Original/decrypted text and positions remain transient. Temporary input is
  deleted after descriptor transfer and cleaned up on cancellation. No
  decrypted PDF is written. Failures use fixed messages and bounded page
  numbers; proof IPC contains six integers, never amounts or identifiers.
- Input size does not bound decompressed PDF objects. Malformed PDFs may still
  exhaust the isolated worker's memory; the UI must treat worker death as a
  failed read, never accept partial output.
- App code has no raw document logging. Audit of all upstream Logcat paths and
  malformed encrypted-document behavior is still required before default use.

## Size and device measurements

The existing main APK at `498eb58a299380efab2cbd866320fa67ab176214` was published
by successful Android CI run `38433923200`. GitHub release metadata reports
**32,594,099 bytes** for `PesaTrack-debug.apk`. This is a baseline including
PDFBox already. It is not a measured dependency-only delta from a pre-PDFBox
APK. Measure final publication against that baseline; compare pre-PDFBox
artifacts separately before asserting library cost.

The synthetic 26-page/881-row JVM test checks bounded document reconstruction
and exact totals. It is not a PDF-decoding benchmark or a Redmi peak-memory/
latency measurement. Encrypted Android tests remain compile-only under the
user's instrumentation pause. Physical-device extraction, APK impact, upstream
logging and patched-crypto compatibility remain release gates. Keep the real
PDF/password local; collect only counts, fixed failure categories and timings.
