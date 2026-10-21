# Security Policy

PesaTrack handles sensitive personal-finance data. Security and privacy regressions are treated as release blockers.

## Supported code

PesaTrack is currently pre-release. Security fixes are made against the current `main` branch and the latest published test build. Older debug APKs and historical branches are not maintained.

## Reporting a vulnerability

Please do not publish credentials, raw financial messages, account details, proof-of-concept exploits or other sensitive material in a public issue.

Prefer GitHub's private vulnerability reporting / Security Advisory flow for this repository when it is available. If private reporting is unavailable, open a minimal public issue asking the repository owner for a private contact channel, without including exploit details.

Include:
- affected commit or APK version;
- affected screen/component;
- reproduction steps using synthetic data;
- expected versus actual behavior;
- impact and any suggested mitigation.

## Sensitive-data rules

Contributions must not log, upload, commit or include in fixtures:
- real SMS bodies;
- phone or account numbers;
- transaction/reference codes;
- balances or personally identifying financial records;
- API tokens, signing keys or credentials.

Security fixes should include a regression test when practical.

## Local storage threat model

PesaTrack's ordinary primary transaction database and separate statement-archive
database use Android app-private SQLite storage. The optional app PIN limits app
UI access but is **not** full database encryption; physical/device compromise,
root access or insecure exports may still expose data. Encrypted user-selected
backups use a separate mechanism and must not be confused with SQLite encryption.
Any encryption-at-rest migration needs independent backup, restore and lossless
migration tests before it can be rolled out.

## Phase 1 safeguards and verification

- Importing saved M-Pesa PDFs validates the 8 MB file-size boundary and PDF header
  before passing bytes to the offline Python reader. A header check rejects obvious
  renamed/unexpected formats, but is **not** malware scanning or PDF authenticity.
- Android CI executes `python3 tools/security_scan.py` before compilation, enforcing
  a two-argument sanitized Log policy, manifest invariants, and the offline
  parser's upfront file-admission hook. Synthetic unit tests guard the scanner.
- Settings → Privacy → Manage saved statements offers per-document deletion only.
  The selected archive and its row-specific labels are removed atomically,
  leaving canonical Activity transactions, confirmed loans, cross-statement rules
  and user-controlled backups unmodified.
- Dependabot groups routine minor/patch version updates monthly to reduce CI
  churn. Security advisories and vulnerability coverage depend on the GitHub
  repository's enabled security features; these controls do not prove all
  dependencies free of known vulnerabilities.

## Safe diagnostic logging

Log generic failure categories only for financial-message ingestion, transaction
screens, PDF import and other sensitive flows. Never pass unfiltered exceptions
or raw stack traces into Android logs when exception messages could contain SMS,
transaction or account data. Developer regression tests must use synthetic data.

## Public release blockers

Before general availability, verify official developer/privacy contact details,
the ODPC/controller assessment, Google Play SMS permission declaration,
data-retention/deletion instructions, actual network destinations and third-party
SDKs. The current Settings Terms and Privacy pages are clearly identified as
pre-release drafts until formally reviewed and versioned.
