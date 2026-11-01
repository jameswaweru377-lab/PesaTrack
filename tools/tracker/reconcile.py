"""Read-only, conservative reconciliation of GitHub task/PR/CI snapshots."""

from __future__ import annotations

from collections import defaultdict
import re

from .models import (checked_at, device_acceptance, explicit_task_status,
                     final_verified, issue_pr_refs, owner_from_body, task_refs)
from .restricted_merge import _safe_addition


AUTOMATIC_CHECKS = ["Android CI", "Security - PR Secrets"]
WITHDRAWN_VERIFICATION = re.compile(
    r"(?im)^Final verification:\s*(?:not verified|failed|withdrawn|revoked)\b")


def _automatic_docs_acceptance(issue: dict, related: list[dict], checks: list[dict],
                               snapshot: dict, linked_issues: dict[int, set[int]]) -> bool:
    """Accept only complete, merged, non-executable tracker-doc evidence."""
    body = issue.get("body") or ""
    if (not isinstance(body, str) or not related or snapshot.get("truncated")
            or device_acceptance(body) != "not_applicable"
            or WITHDRAWN_VERIFICATION.search(body)):
        return False
    number = issue["number"]
    issue_owner = owner_from_body(body)
    if not issue_owner or issue_owner == "Unclaimed":
        return False
    explicit_prs = issue_pr_refs(body)
    if (snapshot.get("historical_window_limited")
            and not {row["number"] for row in related}.issubset(explicit_prs)):
        return False
    for pull in related:
        if (not pull.get("merged_at") or task_refs(pull.get("body")) != {number}
                or owner_from_body(pull.get("body")) != issue_owner
                or len(linked_issues.get(pull["number"], set())) > 1):
            return False
        changes = pull.get("file_changes")
        if (not isinstance(changes, list) or not 0 < len(changes) < 100
                or len(changes) != len(pull.get("files") or [])):
            return False
        if any(not isinstance(change, dict) or not _safe_addition(change)
               or not change["filename"].startswith("docs/tracker/")
               for change in changes):
            return False
        if _status_for_sha(str(pull.get("head_sha") or ""), checks, AUTOMATIC_CHECKS) != "passed":
            return False
    return True


def _next_action(status: str, ci: str, device: str, warnings: list[str]) -> str:
    if status == "Verified":
        return "none"
    if status in {"Blocked", "Paused", "Superseded"}:
        return "resolve-blocker"
    if status in {"Unclaimed", "Untriaged"}:
        return "claim-or-triage-task"
    if any("base differs" in warning for warning in warnings):
        return "update-from-main"
    if ci == "failed":
        return "repair-ci"
    if ci in {"pending", "unknown"}:
        return "collect-ci-evidence"
    if status == "Merged — needs acceptance":
        return ("run-device-verification" if device == "not_verified"
                else "collect-acceptance-evidence")
    return "evaluate-safe-merge"


def _status_for_sha(head_sha: str, checks: list[dict], required: list[str]) -> str:
    same_head = [c for c in checks if c.get("head_sha") == head_sha]
    if not required or not head_sha or not same_head:
        return "unknown"
    results = []
    for name in required:
        matching = [c for c in same_head if c.get("name") == name]
        if not matching:
            return "unknown"
        # GitHub may contain old successful/failed attempts for the same SHA.
        # A newer queued rerun must not inherit an earlier green conclusion.
        row = max(enumerate(matching), key=lambda pair: (
            pair[1].get("run_id") if isinstance(pair[1].get("run_id"), int) else -1,
            pair[1].get("run_attempt") if isinstance(pair[1].get("run_attempt"), int) else -1,
            -pair[0],  # GitHub sends newest first if IDs are unavailable.
        ))[1]
        if row.get("status") != "completed":
            results.append("pending")
        elif row.get("conclusion") == "success":
            results.append("passed")
        elif row.get("conclusion") in {"failure", "cancelled", "timed_out", "action_required"}:
            results.append("failed")
        else:
            results.append("unknown")
    if "failed" in results:
        return "failed"
    if "pending" in results:
        return "pending"
    if "unknown" in results:
        return "unknown"
    return "passed"


def _combine_ci(results: list[str]) -> str:
    for state in ("failed", "pending", "unknown"):
        if state in results:
            return state
    return "passed" if results else "unknown"


def _pr_evidence(related: list[dict], owner: str, main_sha: str | None,
                 checks: list[dict], required: list[str]) -> tuple[list[dict], list[str], list[str]]:
    views, results, warnings = [], [], []
    for pull in related:
        pr_owner = owner_from_body(pull.get("body"))
        if pr_owner and owner != "Unclaimed" and pr_owner != owner:
            warnings.append(f"Claim conflict: issue owner {owner}, PR #{pull['number']} says {pr_owner}; not reassigned.")
        if owner == "Unclaimed" and pr_owner:
            warnings.append(f"PR #{pull['number']} claims {pr_owner}, but issue is unclaimed; verify ownership first.")
        if main_sha and pull.get("state") == "open" and pull.get("base_sha") != main_sha:
            warnings.append(f"PR #{pull['number']} base differs from latest main; review before merging.")
        merged = bool(pull.get("merged_at"))
        ci = _status_for_sha(str(pull.get("head_sha") or ""), checks, required)
        results.append(ci)
        views.append({
            "number": pull["number"], "state": "merged" if merged else pull.get("state", "unknown"),
            "head_sha": pull.get("head_sha"), "base_sha": pull.get("base_sha"),
            "merge_commit_sha": pull.get("merge_commit_sha") if merged else None,
            "ci": ci,
            "auto_merge": ("requested — gates unverified"
                           if "pesatrack-safe-automerge" in (pull.get("labels") or [])
                           else "not requested"),
        })
    return views, results, warnings


