"""Conservative historical-audit import planner, with no remote writes."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import re

AUDIT_ID = re.compile(r"(?:[A-Z]{1,3}-?\d{1,3})\Z")
PR_LINK = re.compile(r"(?:^PR(?:s)?:\s*#|/pull/)(\d+)", re.MULTILINE | re.IGNORECASE)
SOURCE_LINE = re.compile(r"(?im)^Source(?: / original audit ID)?:\s*([^\r\n]+)$")


def _existing_source_ids(body: object) -> set[str]:
    if not isinstance(body, str):
        return set()
    return {match.group(0) for line in SOURCE_LINE.findall(body)
            for match in re.finditer(r"\b(?:[A-Z]{1,3}-?\d{1,3})\b", line)}


def _issue_pr_numbers(body: object) -> set[int]:
    if not isinstance(body, str):
        return set()
    return {int(m) for m in PR_LINK.findall(body)}


def plan_import(audits: list[dict], existing: list[dict]) -> dict:
    """Deterministic preview; never infer an implementation or device acceptance."""
    by_source: dict[str, list[int]] = {}
    by_pr: dict[int, set[int]] = {}
    for issue in existing:
        number = issue.get("number")
        if not isinstance(number, int):
            continue
        for source_id in _existing_source_ids(issue.get("body")):
            by_source.setdefault(source_id, []).append(number)
        for pr_number in _issue_pr_numbers(issue.get("body")):
            by_pr.setdefault(pr_number, set()).add(number)

    records: dict[str, dict] = {}
    review = []
    for entry in audits:
        source_id = str(entry.get("source_id") or "").strip().upper()
        severity = entry.get("severity")
        title = entry.get("title")
        if not AUDIT_ID.fullmatch(source_id) or severity not in {"P0", "P1", "P2", "P3"} or not (
            isinstance(title, str) and title.strip() and len(title) <= 180
        ):
            review.append({"source_id": source_id, "reason": "invalid-source-or-severity"})
            continue
        normalized = {
            "source_id": source_id, "title": title.strip(), "severity": severity,
            "related_prs": sorted({n for n in entry.get("related_prs", []) if isinstance(n, int) and n > 0}),
            "status": "Untriaged",
        }
        old = records.get(source_id)
        if old is not None and old != normalized:
            review.append({"source_id": source_id, "reason": "conflicting-source"})
            records.pop(source_id, None)
            continue
        records[source_id] = normalized

    create = []
    link = []
    for source_id, entry in sorted(records.items()):
        matches = set(by_source.get(source_id, []))
        if not matches:
            for pr_number in entry["related_prs"]:
                matches.update(by_pr.get(pr_number, set()))
        if len(matches) == 1:
            link.append({**entry, "issue_number": next(iter(matches))})
        elif len(matches) > 1:
            review.append({"source_id": source_id, "reason": "ambiguous-existing",
                           "possible_issues": sorted(matches)})
        else:
            create.append(entry)
    return {"create": create, "link": link,
            "review": sorted(review, key=lambda row: (row["source_id"], row["reason"]))}


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--audit-manifest", required=True, type=Path)
    parser.add_argument("--existing-issues", required=True, type=Path)
    parser.add_argument("--apply", action="store_true", help="Reserved; requires separate owner approval")
    args = parser.parse_args(argv)
    if args.apply:
        parser.error("Automatic Issue creation is deliberately disabled pending reviewed preview")
    audits = json.loads(args.audit_manifest.read_text(encoding="utf-8"))
    existing = json.loads(args.existing_issues.read_text(encoding="utf-8"))
    print(json.dumps(plan_import(audits, existing), indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
