# Contributing to PesaTrack

Thanks for taking an interest in PesaTrack. Useful bug reports, documentation feedback, and careful technical discussion are welcome.

**Important:** The source is publicly visible but **not open-source licensed**. The [proprietary source-code notice](LICENSE) does not grant permission to reuse code in another project. Please discuss a proposed code contribution with the maintainer **before** investing in a pull request; any necessary permission or contribution-rights agreement must be settled separately in writing. Do not assume that opening or accepting a PR transfers copyright.

## Start with an issue

- Check existing [issues](https://github.com/jameswaweru377-lab/PesaTrack/issues) and active pull requests for duplicates or overlapping work.
- Describe the problem, the expected result, the app version or commit, and whether it was verified on a real device.
- Use **invented financial examples**. Never attach genuine SMS messages, statements, payslips, account identifiers, screenshots with private records, or credentials.
- For a security weakness, **do not open a detailed public issue**. Follow [Security](SECURITY.md).

## If a code change is agreed

1. Read the [product charter](docs/PRODUCT_CHARTER.md), [current state](CURRENT_STATE.md), [agent instructions](AGENTS.md), and [working method](AGENT_WORKING_METHOD.md).
2. Start from current `main` and make a small, focused branch. Check active PRs and task ownership before modifying shared files.
3. Preserve financial meaning: income, expenses, self-transfers, fees, refunds, loans, balances, and uncertain observations must stay distinct. Never silently guess a value or override a user's confirmed correction.
4. Use synthetic test fixtures and keep secrets, signing files, private records, and unnecessary network calls out of commits.
5. Open a PR using the [template](.github/PULL_REQUEST_TEMPLATE.md). Link the relevant issue, describe the exact change, and list the tests you actually ran and what remains unverified.
6. Wait for maintainer review. Passing CI does not replace manual acceptance, and an unmerged PR is not an approved release.

## Verification expectations

- Android changes normally require JVM tests, Android lint, and debug APK assembly through the existing CI workflow.
- Android instrumentation is **manual and explicitly requested**, not a default PR or release gate. Never claim emulator or phone tests passed unless they were actually run.
- Financial, migration, backup, and statement changes require appropriately focused negative and regression cases.
- Worker changes must pass their relevant syntax and contract checks.
- Documentation-only changes should verify links and accuracy; they are not evidence that an Android build or device journey passed.

Keep the current **Home / Activity / Plan / More** navigation and approved designs unless a change is separately approved. Do not alter release signing, APK publishing, privacy permissions, or user-data semantics incidentally.

Please also read our [Code of Conduct](CODE_OF_CONDUCT.md).
