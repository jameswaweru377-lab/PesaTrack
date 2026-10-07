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
