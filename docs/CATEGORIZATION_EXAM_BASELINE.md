# Categorization exam: invented Kenyan transactions

Scope: **600 wholly invented** examples (60 each of ten transaction kinds),
tested against the current built-in `KenyanTransactionRecognizer` followed by
the exact default fallback used by `TransactionStore`. Cases deliberately have
**no personal SmartRule and no cached remote classification**. This is not a
score for full SMS interpretation, user-remembered categories or later ledger
corrections. No real names, account/phone numbers, messages or financial history
were used or uploaded.

The baseline is pinned to the SHA-256 of
`app/src/test/resources/categorization-exam-v1.tsv` and recorded in
`app/src/test/resources/categorization-exam-baseline-v1.json`.

| Invented transaction kind | Correct | Wrong | Needs review | Unclassified without review |
| --- | ---: | ---: | ---: | ---: |
| Paybill | 0 | 0 | 0 | 60 |
| Till | 0 | 0 | 0 | 60 |
| Person-to-person | 0 | 0 | 0 | 60 |
| Fuliza | 0 | 0 | 60 | 0 |
| Fees | 0 | 10 | 50 | 0 |
| Reversals | 25 | 10 | 25 | 0 |
| Airtime | 60 | 0 | 0 | 0 |
| Salary | 0 | 0 | 0 | 60 |
| Rent | 0 | 0 | 0 | 60 |
| School fees | 0 | 0 | 0 | 60 |
| **Overall count** | **85** | **20** | **135** | **360** |
| **Overall percent** | **14.17%** | **3.33%** | **22.50%** | **60.00%** |

Every kind contains 60 examples: a count of 10 corresponds to 16.67%,
25 to 41.67%, 50 to 83.33%, and 60 to 100%. These are four **mutually
exclusive** outcomes; they sum to 100%. An `UNCATEGORIZED` transaction with
`AUTO_CLASSIFIED` is **not** counted as sent for review.

The invented reversal and fee cases containing the word *airtime* reveal
20 incorrect auto-classifications in this limited default path: the built-in
phone-data recognizer wins before the fallback can retain the reversal or
fee's intended money meaning. This exam **does not fix that behavior**; any
change to classification needs a separate owner-approved task.

## CI guard

`CategorizationExamTest` reruns the **real** offline recognizer and extracted
fallback in the JVM unit-test suite on every pull request. It checks the fixed
600-case file hash, case IDs, exact per-kind baseline and a wrong-answer
ceiling of 20/600 (3.33%). Unreviewed case changes cannot dilute the rate.
Intentional improvements require recording and reviewing the new measured
snapshot; the wrong-answer ceiling may decrease but must not silently rise.
The original 600-case snapshot passed Android CI run 41177373457 with 1,316
JVM tests, zero failures and one skipped test.

This is a diagnostic of default category behavior, **not** a claim that
PesaTrack's complete ingestion, Fuliza accounting, statement imports, personal
rules or all Kenyan financial transactions achieve 14.17% category accuracy.
