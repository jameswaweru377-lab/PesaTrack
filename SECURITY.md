# Security Policy

PesaTrack processes sensitive personal-finance information. We take reports of security and privacy weaknesses seriously and aim to fix credible risks without exposing users' data.

## Supported versions

PesaTrack is currently **pre-release**. Security fixes target the current `main` branch and the latest supported preview build. Historical branches and older debug APKs do not receive individual security patches.

## Report a vulnerability privately

1. Prefer [GitHub's private vulnerability-reporting flow](https://github.com/jameswaweru377-lab/PesaTrack/security/advisories/new) **if it is enabled for this repository**.
2. If private reporting is unavailable, create only a **minimal public issue requesting a private reporting channel**. Do not include the vulnerability, exploit steps, or affected user information in that issue.
3. Where possible, include the affected commit or APK version, impact, clear reproduction steps using synthetic data, and a suggested mitigation.

Please allow time for the maintainer to investigate and coordinate a fix before public disclosure. No fixed response-time guarantee or paid bug bounty is currently offered.

**Never upload real financial SMS, statement PDFs, transaction codes, balances, phone or account numbers, authentication material, signing keys, tokens, or sensitive logs.** Use invented fixtures and redact diagnostic output.

## Security and privacy boundaries

- The ordinary transaction and saved-statement databases use **Android app-private SQLite storage**. The optional app PIN controls UI access; **it is not full database encryption**. A compromised, rooted, or unlocked device and unsafe exports can expose data.
- User-selected backup files are separately **passphrase-encrypted**. Keep the passphrase and exported backups secure; local app deletion does not delete copies stored elsewhere.
- Raw financial-message bodies are not sent to optional merchant enrichment. Online lookup is off by default, and its sanitized descriptors are not accounting authority.
- Financial importers must refuse incomplete, unsupported, or unreconciled evidence rather than silently creating transactions.
- Security checks and dependency alerts are useful safeguards, not proof that the application or any dependency is vulnerability-free. See [security scanning](docs/SECURITY_SCANNING.md).

## Coordinated handling

We assess reports by potential impact on confidentiality, integrity, availability, financial accuracy, and user-data recovery. Credible fixes should include regression coverage where practical. Publication details and timing depend on the issue and affected users; please avoid sharing exploit details before coordination.

Before general public release, verified privacy/support contacts, legal and permission declarations, device validation, and applicable compliance reviews still need to be completed. See the [release-readiness gate](docs/LEGAL_RELEASE_GATE.md).
