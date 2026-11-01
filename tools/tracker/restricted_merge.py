"""Fail-closed tracker auto-merge, with an owner-authored documentation lane."""

from __future__ import annotations

from pathlib import PurePosixPath
import argparse
import json
import os
import re
from urllib import error, parse, request


REPOSITORY = "jameswaweru377-lab/PesaTrack"
APPROVAL_LABEL = "pesatrack-safe-automerge"
REQUIRED_CI = ("Android CI", "Security - PR Secrets")
SHA = re.compile(r"[0-9a-f]{40}\Z")
TASK = re.compile(r"^Tracks #(\d+)\s*$", re.MULTILINE)
OWNER = re.compile(r"^Chat owner: (CHAT-[A-Z0-9_-]+)\s*$", re.MULTILINE)


def _labels(data: dict) -> set[str]:
    labels = data.get("labels")
    if not isinstance(labels, list):
        return set()
    return {item.get("name") for item in labels if isinstance(item, dict)
            and isinstance(item.get("name"), str)}


def _safe_addition(file: dict) -> bool:
    path = file.get("filename")
    if not isinstance(path, str) or file.get("status") != "added":
        return False
    if not path or "\\" in path or path.startswith("/"):
        return False
    parts = path.split("/")
    if any(part in ("", ".", "..") for part in parts):
        return False
    normalized = str(PurePosixPath(path))
    if path != normalized:
        return False
    return (
        (path.startswith("docs/tracker/") and path.endswith(".md"))
        or (path.startswith("tools/tracker/tests/") and path.endswith(".py"))
    )


def _unattended_documentation(pr: dict, files: list[dict]) -> bool:
    """Only repo-owner PRs adding non-executable tracker Markdown can skip consent."""
    return (_owner_authored(pr) and bool(files)
            and all(isinstance(row, dict) and _safe_addition(row)
                    and row["filename"].startswith("docs/tracker/") for row in files))


def _owner_authored(pr: dict) -> bool:
    author = pr.get("user")
    return (isinstance(author, dict)
            and author.get("login") == REPOSITORY.split("/")[0])


def _owner_approved(comments: list, head_sha: str) -> bool:
    exact = f"PESATRACK SAFE AUTO-MERGE APPROVED: {head_sha}"
    return isinstance(comments, list) and any(
        isinstance(item, dict) and isinstance(item.get("user"), dict)
        and item["user"].get("login") == REPOSITORY.split("/")[0]
        and isinstance(item.get("body"), str) and item["body"].strip() == exact
        for item in comments)


def _ci_ok(runs: list, head_sha: str) -> bool:
    if not isinstance(runs, list):
        return False
    for name in REQUIRED_CI:
        matches = [run for run in runs if isinstance(run, dict)
                   and run.get("name") == name and run.get("head_sha") == head_sha
                   and run.get("event") == "pull_request"
                   and isinstance(run.get("id"), int)
                   and isinstance(run.get("run_attempt"), int)]
        if not matches:
            return False
        latest = max(matches, key=lambda run: (run["id"], run["run_attempt"]))
        if latest.get("status") != "completed" or latest.get("conclusion") != "success":
            return False
    return True


