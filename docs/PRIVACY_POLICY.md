# PesaTrack Privacy Policy

_Effective: 7 October 2026_

PesaTrack is a local-first personal-finance application. Its purpose is to help you understand financial activity from supported SMS and notification evidence, manual entries and settings you provide.

## Data processed on your device

Depending on the features you choose to enable, PesaTrack can process:
- financial SMS history you explicitly permit the app to read;
- financial notifications you explicitly allow the notification-listener service to access;
- transaction, account, debt, salary, budget, bill and saving-goal information derived from that evidence;
- manual corrections, categories and settings you enter;
- app-lock and encrypted-backup settings.

The local PesaTrack database on your device is the primary source of truth.

## SMS permission

Historical SMS import is optional. If you grant Android's SMS permission, PesaTrack reads the financial-message history you choose so it can create and reconcile your local ledger.

PesaTrack is designed to ignore unrelated messages rather than turn them into financial records. Full SMS bodies are not sent to the optional online merchant lookup service.

You can use PesaTrack without granting historical SMS access, although historical automatic import will be unavailable.

## Notification access

Live financial-notification tracking is optional. If enabled in Android settings, PesaTrack processes supported financial confirmations as they arrive. You can disable notification access from Android settings.

## Optional online merchant lookup

Online merchant/entity lookup is off by default.

If you turn it on, PesaTrack may send a sanitized business descriptor to the configured enrichment service to improve merchant naming or branding. Before sending, the app applies a privacy filter intended to reject likely phone numbers, transaction codes, email addresses, long numeric references and likely personal names.

PesaTrack does not intentionally send full SMS bodies, transaction amounts, balances, account numbers, phone numbers or transaction/reference codes to this service.

The client also supplies a random install-scoped identifier used only to partition rate limits. It is not an account login or device identifier.

## Backups and exports

If you create an encrypted PesaTrack backup, the encrypted file is written only to a location you choose through Android's document picker. Keep your backup passphrase safe; PesaTrack cannot recover a forgotten passphrase.

CSV or other user-requested exports may contain financial data. Once exported, those files are controlled by the storage location and apps you choose to share them with.

## Advertising, sale and profiling

PesaTrack is not designed to sell financial data or use financial-message content for advertising. Optional enrichment exists only to improve user-facing entity identification.

## Retention and deletion

Local data remains on your device until you delete it through PesaTrack, clear the app's storage, restore/replace the database, or uninstall the app, subject to Android storage behavior.

Removing an exported file or encrypted backup must be done from the storage location where you saved it.

## Security

PesaTrack provides an optional app lock and passphrase-encrypted backup/restore. No software can guarantee absolute security, so keep your device and Android security updates current and protect exported files and backup passphrases.

## Changes to this policy

Material privacy changes should be reflected in this document before a public release that uses them.

## Contact

For product/privacy questions, use the PesaTrack repository's issue/contact channel. Do not post raw SMS, account numbers, transaction codes or other sensitive financial information in public issues.
