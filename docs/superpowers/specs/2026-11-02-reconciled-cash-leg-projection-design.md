# Reconciled cash-leg projection — Phase 4 first stage

## Intent

Prepare the accounting rules for one bank debit, one wallet statement credit and one wallet SMS credit to be understood as **two cash legs of one internal transfer**, never three receipts or new income. Work remains read-only and reversible at this stage.

## Approaches

1. Update Activity transaction rows and report sums immediately: rejected by the current N2 posting gate and because durable device evidence is absent.
2. Sum every statement and SMS observation, then subtract matched amounts: rejected; different scopes and ambiguous matches make subtraction unsafe.
3. **Selected:** a pure, candidate-validated projection of uniquely confirmed legs and internal-transfer amounts. It consumes the existing metadata decisions and explicitly refuses unverified, conflicting, protected or mismatched evidence. No write path or report consumer.

## Behavior

- Accept only current `CONFIRMED` decisions whose exact evidence IDs match a current strong suggestion; rejected or undone choices produce no projection.
- Validate both legs of an owned transfer: distinct accounts, opposite signs, same exact positive magnitude, reference, currency and bounded time. Multiple independent observations of the same wallet leg count once; original observations remain present.
- A confirmed same-leg group collapses multiple observations into one projected leg; protect roles, ownership, reference and source assurance.
- If evidence differs, overlaps another confirmed group, overflows `Long` or is ambiguous, decline the projection without guessing a zero or changing canonical reports.
- Emit **separate preview counts and amounts**, with explicit external-income/spending contribution of zero for confirmed internal transfers. Never publish a new total without the later N2 owner-resume gate.
- No migration, schema, Android permissions, encryption/backup, app-wide totals or instrumentation changes.

## Acceptance

Synthetic triad, reject/undo, protected fee, unverified statement, same-amount different references, overflow and overlap tests. Exact-head Android JVM/lint/build and privacy checks. The original data persists untouched; manual phone tests remain a separate follow-up.