def evaluate_candidate(pr: dict, *, main_sha: str, approvals: list, issue: dict,
                       files: list, open_pr_files: list, runs: list) -> tuple[bool, str]:
    """Return (allowed, short reason), never guessing when evidence is partial."""
    if not isinstance(pr, dict) or not isinstance(main_sha, str) or not SHA.fullmatch(main_sha):
        return False, "invalid-snapshot"
    head, base = pr.get("head") or {}, pr.get("base") or {}
    head_sha = head.get("sha")
    if not isinstance(head_sha, str) or not SHA.fullmatch(head_sha):
        return False, "invalid-head"
    if pr.get("state") != "open" or pr.get("draft") is not False or pr.get("mergeable") is not True:
        return False, "not-ready"
    head_repo = head.get("repo")
    if (base.get("ref") != "main" or not isinstance(head_repo, dict)
            or head_repo.get("full_name") != REPOSITORY):
        return False, "wrong-repository"
    if base.get("sha") != main_sha or pr.get("merge_base_sha") != main_sha:
        return False, "stale-main"
    body = pr.get("body") or ""
    if not isinstance(body, str):
        return False, "task-claim-missing"
    task, owner = TASK.search(body), OWNER.search(body)
    if (len(TASK.findall(body)) != 1 or len(OWNER.findall(body)) != 1
            or not task or not owner or not isinstance(issue, dict)):
        return False, "task-claim-missing"
    issue_body = issue.get("body") or ""
    if not isinstance(issue_body, str):
        return False, "task-claim-mismatch"
    issue_owner = OWNER.search(issue_body)
    if (issue.get("number") != int(task.group(1)) or issue.get("state") != "open"
            or "pesatrack-tracker" not in _labels(issue) or not issue_owner
            or issue_owner.group(1) != owner.group(1)):
        return False, "task-claim-mismatch"
    if not isinstance(files, list) or not files or len(files) >= 100:
        return False, "incomplete-files"
    if any(not isinstance(file, dict) or not _safe_addition(file) for file in files):
        return False, "unsafe-path"
    unattended = _unattended_documentation(pr, files)
    if not unattended and (APPROVAL_LABEL not in _labels(pr)
                           or not _owner_approved(approvals, head_sha)):
        return False, "owner-approval-missing"
    if not isinstance(open_pr_files, list):
        return False, "incomplete-overlap"
    own_paths = {file["filename"] for file in files}
    for other in open_pr_files:
        if not isinstance(other, dict) or not isinstance(other.get("files"), list):
            return False, "incomplete-overlap"
        if other.get("number") != pr.get("number") and own_paths.intersection(other["files"]):
            return False, "overlap"
    if not _ci_ok(runs, head_sha):
        return False, "ci-not-current"
    return True, "eligible"


class GitHubClient:
    """Tiny API boundary; never execute untrusted PR code or print response bodies."""

    def __init__(self, repository: str, token: str):
        self.prefix = f"https://api.github.com/repos/{repository}/"
        self.headers = {"Accept": "application/vnd.github+json",
                        "X-GitHub-Api-Version": "2022-11-28",
                        "Authorization": f"Bearer {token}",
                        "User-Agent": "PesaTrack-Restricted-Automerge"}

    def _request(self, path: str, *, method: str = "GET", payload=None, **query):
        url = self.prefix + path
        if query:
            url += "?" + parse.urlencode(query)
        data = json.dumps(payload).encode("utf-8") if payload is not None else None
        headers = dict(self.headers)
        if data is not None:
            headers["Content-Type"] = "application/json"
        req = request.Request(url, data=data, headers=headers, method=method)
        try:
            with request.urlopen(req, timeout=15) as response:
                return json.load(response)
        except error.HTTPError as exc:
            raise RuntimeError(f"GitHub API returned HTTP {exc.code}") from None
        except (error.URLError, TimeoutError, ValueError):
            raise RuntimeError("GitHub API unavailable or returned invalid JSON") from None

    def get(self, path: str, **query):
        return self._request(path, **query)

    def merge(self, number: int, expected_head_sha: str):
        return self._request(f"pulls/{number}/merge", method="PUT",
                             payload={"sha": expected_head_sha, "merge_method": "merge"})


def _bounded_list(client, path: str, **query) -> list:
    value = client.get(path, per_page=100, **query)
    if not isinstance(value, list) or len(value) >= 100:
        raise RuntimeError("GitHub coverage incomplete; no merge allowed")
    return value


