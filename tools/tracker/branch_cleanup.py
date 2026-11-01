"""Conservatively preview and remove branches proven merged into the default branch."""

from __future__ import annotations

import argparse
from collections import Counter
import json
import os
import re
import subprocess
import sys
import tempfile
import time
from pathlib import Path
from urllib import error, parse, request


API_BASE = "https://api.github.com"
REPOSITORY_PATTERN = re.compile(r"^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")
BRANCH_PATTERN = re.compile(r"^[^\x00\r\n]+$")
SHA_PATTERN = re.compile(r"^[0-9a-fA-F]{40}$")
MAX_PAGES = 20
MAX_CANDIDATES_PER_RUN = 10
MAX_SUPERSEDED_PER_RUN = 30


def _validate_repository(repository: str) -> None:
    if not isinstance(repository, str) or not REPOSITORY_PATTERN.fullmatch(repository):
        raise ValueError("Expected GitHub owner/repository, without a URL")


def _safe_http_error(exc: error.HTTPError, url: str) -> RuntimeError:
    parsed_url = parse.urlsplit(url)
    path = parsed_url.path
    headers = exc.headers or {}
    details = [f"HTTP {exc.code} at {path}"]
    request_id = headers.get("X-GitHub-Request-Id")
    remaining = headers.get("X-RateLimit-Remaining")
    reset = headers.get("X-RateLimit-Reset")
    retry_after = headers.get("Retry-After")
    rate_resource = headers.get("X-RateLimit-Resource")
    permissions = headers.get("X-Accepted-GitHub-Permissions")
    if request_id:
        details.append(f"request_id={request_id}")
    if remaining:
        details.append(f"remaining={remaining}")
    if reset:
        details.append(f"reset={reset}")
    if retry_after:
        details.append(f"retry_after={retry_after}")
    if rate_resource:
        details.append(f"rate_resource={rate_resource}")
    if permissions:
        details.append(f"accepted_permissions={permissions}")
    return RuntimeError("GitHub API request failed (" + ", ".join(details) + ")")


def _api_request(url: str, token: str, *, method: str = "GET", opener=None):
    headers = {
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent": "PesaTrack-Merged-Branch-Cleanup",
    }
    if token:
        headers["Authorization"] = f"Bearer {token}"
    api_opener = opener or request.urlopen
    req = request.Request(url, headers=headers, method=method)
    try:
        with api_opener(req, timeout=30) as response:
            if method == "DELETE" or getattr(response, "status", 200) == 204:
                return None
            body = response.read()
    except error.HTTPError as exc:
        raise _safe_http_error(exc, url) from None
    except (error.URLError, TimeoutError, OSError) as exc:
        raise RuntimeError(f"GitHub API request failed at {parse.urlsplit(url).path}: {type(exc).__name__}") from None
    try:
        value = json.loads(body.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError):
        raise RuntimeError(f"GitHub API returned invalid JSON at {parse.urlsplit(url).path}") from None
    return value


def _api_url(repository: str, path: str, **query) -> str:
    url = f"{API_BASE}/repos/{repository}"
    if path:
        url += "/" + path
    if query:
        url += "?" + parse.urlencode(query)
    return url


def _paged_rows(repository: str, token: str, path: str, *, opener, max_pages: int,
                **query) -> list[dict]:
    rows = []
    for page in range(1, max_pages + 1):
        batch = _api_request(
            _api_url(repository, path, per_page=100, page=page, **query), token,
            opener=opener,
        )
        if not isinstance(batch, list) or any(not isinstance(row, dict) for row in batch):
            raise RuntimeError(f"GitHub pagination returned invalid rows for {path}")
        rows.extend(batch)
        if len(batch) < 100:
            return rows
    raise RuntimeError(f"GitHub pagination reached the {max_pages}-page safety limit for {path}")


