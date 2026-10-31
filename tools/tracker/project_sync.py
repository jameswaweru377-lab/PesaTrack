"""Opt-in GitHub Project v2 mirror; canonical ownership stays in Issues.

Only the dedicated `Tracker status` single-select field may be changed.
Project/item creation and human-maintained Status/approval fields are untouched.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
from urllib import error, request

from .github_snapshot import fetch_snapshot
from .reconcile import reconcile


PROJECT_TITLE = "PesaTrack Development Tracker"
FIELD_NAME = "Tracker status"
PROJECT_ID = re.compile(r"PVT_[A-Za-z0-9_]+\Z")
GRAPHQL_URL = "https://api.github.com/graphql"

PROJECT_QUERY = """
query($project: ID!, $cursor: String) {
  node(id: $project) {
    ... on ProjectV2 {
      id title
      fields(first: 100) { nodes {
        ... on ProjectV2SingleSelectField { id name options { id name } }
      } }
      items(first: 100, after: $cursor) {
        pageInfo { hasNextPage endCursor }
        nodes {
          id isArchived
          content { ... on Issue { number repository { nameWithOwner } } }
          fieldValueByName(name: "Tracker status") {
            ... on ProjectV2ItemFieldSingleSelectValue { name }
          }
        }
      }
    }
  }
}
"""

UPDATE_QUERY = """
mutation($project: ID!, $item: ID!, $field: ID!, $option: String!) {
  updateProjectV2ItemFieldValue(input: {
    projectId: $project, itemId: $item, fieldId: $field,
    value: { singleSelectOptionId: $option }
  }) { projectV2Item { id } }
}
"""


def graphql(token: str, query: str, variables: dict) -> dict:
    """Return only parsed GitHub GraphQL data, without printing response bodies."""
    payload = json.dumps({"query": query, "variables": variables}).encode("utf-8")
    req = request.Request(GRAPHQL_URL, data=payload, method="POST", headers={
        "Authorization": f"Bearer {token}",
        "Accept": "application/vnd.github+json",
        "Content-Type": "application/json",
        "User-Agent": "PesaTrack-Project-Status-Mirror",
    })
    try:
        with request.urlopen(req, timeout=12) as response:
            result = json.load(response)
    except error.HTTPError as exc:
        raise RuntimeError(f"Project access failed with HTTP {exc.code}") from None
    except (error.URLError, TimeoutError):
        raise RuntimeError("GitHub Project API is unavailable") from None
    if not isinstance(result, dict) or result.get("errors") or not isinstance(result.get("data"), dict):
        raise RuntimeError("GitHub Project query was rejected")
    return result["data"]


def load_project(token: str, project_id: str, *, caller=graphql) -> dict:
    """Read a bounded snapshot before any optional mutation."""
    if not PROJECT_ID.fullmatch(project_id):
        raise ValueError("A verified GitHub Project v2 node ID is required")
    cursor = None
    items: list[dict] = []
    fields = []
    title = ""
    for page in range(5):
        result = caller(token, PROJECT_QUERY, {"project": project_id, "cursor": cursor})
        project = result.get("node")
        if not isinstance(project, dict) or project.get("id") != project_id:
            raise ValueError("The configured Project could not be verified")
        title = project.get("title")
        if page == 0:
            fields = [field for field in (project.get("fields") or {}).get("nodes", [])
                      if isinstance(field, dict) and field.get("name") == FIELD_NAME]
        connection = project.get("items") or {}
        for item in connection.get("nodes") or []:
            if not isinstance(item, dict):
                continue
            content = item.get("content") or {}
            current = item.get("fieldValueByName") or {}
            items.append({
                "id": item.get("id"), "number": content.get("number"),
                "repository": (content.get("repository") or {}).get("nameWithOwner"),
                "current_status": current.get("name"),
                "archived": item.get("isArchived") is True,
            })
        page_info = connection.get("pageInfo") or {}
        if not page_info.get("hasNextPage"):
            return {"id": project_id, "title": title, "fields": fields, "items": items}
        new_cursor = page_info.get("endCursor")
        if not isinstance(new_cursor, str) or new_cursor == cursor:
            raise RuntimeError("Project pagination could not advance")
        cursor = new_cursor
    raise RuntimeError("Project has more than 500 items; refusing partial synchronization")


def plan_updates(project: dict, report: dict, repository: str) -> tuple[list[dict], list[int]]:
    """Compute a complete fail-closed update plan; never modify human fields."""
    if project.get("title") != PROJECT_TITLE:
        raise ValueError("The configured Project has an unexpected identity")
    fields = [f for f in project.get("fields", []) if f.get("name") == FIELD_NAME]
    if len(fields) != 1 or not isinstance(fields[0].get("id"), str):
        raise ValueError("Project needs exactly one 'Tracker status' single-select field")
    field = fields[0]
    options = {o.get("name"): o.get("id") for o in field.get("options", [])}
    if len(options) != len(field.get("options", [])):
        raise ValueError("Tracker status options must be unique")
    by_issue: dict[int, dict] = {}
    for item in project.get("items", []):
        number = item.get("number")
        if (item.get("repository") != repository or item.get("archived")
                or not isinstance(number, int)):
            continue
        if number in by_issue:
            raise ValueError("Project contains duplicate Issue cards; review before sync")
        by_issue[number] = item
    updates: list[dict] = []
    missing: list[int] = []
    for task in report.get("tasks", []):
        number, status = task.get("issue_number"), task.get("status")
        if not isinstance(number, int) or not isinstance(status, str):
            raise ValueError("Invalid task record; refusing Project synchronization")
        option_id = options.get(status)
        if not isinstance(option_id, str):
            raise ValueError(f"Project is missing a Tracker status option for {status}")
        item = by_issue.get(number)
        if item is None:
            missing.append(number)
        elif item.get("current_status") != status:
            updates.append({"issue_number": number, "item_id": item["id"],
                            "field_id": field["id"], "option_id": option_id})
    return updates, missing


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", default=os.environ.get("GITHUB_REPOSITORY"))
    parser.add_argument("--project-id", default=os.environ.get("PESATRACK_TRACKER_PROJECT_ID"))
    parser.add_argument("--apply", action="store_true", help="Requires an opt-in repository variable")
    args = parser.parse_args(argv)
    token = os.environ.get("PESATRACK_TRACKER_PROJECTS_TOKEN", "")
    if not args.repository or not args.project_id or not token:
        parser.error("Repository, verified Project ID and separately configured Project token are required")
    if args.apply and os.environ.get("PESATRACK_TRACKER_PROJECT_SYNC_ENABLED") != "true":
        parser.error("Project mutation requires PESATRACK_TRACKER_PROJECT_SYNC_ENABLED=true")
    try:
        report = reconcile(fetch_snapshot(os.environ.get("GITHUB_TOKEN", ""), args.repository))
        if report["incomplete"]:
            raise RuntimeError("Tracker evidence is incomplete; refusing Project updates")
        project = load_project(token, args.project_id)
        changes, missing = plan_updates(project, report, args.repository)
        print(f"Project status mirror: {len(changes)} updates planned; "
              f"{len(missing)} Issues awaiting Project auto-add.")
        if not args.apply:
            for change in changes:
                print(f"Preview PT-{change['issue_number']}: Tracker status field would change")
        if args.apply:
            for change in changes:
                graphql(token, UPDATE_QUERY, {
                    "project": args.project_id, "item": change["item_id"],
                    "field": change["field_id"], "option": change["option_id"],
                })
            print(f"Applied {len(changes)} Project tracker-field updates.")
        return 0
    except (RuntimeError, ValueError) as exc:
        print(f"Project status mirror stopped: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
