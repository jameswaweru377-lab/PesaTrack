"""Read-only, conservative reconciliation of GitHub task/PR/CI snapshots."""

from __future__ import annotations

from collections import defaultdict

from .models import (checked_at, device_acceptance, explicit_task_status,
                     final_verified, issue_pr_refs, owner_from_body, task_refs)


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


def reconcile(snapshot: dict) -> dict:
    """Generate warnings and task status without changing any input or remote state."""
    prs = list(snapshot.get("prs") or [])
    issues = list(snapshot.get("issues") or [])
    checks = list(snapshot.get("checks") or [])
    required = list(snapshot.get("required_checks") or ["Android CI"])
    main_sha = snapshot.get("main_sha")
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

    tasks = []
    seen_issues: set[int] = set()
    for issue in sorted(issues, key=lambda item: int(item.get("number", 0))):
        number = issue.get("number")
        if not isinstance(number, int) or number in seen_issues:
            continue
        seen_issues.add(number)
        owner = owner_from_body(issue.get("body")) or "Unclaimed"
        device = device_acceptance(issue.get("body"))
        related = sorted(by_task.get(number, []), key=lambda p: p["number"])
        task_warnings: list[str] = []
        pr_views = []
        ci_results = []
        for pull in related:
            pr_owner = owner_from_body(pull.get("body"))
            if pr_owner and owner != "Unclaimed" and pr_owner != owner:
                task_warnings.append(f"Claim conflict: issue owner {owner}, PR #{pull['number']} says {pr_owner}; not reassigned.")
            if owner == "Unclaimed" and pr_owner:
                task_warnings.append(f"PR #{pull['number']} claims {pr_owner}, but issue is unclaimed; verify ownership first.")
            if main_sha and pull.get("state") == "open" and pull.get("base_sha") != main_sha:
                task_warnings.append(f"PR #{pull['number']} base differs from latest main; review before merging.")
            merged = bool(pull.get("merged_at"))
            ci = _status_for_sha(str(pull.get("head_sha") or ""), checks, required)
            ci_results.append(ci)
            pr_views.append({
                "number": pull["number"], "state": "merged" if merged else pull.get("state", "unknown"),
                "head_sha": pull.get("head_sha"), "base_sha": pull.get("base_sha"),
                "merge_commit_sha": pull.get("merge_commit_sha") if merged else None,
                "ci": ci,
                # A label requests consideration, not approval or merge eligibility.
                "auto_merge": ("requested — gates unverified"
                               if "pesatrack-safe-automerge" in (pull.get("labels") or [])
                               else "not requested"),
            })

        combined = _combine_ci(ci_results)
        all_merged = bool(related) and all(p.get("merged_at") for p in related)
        manual_status = explicit_task_status(issue.get("body"))
        if manual_status in {"Blocked", "Paused", "Superseded"}:
            status = manual_status
        elif all_merged:
            status = ("Verified" if combined == "passed" and device in
                      {"verified", "not_applicable"} and final_verified(issue.get("body"))
                      else "Merged — needs acceptance")
        elif related:
            status = {"passed": "CI passed", "failed": "CI failed"}.get(combined, "PR open")
        else:
            status = manual_status or ("Unclaimed" if owner == "Unclaimed" else "Claimed")
        if issue.get("state") == "closed" and status != "Verified":
            task_warnings.append("GitHub Issue is closed but acceptance is not proven; recheck manually.")
        tasks.append({
            "task_id": f"PT-{number}", "issue_number": number,
            "title": str(issue.get("title") or "")[:160], "owner": owner,
            "status": status, "prs": pr_views, "ci": combined, "merged": all_merged,
            "device_acceptance": device, "warnings": task_warnings,
            "checked_at": checked_at(snapshot.get("checked_at")),
        })

    for pull in prs:
        if not task_refs(pull.get("body")) and isinstance(pull.get("number"), int):
            warnings.append(f"PR #{pull['number']} has no Tracks/Refs task linkage; verify ownership.")
    return {"checked_at": checked_at(snapshot.get("checked_at")),
            "tasks": tasks, "warnings": warnings,
            "untracked_open_prs": untracked_open_prs,
            "historical_window_limited": bool(snapshot.get("historical_window_limited")),
            "incomplete": bool(snapshot.get("truncated"))}
