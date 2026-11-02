"""Retire two owner-approved superseded documentation-only branch refs."""

from __future__ import annotations

import argparse
import json
import os
import subprocess
from pathlib import Path
from urllib import request

from tools.tracker.branch_cleanup import (
    _lease_delete, _recheck_candidate, _same_repository, _verify_ref_absent,
    collect_inventory,
)


APPROVED = (
    ("design/backup-restore-phase1-20261030",
     "5409f66457b3e3e484bf4c55e21b7e65599555b3",
     "docs/superpowers/specs/2026-10-30-backup-restore-phase1-design.md",
     "b7e004ca67c2a332e1366e226b43f4f1b9296f2f"),
    ("design/universal-statement-auto-import-20261031",
     "3f208075244786b30d62a4f7ba5c999131a56126",
     "docs/superpowers/specs/2026-10-31-universal-statement-safe-auto-import-design.md",
     "eca6f8e08699a97dbc9de097753acf68c24f32fe"),
)


def approved_candidates() -> list[dict]:
    return [dict(zip(("branch", "sha", "doc", "old_blob"), row)) for row in APPROVED]


def plan(snapshot: dict) -> dict:
    if snapshot.get("repository") != "jameswaweru377-lab/PesaTrack" or snapshot.get("default_branch") != "main":
        raise ValueError("Unexpected repository or default branch")
    branches = {item["name"]: item for item in snapshot["branches"]}
    available, skipped = [], []
    for item in approved_candidates():
        branch = branches.get(item["branch"])
        reason = None
        if not branch:
            reason = "missing"
        elif branch["commit"]["sha"] != item["sha"]:
            reason = "moved"
        elif branch["protected"] is not False:
            reason = "protected"
        elif any(p["state"] == "open" and p["head"]["ref"] == item["branch"]
                 and (p["head"].get("repo") is None
                      or _same_repository(p["head"]["repo"], snapshot["repository"]))
                 for p in snapshot["pulls"]):
            reason = "open_pr"
        if reason:
            skipped.append({"branch": item["branch"], "reason": reason})
        else:
            available.append(item)
    return {"candidates": available, "skipped": skipped}


def check_current_documents(rows: list[dict]) -> None:
    for item in rows:
        path = Path(item["doc"])
        if not path.is_file():
            raise RuntimeError("Current-main replacement document is missing")
        blob = subprocess.run(["git", "hash-object", str(path)], text=True,
                              capture_output=True, check=True).stdout.strip()
        if blob == item["old_blob"]:
            raise RuntimeError("Current document has not superseded old branch content")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    token = os.environ["GITHUB_TOKEN"]
    repository = "jameswaweru377-lab/PesaTrack"
    snapshot = collect_inventory(repository, token)
    result = plan(snapshot)
    check_current_documents(result["candidates"])
    deleted, skipped = [], list(result["skipped"])
    if args.apply:
        for item in result["candidates"]:
            candidate = dict(item, evidence="approved_age", merged_pr=None,
                             default_branch="main")
            eligible, reason = _recheck_candidate(repository, token, candidate,
                                                   opener=request.urlopen)
            if not eligible:
                skipped.append({"branch": item["branch"], "reason": reason})
                continue
            _lease_delete(repository, token, item["branch"], item["sha"])
            _verify_ref_absent(repository, token, item["branch"], item["sha"],
                               opener=request.urlopen)
            deleted.append(item["branch"])
    print(json.dumps({"approved": 2, "eligible": len(result["candidates"]),
                      "deleted": deleted, "skipped": skipped}, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
