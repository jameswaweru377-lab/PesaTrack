# PesaTrack Privacy Policy

_Effective: 7 October 2026 (pre-release text; legal review and verified operator contact required before public publication)_

PesaTrack is a local-first personal-finance application. Its purpose is to help you understand financial activity from supported SMS and notification evidence, manual entries and settings you provide.

## Data processed on your device

Depending on the features you choose to enable, PesaTrack can process:
- financial SMS history you explicitly permit the app to read;
- financial notifications you explicitly allow the notification-listener service to access;
- imported statement cash rows and statement-only archives you explicitly review and save;
- transaction, account, debt, salary, budget, bill and saving-goal information derived from that evidence;
- manual corrections, categories and settings you enter;
- optional financial-health context that you enter yourself, including accessible
  emergency savings, essential monthly costs and required debt repayments;
- app-lock and encrypted-backup settings;
- optional voice-entry transcripts you confirm when using on-device speech recognition.

Financial documents and transaction descriptions may contain details about counterparties, including people's names and phone numbers. A saved PDF statement archive is separate from the canonical Activity ledger and should not automatically create debts or additional cash movements.

The local PesaTrack database on your device is the primary source of truth.

Financial-health context is saved in app-private local preferences, included in
user-created encrypted backups, and never added to the transaction ledger. Those
user-entered amounts need reconfirmation after 30 days to influence an assessment.
PesaTrack's financial-health guidance is an indicative local calculation, not
a credit score or a standardized assessment.

Older versions may have stored shopping-list preferences locally or in encrypted
backups. This version no longer reads or updates that retired feature. Updating
the app does not silently erase existing device preferences or backup files.

## SMS permission

Historical SMS import is optional. If you grant Android's SMS permission, PesaTrack reads the financial-message history you choose so it can create and reconcile your local ledger.

PesaTrack is designed to ignore unrelated messages rather than turn them into financial records. Full SMS bodies are not sent to the optional online merchant lookup service.

You can use PesaTrack without granting historical SMS access, although historical automatic import will be unavailable.

## Notification access

Live financial-notification tracking is optional. If enabled in Android settings, PesaTrack processes supported financial confirmations as they arrive. You can disable notification access from Android settings.

## Offline merchant identification

Online merchant/entity lookup is disabled. PesaTrack does not request Android internet permission. Business names are identified using local rules and saved evidence.

Previous versions offered optional online merchant lookup. That control is removed and previously saved preferences cannot re-enable remote requests.

No business descriptor, SMS text, transaction amount, balance, account number, phone number or reference is sent to a merchant-lookup service by this version. Existing locally cached merchant hints may remain on the device.

## Backups and exports

If you create an encrypted PesaTrack backup, the encrypted file is written only to a location you choose through Android's document picker. New backups require a passphrase of at least 12 characters; older encrypted backups using eight-character passphrases remain restorable. Keep your backup passphrase safe; PesaTrack cannot recover a forgotten passphrase.

CSV, PesaTrack personal PDF reports or other user-requested exports may contain financial data and counterparty names. Reports are created locally, and you choose where to save them using Android's document picker. They are **not passphrase-encrypted backups or institution-issued bank statements**. Once exported, those files are controlled by the storage location and apps you choose to share them with.

## Advertising, sale and profiling

PesaTrack is not designed to sell financial data or use financial-message content for advertising. Local merchant identification exists only to improve user-facing entity identification.

## Retention and deletion

Local data remains on your device until you delete it through PesaTrack, clear the app's storage, restore/replace the database, or uninstall the app, subject to Android storage behavior.

Removing an exported file or encrypted backup must be done from the storage location where you saved it.

## Security

PesaTrack provides an optional app lock and passphrase-encrypted backup/restore. **App lock does not encrypt the ordinary local SQLite transaction and statement databases**, which rely on Android app-private storage and device protections. No software can guarantee absolute security, so keep your device and Android security updates current and protect exported files and backup passphrases.

## Data subject rights and contact readiness

Applicable Kenyan data-protection law may provide rights to be informed, access, correct, object to or request deletion of personal information. Before public distribution, the operator must publish an accessible private privacy contact with verified controller identity and assess ODPC registration requirements. Removing an app does not delete any separately exported copies under your control.

## Managing saved PDF statement archives

In **Settings → Privacy → Manage saved statements**, you can delete one independently
saved PDF statement archive at a time after reviewing a confirmation. The deletion
removes that archive's row history and per-row statement category labels. It does
**not** delete canonical Activity transactions, debts confirmed from Activity,
shared merchant-category rules, source PDFs outside the app, or backups exported
elsewhere. Remove external copies separately if desired.

## Changes to this policy

Material privacy changes should be reflected in this document before a public release that uses them.

## Contact

For product/privacy questions, use the PesaTrack repository's issue/contact channel. Do not post raw SMS, account numbers, transaction codes or other sensitive financial information in public issues.