def collect_inventory(repository: str, token: str, *, opener=None,
                      max_pages: int = MAX_PAGES) -> dict:
    """Fetch a complete bounded snapshot; raise rather than act on truncated pages."""
    _validate_repository(repository)
    if isinstance(max_pages, bool) or not isinstance(max_pages, int) or not 1 <= max_pages <= MAX_PAGES:
        raise ValueError(f"Pagination must be bounded between one and {MAX_PAGES} pages")
    api_opener = opener or request.urlopen
    metadata = _api_request(_api_url(repository, ""), token, opener=api_opener)
    full_name = metadata.get("full_name", repository) if isinstance(metadata, dict) else None
    if not isinstance(full_name, str) or full_name.casefold() != repository.casefold():
        raise RuntimeError("GitHub repository metadata was invalid")
    default_branch = metadata.get("default_branch")
    if not isinstance(default_branch, str) or not BRANCH_PATTERN.fullmatch(default_branch):
        raise RuntimeError("Current default branch could not be verified")
    branches = _paged_rows(repository, token, "branches", opener=api_opener,
                           max_pages=max_pages)
    pulls = _paged_rows(repository, token, "pulls", opener=api_opener,
                        max_pages=max_pages, state="all")
    result = {"repository": repository, "default_branch": default_branch,
              "branches": branches, "pulls": pulls, "complete": True}
    _validate_inventory(result)
    return result


def _validate_inventory(inventory: dict) -> None:
    if not isinstance(inventory, dict) or inventory.get("complete") is not True:
        raise RuntimeError("Branch inventory is incomplete")
    repository = inventory.get("repository")
    _validate_repository(repository)
    default_branch = inventory.get("default_branch")
    if not isinstance(default_branch, str) or not BRANCH_PATTERN.fullmatch(default_branch):
        raise RuntimeError("Branch inventory has no valid default branch")
    branches = inventory.get("branches")
    pulls = inventory.get("pulls")
    if not isinstance(branches, list) or not isinstance(pulls, list):
        raise RuntimeError("Branch inventory rows are malformed")
    seen_branches = set()
    for row in branches:
        if not isinstance(row, dict):
            raise RuntimeError("Branch inventory contains a malformed branch")
        name = row.get("name")
        commit = row.get("commit")
        sha = commit.get("sha") if isinstance(commit, dict) else None
        if (not isinstance(name, str) or not BRANCH_PATTERN.fullmatch(name)
                or not isinstance(sha, str) or not SHA_PATTERN.fullmatch(sha)
                or not isinstance(row.get("protected"), bool) or name in seen_branches):
            raise RuntimeError("Branch inventory contains invalid or duplicate branch data")
        seen_branches.add(name)
    if default_branch not in seen_branches:
        raise RuntimeError("Default branch is missing from the branch inventory")
    for pull in pulls:
        if not isinstance(pull, dict) or isinstance(pull.get("number"), bool) or not isinstance(pull.get("number"), int):
            raise RuntimeError("PR inventory contains a malformed pull request")
        if pull.get("state") not in ("open", "closed"):
            raise RuntimeError("PR inventory contains an unknown pull request state")
        if pull.get("merged_at") is not None and not isinstance(pull.get("merged_at"), str):
            raise RuntimeError("PR inventory contains a malformed merge timestamp")
        head = pull.get("head")
        base = pull.get("base")
        if not isinstance(head, dict) or not isinstance(base, dict):
            raise RuntimeError("PR inventory is missing head or base data")
        if (not isinstance(head.get("ref"), str) or not BRANCH_PATTERN.fullmatch(head["ref"])
                or not isinstance(head.get("sha"), str) or not SHA_PATTERN.fullmatch(head["sha"])
                or not isinstance(base.get("ref"), str) or not BRANCH_PATTERN.fullmatch(base["ref"])):
            raise RuntimeError("PR inventory contains invalid head or base data")
        head_repo = head.get("repo")
        base_repo = base.get("repo")
        if head_repo is not None and (not isinstance(head_repo, dict)
                                      or not isinstance(head_repo.get("full_name"), str)):
            raise RuntimeError("PR inventory contains malformed head repository data")
        if base_repo is not None and (not isinstance(base_repo, dict)
                                      or not isinstance(base_repo.get("full_name"), str)):
            raise RuntimeError("PR inventory contains malformed base repository data")


def _same_repository(repository_value, repository: str) -> bool:
    return isinstance(repository_value, dict) and isinstance(repository_value.get("full_name"), str) and (
        repository_value["full_name"].casefold() == repository.casefold()
    )


def _eligible_pull(pull: dict, repository: str, default_branch: str,
                   branch_name: str, branch_sha: str) -> bool:
    head = pull.get("head")
    base = pull.get("base")
    head_sha = head.get("sha") if isinstance(head, dict) else None
    return (
        pull.get("state") == "closed"
        and isinstance(pull.get("merged_at"), str)
        and bool(pull["merged_at"])
        and isinstance(head, dict)
        and head.get("ref") == branch_name
        and isinstance(head_sha, str)
        and head_sha.casefold() == branch_sha.casefold()
        and _same_repository(head.get("repo"), repository)
        and isinstance(base, dict)
        and base.get("ref") == default_branch
        and _same_repository(base.get("repo"), repository)
    )


