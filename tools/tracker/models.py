"""Small, conservative projections of public task metadata."""

from __future__ import annotations

import re

OWNER_LINE = re.compile(r"(?im)^Chat owner:\s*(CHAT-[A-Z0-9_-]+|Unclaimed)\s*$")
TRACKS_LINE = re.compile(r"(?im)^(?:Tracks|Refs)\s+#([1-9][0-9]*)\s*$")
ISSUE_PR_LINE = re.compile(r"(?im)^(?:PR|Related PRs?):\s*([^\r\n]+)$")
DEVICE_LINE = re.compile(r"(?im)^Device acceptance:\s*([^\r\n]+)")
FINAL_LINE = re.compile(r"(?im)^Final verification:\s*verified\s*$")
TASK_STATUS_LINE = re.compile(r"(?im)^Task status:\s*(Untriaged|Unclaimed|Claimed|In progress|Blocked|Paused|Superseded)\s*$")


def owner_from_body(body: object) -> str | None:
    """Only one explicit, well-formed claim counts; ambiguity stays unknown."""
    if not isinstance(body, str):
        return None
    values = set(OWNER_LINE.findall(body))
    return next(iter(values)) if len(values) == 1 else None


def task_refs(body: object) -> set[int]:
    if not isinstance(body, str):
        return set()
    return {int(match) for match in TRACKS_LINE.findall(body)}


def issue_pr_refs(body: object) -> set[int]:
    """Explicit, standalone Issue PR lines; don't guess from prose or hyperlinks."""
    if not isinstance(body, str):
        return set()
    return {int(match) for line in ISSUE_PR_LINE.findall(body)
            for match in re.findall(r"(?<!\w)#([1-9][0-9]*)\b", line)}


def device_acceptance(body: object) -> str:
    if not isinstance(body, str):
        return "not_verified"
    match = DEVICE_LINE.search(body)
    if not match:
        return "not_verified"
    status = match.group(1).strip().lower()
    if status in {"n/a", "not applicable", "not_applicable"}:
        return "not_applicable"
    if status.startswith(("verified", "passed on", "passed:")):
        return "verified"
    return "not_verified"


def final_verified(body: object) -> bool:
    return isinstance(body, str) and FINAL_LINE.search(body) is not None


def explicit_task_status(body: object) -> str | None:
    """Only preserve one recognized human task state; never infer a resolution."""
    if not isinstance(body, str):
        return None
    # The regex accepts mixed case; normalize before deduplicating or comparing.
    values = {value.capitalize() for value in TASK_STATUS_LINE.findall(body)}
    return next(iter(values)) if len(values) == 1 else None


def checked_at(value: object) -> str:
    """Keep the upstream timestamp only when it is a well-formed UTC instant."""
    if isinstance(value, str) and re.fullmatch(r"\d{4}-\d\d-\d\dT\d\d:\d\d:\d\dZ", value):
        return value
    return "unknown"
