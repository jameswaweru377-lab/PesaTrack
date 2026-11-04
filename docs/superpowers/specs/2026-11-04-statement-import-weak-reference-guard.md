# Strong reference proof for repeated statement imports

**Problem:** The everyday statement import preview indexes any nonempty `reference` as exact duplicate proof. A generic label such as `TRANSFER` on an otherwise same-account, same-day, same-amount leg may silently suppress a distinct deposit or payment on confirmation.

**Approved behavior:** Only a stable, digit-bearing transaction reference (uppercase alphanumeric length 8–48, at least one digit) may support an exact match. Case and surrounding whitespace are normalized. All-letter narration, empty references, punctuation and short labels remain weak evidence and may trigger the existing possible-match review.

**Boundaries:** Change only `StatementImportMatchIndex` and its JVM policy tests; preserve existing amount, date, direction, account matching; no persistence/schema/ledger-calculation changes; no import-auto-admission changes; no emulator or Android instrumentation runs. Keep #563/#569 and any other chat's code untouched.

**Safety checks:** Reference `QAB1234567` remains an exact match for the same account/day/side/amount and is rejected for mismatching identity; `TRANSFER` and `PAYMENT` are not exact even when same amount/day; valid reference normalization remains case insensitive; ambiguous same-day movements remain `hasPossibleMatch=true`. A reviewer should also inspect numeric-only references, code collisions, mixed sources, invalid dates and signed amount extremes as follow-ups, without broadening this slice.

**Verification:** RED failing JVM regression against old code, GREEN full PR Android CI and security scan at exact head. Merge only after latest-main compatibility and independent review; skip instrumentation.