def _confirmed_ancestor(repository: str, token: str, branch_sha: str,
                        default_sha: str, *, opener) -> bool:
    """Accept only complete GitHub compare evidence that branch SHA is in main."""
    if (not isinstance(branch_sha, str) or not SHA_PATTERN.fullmatch(branch_sha)
            or not isinstance(default_sha, str) or not SHA_PATTERN.fullmatch(default_sha)):
        return False
    comparison = _api_request(
        _api_url(repository, f"compare/{branch_sha}...{default_sha}"), token,
        opener=opener)
    if not isinstance(comparison, dict):
        return False
    base = comparison.get("base_commit")
    merge_base = comparison.get("merge_base_commit")
    return (comparison.get("status") in ("ahead", "identical")
            and type(comparison.get("behind_by")) is int
            and comparison["behind_by"] == 0
            and type(comparison.get("ahead_by")) is int
            and comparison["ahead_by"] >= 0
            and isinstance(base, dict)
            and base.get("sha") == branch_sha
            and isinstance(merge_base, dict)
            and merge_base.get("sha") == branch_sha)


def _patch_lines_contained(old_files: list, new_files: list) -> bool:
    """Require every changed line of an old PR to appear in the successor patch."""
    if not old_files or not new_files:
        return False

    def changed_lines(files):
        lines = Counter()
        for row in files:
            if not isinstance(row, dict) or not isinstance(row.get("filename"), str):
                return None
            patch = row.get("patch")
            if not isinstance(patch, str) or not patch.strip():
                return None
            if (type(row.get("additions")) is not int
                    or type(row.get("deletions")) is not int
                    or row["additions"] < 0 or row["deletions"] < 0):
                return None
            additions = deletions = 0
            for line in patch.splitlines():
                if line.startswith("+"):
                    additions += 1
                    lines[(row["filename"], line)] += 1
                elif line.startswith("-"):
                    deletions += 1
                    lines[(row["filename"], line)] += 1
            if additions != row["additions"] or deletions != row["deletions"]:
                return None
        return lines

    old_lines, new_lines = changed_lines(old_files), changed_lines(new_files)
    return bool(old_lines) and new_lines is not None and old_lines <= new_lines


def _confirmed_superseded(repository: str, token: str, candidate: dict, *, opener) -> bool:
    """Live PR identity and complete patch inclusion, not title/age heuristics."""
    old_number, successor_number = candidate["old_pr"], candidate["successor_pr"]
    old = _api_request(_api_url(repository, f"pulls/{old_number}"), token, opener=opener)
    successor = _api_request(_api_url(repository, f"pulls/{successor_number}"), token, opener=opener)
    if (not isinstance(old, dict) or not isinstance(successor, dict)
            or old.get("number") != old_number or successor.get("number") != successor_number
            or old.get("state") != "closed" or old.get("merged_at") is not None
            or successor.get("state") != "closed" or not successor.get("merged_at")):
        return False
    old_head, old_base = old.get("head"), old.get("base")
    new_head, new_base = successor.get("head"), successor.get("base")
    if (not all(isinstance(item, dict) for item in (old_head, old_base, new_head, new_base))
            or old_head.get("ref") != candidate["branch"]
            or old_head.get("sha") != candidate["sha"]
            or old_base.get("ref") != candidate["default_branch"]
            or new_base.get("ref") != candidate["default_branch"]
            or not all(_same_repository(item.get("repo"), repository)
                       for item in (old_head, old_base, new_head, new_base))):
        return False
    old_files = _paged_rows(repository, token, f"pulls/{old_number}/files",
                            opener=opener, max_pages=MAX_PAGES)
    new_files = _paged_rows(repository, token, f"pulls/{successor_number}/files",
                            opener=opener, max_pages=MAX_PAGES)
    return _patch_lines_contained(old_files, new_files)


