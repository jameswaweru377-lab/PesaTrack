"""One-time, owner-approved, recoverable retirement of 42 old PesaTrack branches."""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from pathlib import Path
from urllib import error, parse, request

from tools.tracker.branch_cleanup import (
    BRANCH_PATTERN, SHA_PATTERN, _api_request, _api_url,
    _lease_delete, _recheck_candidate, _same_repository, _validate_inventory,
    _verify_ref_absent, collect_inventory,
)

MAX_OLD_RETIREMENTS = 42
EXPECTED_PREFIX = "archive/retired-20261101"
EXPECTED_CUTOFF = "2026-10-02"
APPROVED_BATCHES = {
    EXPECTED_PREFIX: EXPECTED_CUTOFF,
    "archive/retired-20261101-wave2": "2026-10-09",
}


def _validated_entries(snapshot: dict, manifest: dict) -> list[dict]:
    _validate_inventory(snapshot)
    if (not isinstance(manifest, dict)
            or manifest.get("repository") != snapshot["repository"]
            or manifest.get("approved_issue") != 528
            or manifest.get("archive_prefix") not in APPROVED_BATCHES
            or manifest.get("cutoff_tip_date") != APPROVED_BATCHES.get(manifest.get("archive_prefix"))
            or not isinstance(manifest.get("candidates"), list)
            or len(manifest["candidates"]) != MAX_OLD_RETIREMENTS):
        raise ValueError("Retirement manifest must be the exact owner-approved batch")
    seen = set()
    for item in manifest["candidates"]:
        if (not isinstance(item, dict)
                or not isinstance(item.get("branch"), str)
                or not BRANCH_PATTERN.fullmatch(item["branch"])
                or item["branch"].startswith("-")
                or item["branch"].startswith("/")
                or ".." in item["branch"]
                or not isinstance(item.get("sha"), str)
                or not SHA_PATTERN.fullmatch(item["sha"])
                or not isinstance(item.get("tip_date"), str)
                or len(item["tip_date"]) != 10
                or item["tip_date"] > manifest["cutoff_tip_date"]
                or item["branch"] in seen
                or item["branch"] == snapshot["default_branch"]):
            raise ValueError("Retirement manifest has invalid, recent, or duplicate branch")
        seen.add(item["branch"])
    return manifest["candidates"]


def plan_retirement(snapshot: dict, manifest: dict) -> dict:
    entries = _validated_entries(snapshot, manifest)
    repository = snapshot["repository"]
    branches = {row["name"]: row for row in snapshot["branches"]}
    candidates = []
    skipped = []
    for item in entries:
        name, sha = item["branch"], item["sha"]
        branch = branches.get(name)
        if branch is None:
            skipped.append({"branch": name, "reason": "no_longer_exists"})
            continue
        if branch["commit"]["sha"] != sha:
            skipped.append({"branch": name, "reason": "branch_moved"})
            continue
        if branch["protected"]:
            skipped.append({"branch": name, "reason": "protected"})
            continue
        if any(pull["state"] == "open" and pull["head"]["ref"] == name
               and (pull["head"].get("repo") is None
                    or _same_repository(pull["head"]["repo"], repository))
               for pull in snapshot["pulls"]):
            skipped.append({"branch": name, "reason": "open_pr"})
            continue
        candidates.append({"branch": name, "sha": sha, "merged_pr": None,
                           "evidence": "approved_age", "tip_date": item["tip_date"],
                           "archive_ref": f"{manifest['archive_prefix']}/{name}"})
    return {"repository": repository, "default_branch": snapshot["default_branch"],
            "approved_count": len(entries), "candidate_count": len(candidates),
            "candidates": candidates, "skipped": skipped}


