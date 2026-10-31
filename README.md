<div align="center">
  <img src="docs/assets/pesatrack-mark.svg" alt="PesaTrack P logo" width="108" height="108">

  # PesaTrack

  **Your money, better days.**

  A privacy-first Android money companion built for everyday life in Kenya.

  [![Android](https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/about/versions/nougat)
  ![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)
  ![Local-first](https://img.shields.io/badge/Data-local--first-6D28D9)
  ![Status](https://img.shields.io/badge/Status-pre--release-F59E0B)

  [**Download a preview**](https://github.com/jameswaweru377-lab/PesaTrack/releases/tag/ci-apk) · [Features](#what-you-can-do) · [Privacy](#privacy-by-design) · [Development](#project-and-development)
</div>

---

PesaTrack helps you understand money coming in, going out, and moving between your accounts. It builds a personal ledger from supported financial-message evidence and your own entries, while keeping imported statement records available for separate verification and review—without making a cloud account the centre of your finances.

Saved statement archives are separate evidence: saving a bank PDF does not automatically add its rows to Activity or to verified cash-flow totals. Eligible imports require their own review and verification.

**Accuracy comes first:** when a transaction, balance, or document cannot be verified confidently, PesaTrack asks for review or shows the limitation instead of inventing a number.

## What you can do

| | Feature | What it helps with |
| --- | --- | --- |
| 💸 | **Track activity** | Review income, spending, transfers, fees, refunds, and transaction details from supported sources. |
| 🧾 | **Import statements** | Review supported M-Pesa and bank statement files with duplicate and verification safeguards. Unsupported or incomplete formats are not silently accepted. |
| 📊 | **Understand your cash flow** | Explore Money In, Money Out, categories, account context, and reports based on recorded evidence. |
| 🎯 | **Plan ahead** | Organize budgets, bills, saving goals, and payday context without treating expected income as money already received. |
| 🤝 | **Keep track of debts** | Distinguish loans and repayments from ordinary purchases and transfers, with user confirmation where needed. |
| 🔐 | **Keep control of your data** | Use local storage, optional app lock, encrypted user-created backups, and privacy controls. |

### Four places to find what matters

**Home** for the overview · **Activity** for your ledger · **Plan** for commitments and goals · **More** for accounts, statements, reports and settings.

> **Screenshots:** A gallery of real, privacy-safe screens will be added after device capture and review. No personal transactions, account numbers, or private statement details will be published as examples.

## Privacy by design

- **Local-first:** the on-device SQLite data is the source of truth for personal financial records.
- **Optional access:** historical SMS and financial-notification access depend on the permissions you choose to grant.
- **No raw financial-message uploads:** optional online merchant lookup is off by default and uses a sanitized business descriptor when enabled.
- **Your exports:** backups are passphrase-encrypted; exported reports and files need the same care as other financial documents.

An app-lock PIN protects access to the app's interface; it **does not encrypt the ordinary local databases**. See the [security policy](SECURITY.md) and [pre-release privacy-policy source](docs/PRIVACY_POLICY.md) for details.

## Try the Android preview

1. Open the [PesaTrack rolling preview release](https://github.com/jameswaweru377-lab/PesaTrack/releases/tag/ci-apk).
2. Choose **`PesaTrack-Full.apk`** for a new installation, or **`PesaTrack-Update.apk`** for an existing compatible installation. The release process publishes two names for the same signed build; the name alone does not guarantee an in-place update.
3. Review Android's installation and permission prompts, then set up only the financial sources you want to use.

**Before updating:** protect your data with a verified backup. A phone running an older debug-signed APK may not accept the release-signed update. Do not uninstall or erase app data just to force an update. See the [release and signing guide](docs/RELEASE_PROCESS.md).

The preview targets **Android 7.0+ on supported ARM64 devices** and remains under active development. Device compatibility, banking-message coverage, and statement formats vary. PesaTrack is not a bank, a payment app, or a substitute for original provider records.

## Project and development

PesaTrack is built primarily with **Kotlin, Jetpack Compose, and SQLite**. Supported PDF processing runs locally, and an optional enrichment Worker provides non-authoritative merchant hints. Personal financial decisions are made by deterministic local logic and explicit user review—not by an online chatbot.

- [Product charter](docs/PRODUCT_CHARTER.md) — what PesaTrack is building and what it deliberately avoids.
- [Current engineering state](CURRENT_STATE.md) — engineering progress, limitations, and pending acceptance.
- [Development tracker](docs/tracker/README.md) — tasks, ownership, and review rules.
- [Release process](docs/RELEASE_PROCESS.md) — signed APKs and safe update expectations.

Found a bug or have an idea? Read [Contributing](CONTRIBUTING.md). For a vulnerability, use the private-reporting instructions in [Security](SECURITY.md). Community spaces follow our [Code of Conduct](CODE_OF_CONDUCT.md).

## Source code and rights

**Publicly visible does not mean open-source licensed.** PesaTrack's original source code is published for inspection, but no general permission to reuse, modify, redistribute, or commercialize it is granted. See [LICENSE](LICENSE) for the proprietary copyright notice and third-party exceptions.

Copyright © 2026 James Waweru and respective rights holders. All rights reserved.