def _successor_files_unchanged(repository: str, token: str, old_pr: int,
                               successor_merge_sha: str, default_sha: str, *, opener) -> bool:
    """Fail closed if any old-PR path differs after the successor's merge."""
    if (not isinstance(successor_merge_sha, str)
            or not SHA_PATTERN.fullmatch(successor_merge_sha)
            or not isinstance(default_sha, str) or not SHA_PATTERN.fullmatch(default_sha)):
        return False
    old_files = _paged_rows(repository, token, f"pulls/{old_pr}/files",
                            opener=opener, max_pages=MAX_PAGES)
    if not old_files:
        return False
    for item in old_files:
        path = item.get("filename") if isinstance(item, dict) else None
        if (not isinstance(path, str) or not path or path.startswith("/")
                or item.get("status") in ("removed", "renamed")):
            return False
        content_url = "contents/" + parse.quote(path, safe="/")
        try:
            merged = _api_request(_api_url(repository, content_url, ref=successor_merge_sha),
                                  token, opener=opener)
            current = _api_request(_api_url(repository, content_url, ref=default_sha),
                                   token, opener=opener)
        except RuntimeError as exc:
            if str(exc).startswith("GitHub API request failed (HTTP 404 at "):
                return False
            raise
        if (not isinstance(merged, dict) or not isinstance(current, dict)
                or merged.get("type") != "file" or current.get("type") != "file"
                or not isinstance(merged.get("sha"), str)
                or not SHA_PATTERN.fullmatch(merged["sha"])
                or current.get("sha") != merged["sha"]):
            return False
    return True


def build_plan(inventory: dict, *, include_ancestors: bool = False,
               ancestry_checker=None, superseded_pairs=None) -> dict:
    """Classify exact merged heads; optionally include independently proved ancestors."""
    _validate_inventory(inventory)
    repository = inventory["repository"]
    default_branch = inventory["default_branch"]
    pulls = inventory["pulls"]
    default_sha = next((row["commit"]["sha"] for row in inventory["branches"]
                        if row["name"] == default_branch), None)
    if include_ancestors and (not isinstance(default_sha, str)
                              or not SHA_PATTERN.fullmatch(default_sha)
                              or ancestry_checker is None):
        raise RuntimeError("Ancestor cleanup requires a verified default head and comparator")
    candidates = []
    pairs = {}
    if superseded_pairs is not None:
        if not isinstance(superseded_pairs, list) or len(superseded_pairs) > MAX_SUPERSEDED_PER_RUN:
            raise ValueError("Superseded manifest must be a list of at most 30 entries")
        for entry in superseded_pairs:
            if (not isinstance(entry, dict)
                    or not isinstance(entry.get("branch"), str)
                    or not BRANCH_PATTERN.fullmatch(entry["branch"])
                    or not isinstance(entry.get("sha"), str)
                    or not SHA_PATTERN.fullmatch(entry["sha"])
                    or any(type(entry.get(key)) is not int or entry[key] <= 0
                           for key in ("old_pr", "successor_pr"))
                    or entry["old_pr"] == entry["successor_pr"]
                    or entry["branch"] in pairs):
                raise ValueError("Invalid or duplicate superseded manifest entry")
            pairs[entry["branch"]] = entry
    skipped = {"default_branch": 0, "protected": 0, "open_pr": 0,
               "no_merged_pr": 0}
    for branch in inventory["branches"]:
        name = branch["name"]
        sha = branch["commit"]["sha"]
        if name == default_branch:
            skipped["default_branch"] += 1
            continue
        if branch["protected"]:
            skipped["protected"] += 1
            continue
        # Missing open-PR repository identity is uncertain; protect this ref.
        if any(pull.get("state") == "open"
               and pull.get("head", {}).get("ref") == name
               and (pull["head"].get("repo") is None
                    or _same_repository(pull["head"].get("repo"), repository))
               for pull in pulls):
            skipped["open_pr"] += 1
            continue
        if superseded_pairs is not None and name not in pairs:
            skipped["no_merged_pr"] += 1
            continue
        matching = ([] if superseded_pairs is not None else
                    [pull for pull in pulls
                     if _eligible_pull(pull, repository, default_branch, name, sha)])
        if not matching:
            entry = pairs.get(name)
            if entry and entry["sha"] == sha:
                old = next((p for p in pulls if p["number"] == entry["old_pr"]), None)
                successor = next((p for p in pulls if p["number"] == entry["successor_pr"]), None)
                if (old and successor and old["state"] == "closed"
                        and old.get("merged_at") is None
                        and successor["state"] == "closed" and successor.get("merged_at")
                        and old["head"]["ref"] == name and old["head"]["sha"] == sha
                        and _same_repository(old["head"].get("repo"), repository)
                        and old["base"]["ref"] == default_branch
                        and _same_repository(old["base"].get("repo"), repository)
                        and successor["base"]["ref"] == default_branch
                        and _same_repository(successor["base"].get("repo"), repository)):
                    candidates.append({"branch": name, "sha": sha,
                                       "merged_pr": None, "evidence": "superseded",
                                       "old_pr": entry["old_pr"],
                                       "successor_pr": entry["successor_pr"]})
                    continue
            if include_ancestors and ancestry_checker(sha, default_sha):
                candidates.append({"branch": name, "sha": sha,
                                   "merged_pr": None, "evidence": "ancestor",
                                   "default_sha": default_sha})
                continue
            skipped["no_merged_pr"] += 1
            continue
        matching.sort(key=lambda pull: (pull.get("merged_at", ""), pull["number"]))
        candidates.append({"branch": name, "sha": sha,
                           "merged_pr": matching[-1]["number"]})
    return {"repository": repository, "default_branch": default_branch,
            "mode": "superseded" if superseded_pairs is not None else (
                "ancestors" if include_ancestors else "merged_pr"),
            "limit": MAX_SUPERSEDED_PER_RUN if superseded_pairs is not None else MAX_CANDIDATES_PER_RUN,
            "candidates": candidates, "skipped": skipped}


