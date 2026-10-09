# PBKDF2-SHA256 on Android 24 and 25

Android supplies `PBKDF2WithHmacSHA256` through `SecretKeyFactory` starting at API 26. PesaTrack supports API 24, so app-lock PIN creation and encrypted backup previously failed there with `NoSuchAlgorithmException`.

`PasswordKeyDerivation` keeps the original native factory on API 26 and newer. Only API 24/25 select `Pbkdf2Sha256`, an implementation of RFC 8018 section 5.2 using the platform `HmacSHA256` primitive. Passwords are UTF-8, block counters are four-byte big-endian, each block XORs exactly the requested iteration count, and output is truncated to the requested number of bytes. The app still uses 120,000 iterations for PINs and 180,000 for newly exported backups; existing supported backup iteration values are preserved during restore. Salt, AES-GCM parameters, key lengths and stored formats remain unchanged. No SHA-1 substitution or new dependency is introduced.

Mutable password-byte and intermediate arrays are cleared when derivation finishes; caller-owned password/salt arrays are preserved. This is best-effort memory hygiene, not a promise that immutable Strings or provider-internal key state can be erased.

Sources: [Android SecretKeyFactory algorithm support](https://developer.android.com/reference/javax/crypto/SecretKeyFactory), [RFC 8018 section 5.2](https://www.rfc-editor.org/rfc/rfc8018#section-5.2).

## Verification and limits

The Java regression fixtures use fixed outputs independently generated with Python `hashlib.pbkdf2_hmac`, then compare the fallback against JDK 17's native factory. They cover ASCII, UTF-8 accented characters and a supplementary Unicode character, more than one SHA-256 output block, HMAC long/empty keys, and actual PIN/backup iteration counts. Invalid parameters and caller-input preservation are also checked. Run locally from the repository root without Android tools:

```text
javac -encoding UTF-8 -d build/pbkdf2-check app/src/main/java/com/kedevjimmy/pesatrack/security/Pbkdf2Sha256.java app/src/test/java/com/kedevjimmy/pesatrack/security/Pbkdf2Sha256Vectors.java
java -cp build/pbkdf2-check com.kedevjimmy.pesatrack.security.Pbkdf2Sha256Vectors
```

`Pbkdf2Sha256Test` runs the same meaningful checks under JUnit. `Pbkdf2CompatibilityIntegrationTest` verifies fixed keys through Android's actual API selector and decrypts/reproduces a fixed independently generated JDK-native PESABK01/AES-GCM fixture. The fixture is a wire-level cryptography test with invented metadata, not a complete ledger restore. Existing `SecurityIntegrationTest` separately verifies real app-lock and backup round-trip behavior with invented test data.

API 24 instrumentation and modern API instrumentation remain required release evidence. This security-sensitive compatibility implementation requires independent review of the derivation loop and encoding before publication.
