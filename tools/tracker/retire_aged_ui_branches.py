"""Owner-approved exact-tip retirement of two old, unclaimed UI proposals."""

from __future__ import annotations

import argparse
import json
import os
from urllib import request

from tools.tracker.branch_cleanup import (
    _lease_delete, _recheck_candidate, _same_repository, _verify_ref_absent,
    collect_inventory,
)


APPROVED = {
    "codex/more-screen-refresh-20261024": "82b7d6dfa8f1d15d2db5a34dade997cee0fbcb08",
    "codex/statements-overview-dashboard-20261025": "7ec0e188415f46446fc36e899c7ea9e3711e6c9e",
}


def candidates() -> dict[str, str]:
    return dict(APPROVED)


def plan(snapshot: dict) -> dict:
    if (snapshot.get("repository") != "jameswaweru377-lab/PesaTrack"
            or snapshot.get("default_branch") != "main"):
        raise ValueError("Unexpected repository or default branch")
    branches = {row["name"]: row for row in snapshot["branches"]}
    eligible, skipped = [], []
    for name, sha in candidates().items():
        row = branches.get(name)
        reason = None
        if row is None:
            reason = "missing"
        elif row["commit"]["sha"] != sha:
            reason = "moved"
        elif row["protected"] is not False:
            reason = "protected"
        elif any(p["state"] == "open" and p["head"]["ref"] == name
                 and (p["head"].get("repo") is None
                      or _same_repository(p["head"]["repo"], snapshot["repository"]))
                 for p in snapshot["pulls"]):
            reason = "open_pr"
        if reason:
            skipped.append({"branch": name, "reason": reason})
        else:
            eligible.append({"branch": name, "sha": sha})
    return {"eligible": eligible, "skipped": skipped}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    repository = "jameswaweru377-lab/PesaTrack"
    token = os.environ["GITHUB_TOKEN"]
    result = plan(collect_inventory(repository, token))
    deleted, skipped = [], list(result["skipped"])
    if args.apply:
        for item in result["eligible"]:
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
    print(json.dumps({"approved": len(APPROVED), "eligible": len(result["eligible"]),
                      "deleted": deleted, "skipped": skipped}, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