def _recheck_candidate(repository: str, token: str, candidate: dict, *, opener) -> tuple[bool, str]:
    branch_name = candidate.get("branch")
    pull_number = candidate.get("merged_pr")
    default_branch = candidate.get("default_branch")
    ancestry_mode = candidate.get("evidence") == "ancestor" and pull_number is None
    superseded_mode = candidate.get("evidence") == "superseded" and pull_number is None
    if not ancestry_mode and not superseded_mode:
        pull = _api_request(_api_url(repository, f"pulls/{pull_number}"), token, opener=opener)
        if not isinstance(pull, dict) or pull.get("number") != pull_number:
            return False, "merged_pr_unavailable"
        if not _eligible_pull(pull, repository, default_branch, branch_name, candidate["sha"]):
            return False, "merged_pr_no_longer_proves_branch"
    # Never filter by head owner: such a filter may hide head.repo=null.
    open_pulls = _paged_rows(repository, token, "pulls", opener=opener,
                             max_pages=MAX_PAGES, state="open")
    for item in open_pulls:
        if not isinstance(item, dict) or item.get("state") != "open":
            return False, "open_pr_check_invalid"
        head = item.get("head")
        head_repo = head.get("repo") if isinstance(head, dict) else None
        if (not isinstance(head, dict) or not isinstance(head.get("ref"), str)
                or (head_repo is not None and (not isinstance(head_repo, dict)
                                               or not isinstance(head_repo.get("full_name"), str)))):
            return False, "open_pr_check_invalid"
        if (head.get("ref") == branch_name
                and (head_repo is None or _same_repository(head_repo, repository))):
            return False, "open_pr_exists_or_repository_unknown"

    metadata = _api_request(_api_url(repository, ""), token, opener=opener)
    full_name = metadata.get("full_name", repository) if isinstance(metadata, dict) else None
    if not isinstance(full_name, str) or full_name.casefold() != repository.casefold():
        return False, "repository_changed"
    if metadata.get("default_branch") != default_branch:
        return False, "default_branch_changed"
    branch_url = _api_url(repository, "branches/" + parse.quote(branch_name, safe=""))
    branch = _api_request(branch_url, token, opener=opener)
    if not isinstance(branch, dict) or branch.get("name") != branch_name:
        return False, "branch_unavailable"
    commit = branch.get("commit")
    if (not isinstance(commit, dict) or commit.get("sha") != candidate.get("sha")):
        return False, "branch_moved"
    if branch.get("protected") is not False:
        return False, "protected_or_unknown"
    if superseded_mode:
        if not _confirmed_superseded(repository, token, candidate, opener=opener):
            return False, "supersession_not_proven"
        main_branch = _api_request(
            _api_url(repository, "branches/" + parse.quote(default_branch, safe="")),
            token, opener=opener)
        main_sha = (main_branch.get("commit", {}).get("sha")
                    if isinstance(main_branch, dict)
                    and isinstance(main_branch.get("commit"), dict) else None)
        if (not isinstance(main_branch, dict)
                or main_branch.get("name") != default_branch
                or not isinstance(main_sha, str)):
            return False, "default_head_unknown"
        successor = _api_request(
            _api_url(repository, f"pulls/{candidate['successor_pr']}"), token, opener=opener)
        merge_sha = successor.get("merge_commit_sha") if isinstance(successor, dict) else None
        if not _confirmed_ancestor(repository, token, merge_sha, main_sha, opener=opener):
            return False, "successor_not_in_main"
        if not _successor_files_unchanged(repository, token, candidate["old_pr"],
                                          merge_sha, main_sha, opener=opener):
            return False, "successor_files_changed_on_main"
    if ancestry_mode:
        main_branch = _api_request(
            _api_url(repository, "branches/" + parse.quote(default_branch, safe="")),
            token, opener=opener)
        if (not isinstance(main_branch, dict)
                or main_branch.get("name") != default_branch
                or not isinstance(main_branch.get("commit"), dict)
                or main_branch["commit"].get("sha") != candidate.get("default_sha")):
            return False, "default_head_changed"
        if not _confirmed_ancestor(repository, token, candidate["sha"],
                                   candidate["default_sha"], opener=opener):
            return False, "ancestry_not_proven"
    return True, ""