def _inspect(client, number: int) -> tuple[str, str | None]:
    main = client.get("commits/main")
    pr = client.get(f"pulls/{number}")
    main_sha = main.get("sha") if isinstance(main, dict) else None
    head_sha = (pr.get("head") or {}).get("sha") if isinstance(pr, dict) else None
    if not isinstance(main_sha, str) or not SHA.fullmatch(main_sha):
        raise RuntimeError("Could not verify current main")
    if not isinstance(head_sha, str) or not SHA.fullmatch(head_sha):
        return "invalid-head", None
    compared = client.get(f"compare/{main_sha}...{head_sha}")
    merge_base = (compared.get("merge_base_commit") or {}).get("sha")
    pr["merge_base_sha"] = merge_base
    comments = _bounded_list(client, f"issues/{number}/comments")
    files = _bounded_list(client, f"pulls/{number}/files")
    body = pr.get("body") or ""
    task = TASK.search(body) if isinstance(body, str) else None
    issue = client.get(f"issues/{task.group(1)}") if task else {}
    all_open = _bounded_list(client, "pulls", state="open")
    other_files = []
    for other in all_open:
        other_number = other.get("number") if isinstance(other, dict) else None
        if not isinstance(other_number, int):
            raise RuntimeError("Open PR record incomplete")
        if other_number != number:
            changed = _bounded_list(client, f"pulls/{other_number}/files")
            other_files.append({"number": other_number,
                                "files": [row.get("filename") for row in changed]})
    actions = client.get("actions/runs", head_sha=head_sha, event="pull_request", per_page=100)
    runs = actions.get("workflow_runs") if isinstance(actions, dict) else None
    if not isinstance(runs, list) or len(runs) >= 100:
        raise RuntimeError("CI coverage incomplete; no merge allowed")
    allowed, reason = evaluate_candidate(pr, main_sha=main_sha, approvals=comments,
                                         issue=issue, files=files, open_pr_files=other_files,
                                         runs=runs)
    return ("eligible" if allowed else reason), head_sha


def run(repository: str, token: str, *, apply: bool = False, client=None) -> list[dict]:
    """Scan owner-authored and opted-in PRs; merge at most one per invocation."""
    if repository != REPOSITORY:
        raise ValueError("Only the PesaTrack repository is allowed")
    if apply and not token:
        raise ValueError("Applying a merge requires an authenticated token")
    github = client or GitHubClient(repository, token)
    pulls = _bounded_list(github, "pulls", state="open")
    candidates = [pr for pr in pulls if isinstance(pr, dict)
                  and (APPROVAL_LABEL in _labels(pr)
                       or (_owner_authored(pr)
                           and isinstance(pr.get("body"), str)
                           and TASK.search(pr["body"])))]
    results = []
    for pr in candidates:
        number = pr.get("number")
        if not isinstance(number, int) or number <= 0:
            raise RuntimeError("Candidate PR number is invalid")
        status, head_sha = _inspect(github, number)
        if status == "eligible" and apply:
            # Recheck every evidence source and current base immediately before mutation.
            status, current_head = _inspect(github, number)
            if status == "eligible" and current_head == head_sha:
                response = github.merge(number, head_sha)
                if not isinstance(response, dict) or response.get("merged") is not True:
                    raise RuntimeError("GitHub did not confirm the guarded merge")
                status = "merged"
        results.append({"pr": number, "status": status})
        if status == "merged":
            break
    return results


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", default=os.environ.get("GITHUB_REPOSITORY"))
    parser.add_argument("--apply", action="store_true", help="Merge opted-in eligible PRs")
    args = parser.parse_args(argv)
    app_token = os.environ.get("PESATRACK_AUTOMERGE_APP_TOKEN", "")
    default_token = os.environ.get("GITHUB_TOKEN", "")
    try:
        if args.apply and (not app_token or app_token == default_token):
            raise ValueError("A separate GitHub App installation token is required to merge")
        token = app_token or default_token
        results = run(args.repository, token, apply=args.apply)
    except (RuntimeError, ValueError) as exc:
        print(f"Restricted auto-merge stopped: {exc}")
        return 2
    lines = ["## Restricted PesaTrack auto-merge", "", "| PR | Result |", "|---|---|"]
    lines.extend(f"| #{row['pr']} | {row['status']} |" for row in results)
    if not results:
        lines.append("| — | No opted-in open PRs |")
    rendered = "\n".join(lines) + "\n"
    print(rendered)
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as output:
            output.write(rendered)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
