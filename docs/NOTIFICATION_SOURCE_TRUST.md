# Notification source approval

Financial notification capture requires explicit approval of the exact Android package in **Settings → Approved notification apps**. All packages begin unapproved, including the current SMS app. Android's notification-access permission alone does not approve financial sources. Historical SMS import and recent SMS catch-up keep their existing permission and sender interpretation paths.

The listener checks approval before reading notification title/body or running financial relevance, M-Pesa detection or configured rules. Queued work checks approval again at admission before financial parsing and canonical ingestion. Preference synchronization is released before database work to keep Settings and Android callbacks responsive. Provider-neutral financial interpretation is unchanged. App labels and financial wording cannot approve a package; empty, malformed and PesaTrack's own package are rejected.

Unapproved notifications retain only the app package and app label in private local preferences. No notification title, body, sender, amount or account information is retained by this source list. Observed app metadata is bounded to 100 entries. Settings also offers apps Android currently makes visible and the OS-selected SMS app as approval choices; no new permission or installed-app cloud upload is used.

Approval applies to future notifications. Skipped notifications are not replayed automatically. Revocation blocks queued/new work; processing admitted while approval was enabled may finish, and existing ledger records remain available for review. Approvals are excluded from encrypted backup/restore, so restoring a backup cannot enable an approval that the user revoked, and a new device starts without approvals. Existing approvals on the current device are preserved by restore.

Approval is a user trust decision about an entire app, not authentication of every notification's financial content. Avoid approving forwarding/chat apps: an approved app can itself emit misleading text. This change prevents unknown packages from borrowing a bank/M-Pesa title to reach canonical ingestion; it does not claim to authenticate bank servers or SMS senders.

## Validation

JVM tests cover exact package approval, self/missing sources, default-deny capture screening, approved financial positives, OTP/promotional rejection and revocation. Android instrumentation covers unapproved invented spoof notifications through the admission boundary without canonical changes, approved/revoked ingestion, approval persistence, backup revocation preservation and Settings toggles. Physical-device acceptance must check Android notification permission, enable/disable approval, a supported invented notification, and queue revocation.

Without an Android installation, JDK 17 can validate the production source trust predicate from the repository root:

```text
javac -encoding UTF-8 -d build/source-trust-check app/src/main/java/com/kedevjimmy/pesatrack/ingestion/NotificationSourceTrustPolicy.java tools/NotificationSourceTrustCheck.java
java -cp build/source-trust-check NotificationSourceTrustCheck
```

This nine-case check does not replace Kotlin compilation, Android instrumentation or device acceptance.