def _lease_delete(repository: str, token: str, branch_name: str,
                  expected_sha: str, *, runner=None) -> None:
    """Delete one exact Git ref with a remote enforced compare-and-swap."""
    _validate_repository(repository)
    if (not isinstance(branch_name, str) or not BRANCH_PATTERN.fullmatch(branch_name)
            or not isinstance(expected_sha, str) or not SHA_PATTERN.fullmatch(expected_sha)):
        raise ValueError("Invalid branch or expected commit for lease deletion")
    ref = f"refs/heads/{branch_name}"
    with tempfile.TemporaryDirectory(prefix="pesatrack-cleanup-") as directory:
        askpass = Path(directory) / "git-askpass.sh"
        askpass.write_text(
            '#!/bin/sh\ncase "$1" in\n'
            '  *Username*) printf "%s\\n" "x-access-token" ;;\n'
            '  *Password*) printf "%s\\n" "$PESATRACK_CLEANUP_GIT_TOKEN" ;;\n'
            '  *) exit 1 ;;\nesac\n', encoding="utf-8")
        askpass.chmod(0o700)
        environment = os.environ.copy()
        environment.update({
            "GIT_ASKPASS": str(askpass),
            "GIT_TERMINAL_PROMPT": "0",
            "PESATRACK_CLEANUP_GIT_TOKEN": token,
            "GIT_TRACE": "0",
            "GIT_CURL_VERBOSE": "0",
        })
        command = [
            "git", "-c", "credential.helper=", "push", "--porcelain",
            f"--force-with-lease={ref}:{expected_sha}",
            f"https://github.com/{repository}.git", f":{ref}",
        ]
        try:
            result = (runner or subprocess.run)(
                command, env=environment, capture_output=True,
                text=True, timeout=45)
        except (OSError, subprocess.TimeoutExpired):
            raise RuntimeError("Lease-checked Git deletion did not finish; result unknown") from None
        if result.returncode != 0:
            raise RuntimeError("Exact-SHA Git lease deletion rejected or unavailable")


def _verify_ref_absent(repository: str, token: str,
                       branch_name: str, expected_sha: str, *,
                       opener, sleeper=time.sleep) -> None:
    """Confirm a Git lease delete, allowing bounded GitHub read propagation."""
    url = _api_url(repository, "branches/" + parse.quote(branch_name, safe=""))
    for attempt in range(5):
        try:
            branch = _api_request(url, token, opener=opener)
        except RuntimeError as exc:
            if str(exc).startswith("GitHub API request failed (HTTP 404 at "):
                return
            # Permission/rate-limit errors are never treated as stale reads.
            raise RuntimeError(f"Branch verification failed: {exc}") from None
        if not isinstance(branch, dict):
            raise RuntimeError("Branch verification returned malformed data")
        commit = branch.get("commit")
        current_sha = commit.get("sha") if isinstance(commit, dict) else None
        if (branch.get("name") != branch_name
                or not isinstance(current_sha, str)):
            raise RuntimeError("Branch verification returned inconsistent data")
        if current_sha != expected_sha:
            raise RuntimeError("Branch changed or was recreated after lease deletion")
        if attempt < 4:
            sleeper(2)
    raise RuntimeError("Branch is still present after bounded lease-deletion verification")


