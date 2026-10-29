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
  commercial business-type terms, reject ambiguous names and single words,
  and share a synthetic test corpus across Android and Worker. This can
  under-enrich some valid merchants, which is preferable to leaking names.

## Boundaries

- Continue blocking phone numbers, email, account/reference numbers with four
  or more consecutive digits,
  and 10-character alphanumeric transaction codes containing digits.
- All-letter words of ten characters are not automatically transaction codes.
- Known public entities such as Java House and KPLC Prepaid may be explicitly
  recognized. Named institutions with a meaningful business-type term (such
  as Strathmore University or Zuri Properties) are also recognized.
- A standalone surname, a multiword personal name, and ambiguous generic
  descriptors such as `Mary Mobile` or `Jane Services` are not exported.
- No new app permission, network destination, telemetry, identifier, channel,
  provider-specific financial rule, or change to ledger/accounting logic.
- Unknown descriptors simply stay local: optional enrichment may return no
  result, never inventing a category or cash movement.
- The same synthetic test vectors exercise both language implementations;
  changing the accepted descriptor set later requires updating both tests.

## Review and rollout

Keep this in an independent PR. CI must pass Worker tests, Android JVM tests,
lint, and APK assembly. Do not merge, deploy the Worker, or publish an APK
until the owner reviews the changed privacy boundary. A real-device test is
not implied by CI success.
