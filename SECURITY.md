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