def _archive_tip(repository: str, token: str, branch: str, sha: str,
                 archive_ref: str, *, opener=None) -> None:
    """Create an immutable lightweight recovery tag and confirm its exact target."""
    if (not SHA_PATTERN.fullmatch(sha)
            or not any(archive_ref == f"{prefix}/{branch}" for prefix in APPROVED_BATCHES)):
        raise ValueError("Invalid recovery tag target")
    tag_url = _api_url(repository, "git/ref/tags/" + parse.quote(archive_ref, safe="/"))

    def matches(tag):
        object_data = tag.get("object") if isinstance(tag, dict) else None
        return (isinstance(tag, dict)
                and tag.get("ref") == f"refs/tags/{archive_ref}"
                and isinstance(object_data, dict)
                and object_data.get("type") == "commit"
                and object_data.get("sha") == sha)

    try:
        existing = _api_request(tag_url, token, opener=opener or request.urlopen)
    except RuntimeError as exc:
        if not str(exc).startswith("GitHub API request failed (HTTP 404 at "):
            raise
    else:
        if matches(existing):
            return
        raise RuntimeError("Existing recovery tag points to a different commit; branch retained")

    payload = json.dumps({"ref": f"refs/tags/{archive_ref}", "sha": sha}).encode("utf-8")
    headers = {"Accept": "application/vnd.github+json", "Content-Type": "application/json",
               "X-GitHub-Api-Version": "2022-11-28", "User-Agent": "PesaTrack-Old-Branch-Retirement",
               "Authorization": f"Bearer {token}"}
    post = request.Request(_api_url(repository, "git/refs"), data=payload,
                           headers=headers, method="POST")
    try:
        with (opener or request.urlopen)(post, timeout=30) as response:
            created = json.loads(response.read().decode("utf-8"))
    except (error.HTTPError, error.URLError, OSError, TimeoutError,
            ValueError, UnicodeDecodeError):
        raise RuntimeError("Recovery tag creation failed; branch retained") from None
    if not isinstance(created, dict) or created.get("ref") != f"refs/tags/{archive_ref}":
        raise RuntimeError("Recovery tag response invalid; branch retained")
    for attempt in range(5):
        try:
            tag = _api_request(tag_url, token, opener=opener or request.urlopen)
        except RuntimeError as exc:
            if (str(exc).startswith("GitHub API request failed (HTTP 404 at ")
                    and attempt < 4):
                if opener is None:
                    time.sleep(1)
                continue
            raise RuntimeError("Recovery tag verification failed; branch retained") from None
        if matches(tag):
            return
        raise RuntimeError("Recovery tag target not confirmed; branch retained")
    raise RuntimeError("Recovery tag not visible after bounded retries; branch retained")


def apply_retirement(repository: str, token: str, snapshot: dict, manifest: dict,
                     *, opener=None, archiver=None, pusher=None, verifier=None) -> dict:
    plan = plan_retirement(snapshot, manifest)
    if plan["repository"] != repository:
        raise ValueError("Inventory repository does not match request")
    api_opener = opener or request.urlopen
    deleted, skipped, failures = [], list(plan["skipped"]), []
    for candidate in plan["candidates"]:
        if opener is None:
            time.sleep(1)
        current = dict(candidate, default_branch=plan["default_branch"])
        try:
            eligible, reason = _recheck_candidate(repository, token, current,
                                                   opener=api_opener)
            if not eligible:
                skipped.append({"branch": candidate["branch"], "reason": reason})
                continue
            (archiver or _archive_tip)(repository, token, candidate["branch"],
                                      candidate["sha"], candidate["archive_ref"])
            (pusher or _lease_delete)(repository, token, candidate["branch"],
                                       candidate["sha"])
            (verifier or _verify_ref_absent)(repository, token, candidate["branch"],
                                             candidate["sha"], opener=api_opener)
            deleted.append({"branch": candidate["branch"], "sha": candidate["sha"],
                            "archive_ref": candidate["archive_ref"]})
        except (RuntimeError, ValueError) as exc:
            failures.append({"branch": candidate["branch"], "reason": str(exc)})
            break
    processed = len(deleted) + len(failures) + len(skipped) - len(plan["skipped"])
    return {"repository": repository, "approved_count": plan["approved_count"],
            "candidate_count": plan["candidate_count"], "processed_count": processed,
            "deferred_count": plan["candidate_count"] - processed,
            "deleted": deleted, "skipped": skipped, "failures": failures}


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", required=True)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args(argv)
    token = os.environ.get("GITHUB_TOKEN", "")
    if not token:
        parser.error("GITHUB_TOKEN is required")
    try:
        manifest = json.loads(args.manifest.read_text(encoding="utf-8"))
        snapshot = collect_inventory(args.repository, token)
        report = (apply_retirement(args.repository, token, snapshot, manifest)
                  if args.apply else plan_retirement(snapshot, manifest))
    except (RuntimeError, ValueError, OSError, json.JSONDecodeError) as exc:
        print(json.dumps({"error": str(exc)}), file=sys.stderr)
        return 1
    print(json.dumps(report, indent=2, sort_keys=True))
    return 1 if report.get("failures") else 0


if __name__ == "__main__":
    raise SystemExit(main())
