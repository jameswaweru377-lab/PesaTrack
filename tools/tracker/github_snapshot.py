"""Bounded read-only GitHub REST collection; no financial data or mutations."""

from __future__ import annotations

from datetime import datetime, timezone
import json
import os
from pathlib import Path
import re
import tempfile
from urllib import error, parse, request

from .models import issue_pr_refs, task_refs

REPOSITORY_PATTERN = re.compile(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+\Z")
BASE_URL = "https://api.github.com"
SNAPSHOT_FIELDS = {"repository", "main_sha", "issues", "prs", "checks",
                   "historical_window_limited", "truncated", "checked_at"}


def _validated_snapshot(snapshot: object, repository: str) -> dict:
    if (not isinstance(snapshot, dict) or not SNAPSHOT_FIELDS.issubset(snapshot)
            or not isinstance(snapshot.get("main_sha"), str)
            or not re.fullmatch(r"[0-9a-f]{40}", snapshot["main_sha"])
            or any(not isinstance(snapshot.get(key), list)
                   for key in ("issues", "prs", "checks"))
            or not isinstance(snapshot.get("historical_window_limited"), bool)
            or not isinstance(snapshot.get("truncated"), bool)
            or not isinstance(snapshot.get("checked_at"), str)):
        raise RuntimeError("Cached GitHub snapshot is invalid")
    if snapshot["repository"] != repository:
        raise RuntimeError("Cached GitHub snapshot does not match repository")
    return snapshot


def save_snapshot(path: str | Path, snapshot: dict) -> None:
    """Atomically store a validated per-run snapshot outside the repository."""
    target = Path(path)
    _validated_snapshot(snapshot, snapshot.get("repository"))
    target.parent.mkdir(parents=True, exist_ok=True)
    fd, temporary = tempfile.mkstemp(prefix="pesatrack-snapshot-", dir=target.parent)
    try:
        os.chmod(temporary, 0o600)
        with os.fdopen(fd, "w", encoding="utf-8") as output:
            json.dump(snapshot, output, separators=(",", ":"))
        os.replace(temporary, target)
    except Exception:
        try:
            os.close(fd)
        except OSError:
            pass
        try:
            os.unlink(temporary)
        except OSError:
            pass
        raise


def load_snapshot(path: str | Path, repository: str) -> dict:
    """Read a complete same-repository snapshot from the current runner."""
    try:
        snapshot = json.loads(Path(path).read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError):
        raise RuntimeError("Cached GitHub snapshot is unavailable or invalid") from None
    _validated_snapshot(snapshot, repository)
    return snapshot


def _open_json(url: str, headers: dict, timeout: int):
    req = request.Request(url, headers=headers, method="GET")
    try:
        with request.urlopen(req, timeout=timeout) as response:
            return json.load(response)
    except error.HTTPError as failure:
        # Never echo the response body; it might include Issue text or secrets.
        path = parse.urlsplit(url).path
        details = []
        headers = failure.headers or {}
        safe_headers = (
            ("X-GitHub-Request-Id", "request_id", r"[A-Za-z0-9-]{1,80}"),
            ("X-RateLimit-Resource", "rate_limit_resource", r"[a-z_]{1,40}"),
            ("X-RateLimit-Remaining", "rate_limit_remaining", r"[0-9]{1,12}"),
            ("X-RateLimit-Reset", "rate_limit_reset", r"[0-9]{1,12}"),
            ("Retry-After", "retry_after", r"[0-9]{1,12}"),
            ("X-Accepted-GitHub-Permissions", "accepted_permissions",
             r"[A-Za-z0-9_, =;-]{1,160}"),
        )
        for header, label, pattern in safe_headers:
            value = headers.get(header)
            if isinstance(value, str) and re.fullmatch(pattern, value):
                details.append(f"{label}={value}")
        suffix = f"; {'; '.join(details)}" if details else ""
        raise RuntimeError(
            f"GitHub API request failed with HTTP {failure.code} (GET {path}{suffix})"
        ) from None
    except (error.URLError, TimeoutError):
        raise RuntimeError("GitHub API unavailable or timed out") from None


def fetch_snapshot(token: str, repository: str, *, opener=None, max_pages: int = 3) -> dict:
    """Fetch a conservative current snapshot without changing any GitHub state."""
    if not isinstance(repository, str) or not REPOSITORY_PATTERN.fullmatch(repository):
        raise ValueError("Expected GitHub owner/repository, without a URL")
    if not 1 <= max_pages <= 5:
        raise ValueError("Pagination must be bounded between one and five pages")
    open_json = opener or _open_json
    headers = {"Accept": "application/vnd.github+json",
               "X-GitHub-Api-Version": "2022-11-28",
               "User-Agent": "PesaTrack-ReadOnly-Development-Tracker"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    prefix = f"{BASE_URL}/repos/{repository}/"

    def get(path: str, **query):
        url = prefix + path
        if query:
            url += "?" + parse.urlencode(query)
        return open_json(url, headers, 10)

    def paged(path: str, **query) -> list[dict]:
        result = []
        for page in range(1, max_pages + 1):
            batch = get(path, per_page=100, page=page, **query)
            if not isinstance(batch, list):
                raise RuntimeError("GitHub pagination returned an unexpected shape")
            result.extend(batch)
            if len(batch) < 100:
                break
        return result

    main = get("commits/main")
    main_sha = main.get("sha")
    if not isinstance(main_sha, str) or not re.fullmatch(r"[0-9a-f]{40}", main_sha):
        raise RuntimeError("Current main commit could not be verified")

    issues = [
        {"number": item["number"], "title": item.get("title"),
         "state": item.get("state"), "body": item.get("body"),
         "labels": [entry.get("name") for entry in item.get("labels", [])
                    if isinstance(entry, dict)]}
        for item in paged("issues", state="all", labels="pesatrack-tracker")
        if "pull_request" not in item and isinstance(item.get("number"), int)
    ]

    # Open PRs must not disappear behind hundreds of recently closed PRs.
    # Collect them independently, then add only relevant recent/explicit history.
    open_pulls = [pull for pull in paged("pulls", state="open")
                  if pull.get("state") == "open"]
    recent_closed = [pull for pull in paged("pulls", state="closed")
                     if pull.get("state") != "open"]
    tracked = {item["number"] for item in issues}
    explicitly_linked_prs = set().union(*(issue_pr_refs(item.get("body")) for item in issues))
    relevant_closed = [pull for pull in recent_closed
                       if task_refs(pull.get("body")) & tracked
                       or pull.get("number") in explicitly_linked_prs]
    candidates = (open_pulls + relevant_closed)[:60]
    # Historical PRs eventually fall outside the bounded recent-PR pages.
    # Fetch only explicit references (never guessed references) with a strict cap.
    recent_numbers = {pull.get("number") for pull in candidates}
    missing_historical = sorted(explicitly_linked_prs - recent_numbers)
    incomplete_historical = len(missing_historical) > 20
    for number in missing_historical[:20]:
        if len(candidates) >= 60:
            incomplete_historical = True
            break
        try:
            pull = get(f"pulls/{number}")
        except RuntimeError:
            incomplete_historical = True
            continue
        if isinstance(pull, dict) and pull.get("number") == number:
            candidates.append(pull)
        else:
            incomplete_historical = True
    prs = []
    checks = []
    for pull in candidates:
        number = pull.get("number")
        if not isinstance(number, int):
            continue
        head_sha = (pull.get("head") or {}).get("sha")
        if not isinstance(head_sha, str) or not re.fullmatch(r"[0-9a-f]{40}", head_sha):
            continue
        files = paged(f"pulls/{number}/files")
        normalized = {
            "number": number, "state": pull.get("state"),
            "body": pull.get("body"), "head_sha": head_sha,
            "base_ref": (pull.get("base") or {}).get("ref"),
            "base_sha": (pull.get("base") or {}).get("sha"),
            "merge_commit_sha": pull.get("merge_commit_sha"),
            "merged_at": pull.get("merged_at"),
            "labels": [item.get("name") for item in pull.get("labels", [])
                       if isinstance(item, dict) and isinstance(item.get("name"), str)],
            "files": [row["filename"] for row in files if isinstance(row.get("filename"), str)],
            "file_changes": [{"filename": row.get("filename"), "status": row.get("status")}
                             for row in files if isinstance(row, dict)],
        }
        prs.append(normalized)
        relevant_to_task = (bool(task_refs(pull.get("body")) & tracked)
                            or number in explicitly_linked_prs)
        if not relevant_to_task:
            continue
        runs = get("actions/runs", head_sha=head_sha, per_page=100, event="pull_request")
        for run in runs.get("workflow_runs", []):
            if run.get("head_sha") != head_sha:
                continue
            checks.append({
                "head_sha": head_sha, "name": run.get("name"),
                "status": run.get("status"), "conclusion": run.get("conclusion"),
                "run_id": run.get("id"), "run_attempt": run.get("run_attempt"),
            })

    closed_history_capped = len(recent_closed) >= max_pages * 100
    return {
        "checked_at": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "repository": repository,
        "main_sha": main_sha, "issues": issues, "prs": prs,
        "checks": checks, "required_checks": ["Android CI"],
        # The Project planner separately protects existing stronger evidence
        # against downgrades when old unlinked PRs fall beyond this window.
        "historical_window_limited": closed_history_capped,
        "truncated": (incomplete_historical or len(candidates) == 60
                      or len(open_pulls) >= max_pages * 100
                      or len(issues) >= max_pages * 100),
    }
