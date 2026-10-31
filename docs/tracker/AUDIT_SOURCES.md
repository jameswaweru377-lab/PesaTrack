# Historical audit import sources

The starter manifest, [`AUDIT_MANIFEST.json`](AUDIT_MANIFEST.json), lists **42 findings**:

| Source | Findings | IDs | Verification level |
| --- | ---: | --- | --- |
| Whole-system audit remediation tracker, 30 October 2026, audit baseline 29 October | 20 | R, S, A, U, X, D series | Historical statuses; not live device/CI proof |
| [Cross-screen visual consistency audit, 31 October 2026](../audits/2026-10-31-visual-consistency-audit.md) | 22 | VC-01 through VC-22 | Mostly source-level risks; some measured source contrast; runtime confirmation still needed |

The original whole-system snapshot is `audit_drafts/AUDIT_REMEDIATION_TRACKER.md` in the concurrent working workspace. It may not be in every clone. The compact manifest intentionally includes source IDs, short titles, severity and explicitly known related PR numbers **only**; it does not contain private user data, audit PDF contents, phone information or a claim that any finding is resolved.

## Import rules

1. Snapshot existing repository Issues and compare explicit `Source:` lines and `PR: #...` evidence before proposing any new Issues.
2. Run the local preview: `python3 -m tools.tracker.bootstrap --audit-manifest docs/tracker/AUDIT_MANIFEST.json --existing-issues path/to/approved-public-issues.json`. Keep that second file outside the repository; supply JSON objects with only an Issue `number` and public `body`.
3. Review all `create`, `link` and `review` groups with the owner. Duplicate or conflicting matches remain flagged; do not invent links.
4. A new Issue must start **Untriaged**, even if a historical note says a PR merged. Confirm the current branch/head SHA, CI, merge and device acceptance separately before changing status. Deferred S3 remains deferred until explicitly resumed.
5. Only after reviewing the preview may a separate, explicitly authorized action create Issues; `bootstrap --apply` deliberately refuses remote writes.

The audit standards and proposals are **cross-cutting acceptance criteria**, not an additional set of silently completed tasks. Keep the original audit's security, privacy, accessibility, consistency and release gates visible as subtasks or checklists when individual Issues are triaged.
