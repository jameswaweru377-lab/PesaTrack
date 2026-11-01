"""Conservatively preview and remove branches proven merged into the default branch."""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import time
from urllib import error, parse, request


API_BASE = "https://api.github.com"
REPOSITORY_PATTERN = re.compile(r"^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")
BRANCH_PATTERN = re.compile(r"^[^\x00\r\n]+$")
SHA_PATTERN = re.compile(r"^[0-9a-fA-F]{40}$")
MAX_PAGES = 20
MAX_CANDIDATES_PER_RUN = 170


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


def build_plan(inventory: dict) -> dict:
    """Classify only exact, same-repository merged PR heads as deletion candidates."""
    _validate_inventory(inventory)
    repository = inventory["repository"]
    default_branch = inventory["default_branch"]
    pulls = inventory["pulls"]
    candidates = []
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
        associated = [pull for pull in pulls
                      if pull.get("head", {}).get("ref") == name
                      and _same_repository(pull.get("head", {}).get("repo"), repository)]
        if any(pull.get("state") == "open" for pull in associated):
            skipped["open_pr"] += 1
            continue
        matching = [pull for pull in associated
                    if _eligible_pull(pull, repository, default_branch, name, sha)]
        if not matching:
            skipped["no_merged_pr"] += 1
            continue
        matching.sort(key=lambda pull: (pull.get("merged_at", ""), pull["number"]))
        candidates.append({"branch": name, "sha": sha,
                           "merged_pr": matching[-1]["number"]})
    return {"repository": repository, "default_branch": default_branch,
            "candidates": candidates, "skipped": skipped}


def _recheck_candidate(repository: str, token: str, candidate: dict, *, opener) -> tuple[bool, str]:
    branch_name = candidate.get("branch")
    pull_number = candidate.get("merged_pr")
    pull = _api_request(_api_url(repository, f"pulls/{pull_number}"), token, opener=opener)
    default_branch = candidate.get("default_branch")
    if not isinstance(pull, dict) or pull.get("number") != pull_number:
        return False, "merged_pr_unavailable"
    if not _eligible_pull(pull, repository, default_branch, branch_name, candidate["sha"]):
        return False, "merged_pr_no_longer_proves_branch"
    owner = repository.split("/", 1)[0]
    open_url = _api_url(repository, "pulls", state="open", head=f"{owner}:{branch_name}", per_page=100)
    open_pulls = _api_request(open_url, token, opener=opener)
    if not isinstance(open_pulls, list) or any(not isinstance(item, dict) for item in open_pulls):
        return False, "open_pr_check_invalid"
    for item in open_pulls:
        if item.get("state") != "open":
            continue
        head = item.get("head")
        head_repo = head.get("repo") if isinstance(head, dict) else None
        if (not isinstance(head, dict) or not isinstance(head.get("ref"), str)
                or (head_repo is not None and (not isinstance(head_repo, dict)
                                               or not isinstance(head_repo.get("full_name"), str)))):
            return False, "open_pr_check_invalid"
        if (head.get("ref") == branch_name
                and _same_repository(head_repo, repository)):
            return False, "open_pr_exists"

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
    return True, ""


def apply_plan(repository: str, token: str, inventory: dict, *, opener=None) -> dict:
    """Recheck each candidate and delete its exact branch ref, stopping on delete errors."""
    _validate_repository(repository)
    _validate_inventory(inventory)
    if inventory["repository"].casefold() != repository.casefold():
        raise ValueError("Inventory repository does not match the requested repository")
    plan = build_plan(inventory)
    api_opener = opener or request.urlopen
    candidates = plan["candidates"][:MAX_CANDIDATES_PER_RUN]
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
        ref_path = "git/refs/heads/" + parse.quote(candidate["branch"], safe="/")
        try:
            _api_request(_api_url(repository, ref_path), token, method="DELETE", opener=api_opener)
            deleted.append({"branch": candidate["branch"], "merged_pr": candidate["merged_pr"]})
        except RuntimeError as exc:
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
    args = parser.parse_args(argv)
    token = os.environ.get("GITHUB_TOKEN", "")
    if not token:
        parser.error("GITHUB_TOKEN is required")
    try:
        snapshot = collect_inventory(args.repository, token)
        if args.apply:
            report = apply_plan(args.repository, token, snapshot)
        else:
            report = build_plan(snapshot)
    except (RuntimeError, ValueError) as exc:
        print(json.dumps({"error": str(exc)}, sort_keys=True), file=sys.stderr)
        return 1
    print(json.dumps(report, indent=2, sort_keys=True))
    return 1 if report.get("failures") else 0


if __name__ == "__main__":
    raise SystemExit(main())
