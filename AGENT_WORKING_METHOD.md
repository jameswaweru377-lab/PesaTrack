# PesaTrack Agent Working Method

Read this file at the start of every session, before any audit or code change.
Keep AGENTS.md (the financial rules) and docs/PRODUCT_CHARTER.md. This file adds
HOW to work. If two documents disagree, the code and test evidence win, and you
fix the document in the same change.

## 1. Who you work for

The owner is a founder in Nairobi who often dictates by voice. Write in plain,
short English. Lead with the answer. Explain any technical word in one line.
Do not bury the result under process. Be kind and direct: say what is good,
say what is wrong, say what to do next.

## 2. The mission, in three lines

1. Trust beats features. A wrong number is worse than a missing number.
2. Prefer uncertain over wrong. Show what is known and what needs confirmation.
3. Local-first and private. Financial data does not leave the phone unless the
   owner approves it in writing and the privacy policy says so.

## 3. How to work (the method)

1. Evidence over assertion. Never say something works, passes or is fixed
   unless you ran it. Name the command and the result. If you could not run it,
   write 'not verified' and say why.
2. Read before you write. Read the code, the tests and the docs around the
   change. Check what a commit title or a doc claims against the real code.
3. Probe, do not assume. When logic looks suspicious, test it with real inputs.
   Example: a privacy filter looked wrong, so it was fed real business and
   personal names, and the results showed it failing in both directions.
4. Separate fact from judgement. Facts have evidence (file and line, output).
   Judgements are labelled as judgements. Give a severity and the reason.
5. State the limits. Say what you checked, what you did not, and what could
   change the conclusion. Never imply full coverage you did not have.
6. Calibrate. Do not exaggerate problems and do not hide them. Credit what is
   genuinely good. Give mitigating facts when they exist.
7. Own mistakes plainly. If an earlier assumption turns out wrong, say so in
   one sentence, correct it, and continue. No long apologies.
8. Smallest safe change. One concern per change. Do not refactor and add
   features in the same change. Do not rewrite a whole file to adjust it.
9. Tests first in risky areas (money, database, backup, migrations, ingestion).
   Write the failing test, then the fix.
10. Find answers yourself before asking. Ask a human only in the cases listed in
    section 8, and ask one clear question at a time.
11. Prioritise. Fix what blocks release first, then what risks user data, then
    what slows the team, then polish.
12. Report briefly (section 6).

## 3b. Start-of-session checklist

1. Record the branch and commit SHA.
2. Read: README, CURRENT_STATE.md, docs/PRODUCT_CHARTER.md, AGENTS.md, DESIGN.md,
   the latest audit report.
3. Run the unit tests and note the baseline numbers before changing anything.
4. List open pull requests. Never touch another agent's branch.
5. Say in one line what you will do and what you will not do.

## 4. Rules by area

Money and finance
- Amounts are integer minor units. Never use Double or Float for money.
- Keep transfers, loans, fees, refunds and own-account moves distinct. Never
  invent a balance. If evidence is weak, mark it uncertain and ask for review.
- If a number appears on two screens it comes from the same function. If two
  numbers differ by definition, they get different labels, and any comparison
  says what 'prior' means.

Database and backup
- One owner per database file. No new helper per call.
- Any schema change needs a migration, a backup and restore contract update,
  and tests. Never hardcode the schema version in a second place.
- Anything that writes or restores user data must be tested by a test that
  actually runs in CI (see section 5).
- Post-commit work (diagnostics, logging, analytics) must never make a committed
  write look like a failure.

Privacy and security
- No new permission, network call, telemetry or third-party data flow without
  explicit owner approval and a same-change update to the privacy policy and
  the Play data-safety notes.
- Tests and examples use invented Kenyan data only. Never real names, numbers
  or messages.
- Security timing logic (lockouts, auto-lock) uses a monotonic clock.
- Do not describe app lock as encryption. It is a UI gate.
- Any filter that decides what may leave the device needs a shared test corpus
  with both positive and negative cases, in every language it is implemented in.

Code structure
- No file over 800 lines and no function over 80 lines in new or changed code.
- Do not add to TransactionStore.kt, ActivityScreen.kt or
  FinancialMessageIngestionCoordinator.ingest() except to extract from them.
- The domain layer imports nothing from Android or from outer layers.
- No logic copied between Kotlin and JavaScript. Share a test corpus instead.
- No silent catch. Every caught error is logged as a diagnostic or rethrown.

UI and UX
- Use theme tokens and shared components only: no hex colors, no raw dp values
  outside 4, 8, 12, 16, 24, 32, no new corner radii, no new button styles.
- One primary color. One primary action per screen. 16 dp screen margins.
  Sentence case. 48 dp minimum touch targets. Contrast of at least 4.5 to 1.
- Semantic colors are reserved: amber means attention, rose means loss or
  error, green or aqua means good or done, gray means no change.
- Every screen defines loading, empty (with a next action), error and
  partial-data states. Lists start within the first screen. Drilled-in screens
  have a back button and a title. No decorative charts without a scale.

CI and release
- Never publish a debug or debuggable build to a public place.
- Release builds use R8 shrinking and are tested on a real device before a tag.
- New dependencies need a one-line reason, an exact pinned version and a check
  of the APK size impact.

Documentation
- Update CURRENT_STATE.md, README.md, the privacy policy and DESIGN.md in the
  same change when the change touches what they describe.

## 5. Tests: what counts as proof

- Unit tests on the JVM prove pure logic only.
- Emulator tests prove database, migration, backup and ingestion behaviour, but
  only when they are actually executed. A test that is compiled but never run
  proves nothing. Say which kind you ran.
- A device run proves the user journey. Never claim a device result without
  naming the device, the date and the commit.

## 6. How to report a finished task

Use this shape, in plain words:

1. Result: one or two sentences. Done, partly done, or blocked.
2. Evidence: the exact commands you ran and the pass and fail counts.
3. What changed: files and behaviour, briefly.
4. Not verified: what you could not run or check.
5. Risks and follow-ups: at most three, in priority order.
6. Docs updated: which ones.

Example of a good result line: 'Fixed. The restore now rolls back if the
statement archive fails. Ran StatementArchiveBackupIntegrationTest and the 987
domain tests: all pass. Not verified on a physical device.'

## 7. Audit mode

- Keep one findings register (the audit report). Update statuses (Open, Fixed,
  Won't fix) with evidence. Add new findings with the next free ID.
- Every finding has: ID, severity (P0 blocks release, P1 fix next, P2 plan,
  P3 opportunistic), status, evidence, and an action tagged UPDATE, REMOVE or ADD.
- Re-verify old findings against the current code. Do not trust commit titles.
- List strengths. A fair audit says what to keep.
- Finish with an ordered action plan and an explicit list of what was not covered.

## 8. Stop and ask the owner when

- a change needs a new permission, network egress or telemetry;
- a change alters how money is classified, counted or displayed;
- a change deletes user data or breaks backup and restore compatibility;
- two reasonable options would produce different financial numbers;
- you find a possible data-loss or privacy problem in shipped behaviour.

## 9. Anti-patterns (do not do these)

- Declaring success because the code compiles.
- Hiding a decision behind a new setting or toggle.
- Growing a giant file or function because it is faster today.
- Fixing a symptom in one screen when the cause is a shared function.
- Claiming coverage you did not have, or numbers you did not measure.
- Long reports that bury the answer. Lead with the result.
- Changing behaviour and tests in the same step so the test can no longer fail.
- Creating a second source of truth for a number, a color or a rule.
