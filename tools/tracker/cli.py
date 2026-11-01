"""Advisory GitHub Actions/terminal entry point; cannot mutate Issues."""

from __future__ import annotations

import argparse
from collections import Counter
import json
import os
from pathlib import Path
import sys

from .github_snapshot import fetch_snapshot, save_snapshot
from .reconcile import reconcile


def report_markdown(data: dict) -> str:
    tasks = data["tasks"]
    counts = Counter(task["status"] for task in tasks)
    merged = sum(t["status"] == "Merged — needs acceptance" for t in tasks)
    failures = sum(t["ci"] == "failed" for t in tasks)
    unclaimed = sum(t["owner"] == "Unclaimed" for t in tasks)
    lines = [
        "## PesaTrack tracker — read-only", "",
        f"Checked at: {data['checked_at']}",
        f"Canonical tasks: {len(tasks)}. Merged needing acceptance: {merged}. "
        f"CI failed: {failures}. Unclaimed: {unclaimed}.", "",
        f"Untracked open PRs: {len(data.get('untracked_open_prs', []))}. "
        "They need a canonical task link before claiming ownership.", "",
        "Progress by canonical Issue (not live ChatGPT presence): " +
        ", ".join(f"{status}: {count}" for status, count in sorted(counts.items()))
        if counts else "No approved task Issues registered yet.", "",
        "| Task | Owner | Status | CI | Device | Acceptance | Next action |",
        "|---|---|---|---|---|---|---|",
    ]
    if not tasks:
        lines.extend(["", "No labeled tracker Issues are visible yet. This does not mean "
                      "that PesaTrack has no unfinished work; check the label and import setup."])
    if data.get("incomplete"):
        lines.extend(["", "**Coverage incomplete:** GitHub pagination or PR evaluation "
                      "was capped. Some historical work may be missing; do not treat "
                      "this report as a complete inventory."])
    if data.get("historical_window_limited"):
        lines.extend(["", "Historical PR search is bounded. Older unlinked closed PRs may "
                      "not appear; canonical Issue PR references are resolved separately."])
    for t in tasks:
        # Output only bounded identifiers/status values, never raw Issue bodies.
        acceptance = t.get("acceptance") or {}
        lines.append(
            f"| {t['task_id']} | {t['owner']} | {t['status']} | "
            f"{t['ci']} | {t['device_acceptance']} | "
            f"{acceptance.get('mode', 'unknown')} | "
            f"{acceptance.get('action', 'collect-evidence')} |"
        )
    if data["warnings"]:
        lines.extend(["", f"Cross-PR collision/linkage warnings: {len(data['warnings'])}.",
                      "Review the private workflow logs and GitHub issue/PR links before editing."])
    untracked = data.get("untracked_open_prs", [])
    if untracked:
        lines.extend(["", "Unlinked open PR numbers (bounded): " +
                      ", ".join(f"#{number}" for number in untracked[:25]) +
                      (" (more omitted)" if len(untracked) > 25 else "")])
    return "\n".join(lines) + "\n"


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", default=os.environ.get("GITHUB_REPOSITORY"))
    parser.add_argument("--dry-run", action="store_true", help="Always read-only (default)")
    parser.add_argument("--json", action="store_true", help="Output the bounded task report as JSON")
    parser.add_argument("--snapshot-file", help="Store this run's snapshot for later tracker steps")
    parser.add_argument("--sync-task-metadata", action="store_true",
                        help="Reserved: unavailable until separate owner-reviewed approval")
    args = parser.parse_args(argv)
    if args.sync_task_metadata:
        parser.error("Metadata synchronization is not enabled; only read-only reports are permitted")
    if not args.repository:
        parser.error("Supply --repository owner/name or GITHUB_REPOSITORY")
    try:
        snapshot = fetch_snapshot(os.environ.get("GITHUB_TOKEN", ""), args.repository)
        if args.snapshot_file:
            save_snapshot(args.snapshot_file, snapshot)
        output = reconcile(snapshot)
    except (RuntimeError, ValueError) as exc:
        print(f"Tracker unavailable: {exc}", file=sys.stderr)
        return 2
    rendered = json.dumps(output, indent=2) if args.json else report_markdown(output)
    print(rendered)
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with Path(summary).open("a", encoding="utf-8") as out:
            out.write(report_markdown(output))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
