# S5: Merchant descriptor privacy boundary

## Intent

Correct the audited mismatch between the Android and Worker sanitizers: ordinary
public business names can be rejected as transaction codes, while ambiguous
personal-looking descriptors can be accepted for optional online enrichment.
This is a privacy-first correction, not an expansion of online-lookup channels.

## Approaches considered

- Keep the existing generic-word heuristics: smallest change, but leaves known
  privacy leaks and rejected business examples unfixed.
- Allow all human-readable names after stripping numbers: improves merchant
  coverage but risks exposing personal names. Rejected.
- **Chosen:** accept a narrow set of known public names and genuinely
  commercial business-type terms at the end of a descriptor, reject ambiguous
  names and single words, and share a synthetic test corpus across Android
  and Worker. This can
  under-enrich some valid merchants, which is preferable to leaking names.

## Boundaries

- Fail closed for any digit anywhere or any non-ASCII/potentially obfuscated
  character, including split phone/account numbers and Unicode-hidden codes.
  Do this before stripping punctuation and never send an unsafe original.
- Ordinary ten-letter words require commercial evidence or an exact public
  name match; unknown ten-letter tokens also fail closed.
- Known public entities such as Java House, KPLC Prepaid, Strathmore University,
  and Zuri Properties may be explicitly recognized. `properties` alone is not
  evidence that a label is public.
- A standalone surname, a multiword personal name, and ambiguous generic
  descriptors such as `Mary Mobile` or `Jane Services` are not exported.
- No new app permission, network destination, telemetry, identifier, channel,
  provider-specific financial rule, or change to ledger/accounting logic.
- Unknown descriptors simply stay local: optional enrichment may return no
  result, never inventing a category or cash movement.
- A business-type suffix remains a heuristic, not a proof of public identity.
  The owner must review that residual disclosure risk before merge/deployment.
- The same synthetic test vectors exercise both language implementations;
  changing the accepted descriptor set later requires updating both tests.

## Review and rollout

Keep this in an independent PR. CI must pass Worker tests, Android JVM tests,
lint, and APK assembly. Do not merge, deploy the Worker, or publish an APK
until the owner reviews the changed privacy boundary. A real-device test is
not implied by CI success.