def _task_from_issue(issue: dict, related: list[dict], checks: list[dict],
                     required: list[str], snapshot: dict,
                     explicitly_linked: dict[int, set[int]]) -> dict:
    number = issue["number"]
    owner = owner_from_body(issue.get("body")) or "Unclaimed"
    device = device_acceptance(issue.get("body"))
    views, results, warnings = _pr_evidence(
        related, owner, snapshot.get("main_sha"), checks, required)
    combined = _combine_ci(results)
    all_merged = bool(related) and all(p.get("merged_at") for p in related)
    manual_status = explicit_task_status(issue.get("body"))
    automatic_docs = (all_merged and manual_status not in
                      {"Untriaged", "Unclaimed", "Blocked", "Paused", "Superseded"}
                      and _automatic_docs_acceptance(issue, related, checks,
                                                     snapshot, explicitly_linked))
    recorded_verified = (combined == "passed" and device in
                         {"verified", "not_applicable"} and final_verified(issue.get("body")))
    if manual_status in {"Blocked", "Paused", "Superseded"}:
        status = manual_status
    elif all_merged:
        status = "Verified" if recorded_verified or automatic_docs else "Merged — needs acceptance"
    elif related:
        status = {"passed": "CI passed", "failed": "CI failed"}.get(combined, "PR open")
    else:
        status = manual_status or ("Unclaimed" if owner == "Unclaimed" else "Claimed")
    if issue.get("state") == "closed" and status != "Verified":
        warnings.append("GitHub Issue is closed but acceptance is not proven; recheck manually.")
    mode = ("automatic-docs" if automatic_docs else "recorded" if recorded_verified
            and status == "Verified" else "evidence-required")
    return {
        "task_id": f"PT-{number}", "issue_number": number,
        "title": str(issue.get("title") or "")[:160], "owner": owner,
        "status": status, "prs": views, "ci": combined, "merged": all_merged,
        "device_acceptance": device, "warnings": warnings,
        "acceptance": {
            "mode": mode,
            "reason": ("merged tracker documentation and both exact-head checks passed"
                       if automatic_docs else "explicit verification recorded"
                       if mode == "recorded" else "additional evidence required"),
            "action": _next_action(status, combined, device, warnings),
        },
        "checked_at": checked_at(snapshot.get("checked_at")),
    }


def reconcile(snapshot: dict) -> dict:
    """Generate warnings and task status without changing any input or remote state."""
    prs = list(snapshot.get("prs") or [])
    issues = list(snapshot.get("issues") or [])
    warnings: list[str] = []
    by_task: dict[int, list[dict]] = defaultdict(list)
    path_to_open_prs: dict[str, set[int]] = defaultdict(set)
    explicitly_linked: dict[int, set[int]] = defaultdict(set)
    for issue in issues:
        if isinstance(issue.get("number"), int):
            for pr_number in issue_pr_refs(issue.get("body")):
                explicitly_linked[pr_number].add(issue["number"])
    for pr_number, claims in sorted(explicitly_linked.items()):
        if len(claims) > 1:
            warnings.append(
                f"PR #{pr_number} is named by multiple Issues "
                f"{', '.join('#' + str(number) for number in sorted(claims))}; "
                "review linkage and ownership, do not auto-reassign."
            )

    for pull in prs:
        number = pull.get("number")
        if not isinstance(number, int):
            continue
        for reference in task_refs(pull.get("body")):
            by_task[reference].append(pull)
        for reference in explicitly_linked.get(number, set()):
            if pull not in by_task[reference]:
                by_task[reference].append(pull)
        if pull.get("state") == "open" and not pull.get("merged_at"):
            for path in pull.get("files") or []:
                if isinstance(path, str) and len(path) <= 300:
                    path_to_open_prs[path].add(number)

    tracked_issue_numbers = {issue.get("number") for issue in issues}
    linked_numbers = {p.get("number") for number, matches in by_task.items()
                      if number in tracked_issue_numbers for p in matches}
    untracked_open_prs = sorted(p["number"] for p in prs
                                if isinstance(p.get("number"), int)
                                and p.get("state") == "open" and not p.get("merged_at")
                                and p["number"] not in linked_numbers)

    for path, numbers in sorted(path_to_open_prs.items()):
        if len(numbers) > 1:
            warnings.append(f"Open PRs {', '.join('#' + str(n) for n in sorted(numbers))} both touch {path}; coordinate, do not auto-merge.")

    checks = list(snapshot.get("checks") or [])
    required = list(snapshot.get("required_checks") or ["Android CI"])
    tasks = []
    seen_issues: set[int] = set()
    for issue in sorted(issues, key=lambda item: int(item.get("number", 0))):
        number = issue.get("number")
        if not isinstance(number, int) or number in seen_issues:
            continue
        seen_issues.add(number)
        related = sorted(by_task.get(number, []), key=lambda p: p["number"])
        tasks.append(_task_from_issue(issue, related, checks, required,
                                      snapshot, explicitly_linked))

    for pull in prs:
        if not task_refs(pull.get("body")) and isinstance(pull.get("number"), int):
            warnings.append(f"PR #{pull['number']} has no Tracks/Refs task linkage; verify ownership.")
    return {"checked_at": checked_at(snapshot.get("checked_at")),
            "tasks": tasks, "warnings": warnings,
            "untracked_open_prs": untracked_open_prs,
            "historical_window_limited": bool(snapshot.get("historical_window_limited")),
            "incomplete": bool(snapshot.get("truncated"))}