def apply_plan(repository: str, token: str, inventory: dict, *, opener=None,
               pusher=None, verifier=None, include_ancestors: bool = False,
               superseded_pairs=None) -> dict:
    """Recheck each candidate and delete its exact branch ref, stopping on delete errors."""
    _validate_repository(repository)
    _validate_inventory(inventory)
    if inventory["repository"].casefold() != repository.casefold():
        raise ValueError("Inventory repository does not match the requested repository")
    api_opener = opener or request.urlopen
    checker = (lambda branch_sha, main_sha: _confirmed_ancestor(
        repository, token, branch_sha, main_sha, opener=api_opener))
    plan = build_plan(inventory, include_ancestors=include_ancestors,
                      ancestry_checker=checker if include_ancestors else None,
                      superseded_pairs=superseded_pairs)
    candidates = plan["candidates"][:plan["limit"]]
    deleted = []
    skipped = []
    failures = []
    for candidate in candidates:
        if opener is None:
            time.sleep(1)
        current = dict(candidate)
        current["default_branch"] = inventory["default_branch"]
        api_failed = False
        try:
            eligible, reason = _recheck_candidate(repository, token, current, opener=api_opener)
        except RuntimeError as exc:
            eligible, reason = False, f"recheck_api_failure: {exc}"
            api_failed = True
        if not eligible:
            if api_failed:
                failures.append({"branch": candidate["branch"],
                                 "merged_pr": candidate["merged_pr"], "reason": reason})
                break
            skipped.append({"branch": candidate["branch"], "merged_pr": candidate["merged_pr"],
                            "reason": reason})
            continue
        try:
            (pusher or _lease_delete)(repository, token, candidate["branch"],
                                       candidate["sha"])
            (verifier or _verify_ref_absent)(
                repository, token, candidate["branch"], candidate["sha"],
                opener=api_opener)
            result = {"branch": candidate["branch"], "merged_pr": candidate["merged_pr"]}
            if candidate.get("evidence"):
                result["evidence"] = candidate["evidence"]
            deleted.append(result)
        except (RuntimeError, ValueError) as exc:
            failures.append({"branch": candidate["branch"], "merged_pr": candidate["merged_pr"],
                             "reason": str(exc)})
            break
    processed_count = len(deleted) + len(skipped) + len(failures)
    return {"repository": repository, "default_branch": inventory["default_branch"],
            "candidate_count": len(plan["candidates"]), "processed_count": processed_count,
            "deferred_count": len(plan["candidates"]) - processed_count, "deleted": deleted,
            "initial_skipped": plan["skipped"], "skipped": skipped,
            "failures": failures}


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", required=True)
    parser.add_argument("--apply", action="store_true", help="delete only rechecked proven-merged branches")
    parser.add_argument("--include-ancestors", action="store_true",
                        help="opt-in: also recheck branches fully contained in current main")
    parser.add_argument("--superseded-manifest", type=Path,
                        help="owner-only reviewed JSON list of explicitly superseded PR branches")
    args = parser.parse_args(argv)
    token = os.environ.get("GITHUB_TOKEN", "")
    if not token:
        parser.error("GITHUB_TOKEN is required")
    try:
        if args.superseded_manifest and args.include_ancestors:
            raise ValueError("Superseded and ancestor modes cannot be combined")
        pairs = None
        if args.superseded_manifest:
            pairs = json.loads(args.superseded_manifest.read_text(encoding="utf-8"))
        snapshot = collect_inventory(args.repository, token)
        if args.apply:
            report = apply_plan(args.repository, token, snapshot,
                                include_ancestors=args.include_ancestors,
                                superseded_pairs=pairs)
        else:
            checker = (lambda branch_sha, main_sha: _confirmed_ancestor(
                args.repository, token, branch_sha, main_sha,
                opener=request.urlopen))
            report = build_plan(snapshot, include_ancestors=args.include_ancestors,
                                ancestry_checker=checker if args.include_ancestors else None,
                                superseded_pairs=pairs)
    except (RuntimeError, ValueError, OSError, json.JSONDecodeError) as exc:
        print(json.dumps({"error": str(exc)}, sort_keys=True), file=sys.stderr)
        return 1
    print(json.dumps(report, indent=2, sort_keys=True))
    return 1 if report.get("failures") else 0


if __name__ == "__main__":
    raise SystemExit(main())
