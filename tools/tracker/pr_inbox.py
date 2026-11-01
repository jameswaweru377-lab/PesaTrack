"""Discover GitHub-visible PR work in the existing Project without inventing task owners.

The canonical task remains a labeled Issue. Pull-request cards form a separate
intake surface, including PRs without Issue links. No Issues, PRs, or human
Project status/ownership fields are changed.
"""

from __future__ import annotations

import argparse
import os
import re
import sys
from urllib import parse

from .github_snapshot import REPOSITORY_PATTERN, _open_json
from .project_sync import FIELD_NAME, PROJECT_ID, PROJECT_TITLE, graphql

PR_NODE = re.compile(r"PR_[A-Za-z0-9_-]{1,128}\Z")
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
          content { __typename ... on PullRequest {
            id number state repository { nameWithOwner }
          } }
          fieldValueByName(name: "Tracker status") {
            ... on ProjectV2ItemFieldSingleSelectValue { name }
          }
        }
      }
    }
  }
}
"""
OWNER_QUERY = PROJECT_QUERY.replace(
    "node(id: $project) {\n    ... on ProjectV2 {",
    "user(login: $owner) {\n    projectV2(number: $number) {").replace(
    "query($project: ID!, $cursor: String)",
    "query($owner: String!, $number: Int!, $cursor: String)")
ADD_PR = """
mutation($project: ID!, $pr: ID!) {
  addProjectV2ItemById(input: {projectId: $project, contentId: $pr}) {
    item { id }
  }
}
"""
SET_STATUS = """
mutation($project: ID!, $item: ID!, $field: ID!, $option: String!) {
  updateProjectV2ItemFieldValue(input: {
    projectId: $project, itemId: $item, fieldId: $field,
    value: {singleSelectOptionId: $option}
  }) { projectV2Item { id } }
}
"""
CLEAR_STATUS = """
mutation($project: ID!, $item: ID!, $field: ID!) {
  clearProjectV2ItemFieldValue(input: {
    projectId: $project, itemId: $item, fieldId: $field
  }) { projectV2Item { id } }
}
"""


def fetch_open_prs(token: str, repository: str, *, opener=_open_json) -> list[dict]:
    """Bounded open-PR inventory. An incomplete page stops every write."""
    if (not REPOSITORY_PATTERN.fullmatch(repository)
            or any(part in {".", ".."} for part in repository.split("/"))):
        raise ValueError("Invalid GitHub repository")
    if not token:
        raise ValueError("A GitHub repository read token is required")
    headers = {"Authorization": f"Bearer {token}",
               "Accept": "application/vnd.github+json",
               "User-Agent": "PesaTrack-PR-Intake"}
    found = []
    for page in range(1, 6):
        url = (f"https://api.github.com/repos/{repository}/pulls?"
               + parse.urlencode({"state": "open", "per_page": 100, "page": page}))
        rows = opener(url, headers, 12)
        if not isinstance(rows, list) or len(rows) > 100:
            raise RuntimeError("Invalid or incomplete PR inventory")
        for row in rows:
            number, node_id = row.get("number"), row.get("node_id")
            if (not isinstance(number, int) or number <= 0
                    or not isinstance(node_id, str) or not PR_NODE.fullmatch(node_id)
                    or row.get("state") != "open"):
                raise RuntimeError("PR inventory lacks a verifiable identity")
            found.append({"number": number, "node_id": node_id})
        if len(rows) < 100:
            break
    else:
        raise RuntimeError("More than 500 open PRs; refusing partial discovery")
    if len({row["number"] for row in found}) != len(found):
        raise RuntimeError("Duplicate PRs in inventory")
    return found


def load_pr_project(token: str, project_id: str, repository: str, *, caller=graphql) -> dict:
    if not PROJECT_ID.fullmatch(project_id):
        raise ValueError("Configured Project ID is invalid")
    owner = repository.split("/")[0]
    items, fields, cursor = [], [], None
    for page in range(5):
        try:
            response = caller(token, PROJECT_QUERY, {"project": project_id, "cursor": cursor})
            project = response.get("node")
        except RuntimeError as exc:
            if str(exc) != "GitHub Project query was rejected (NOT_FOUND)":
                raise
            response = caller(token, OWNER_QUERY, {"owner": owner, "number": 1,
                                                   "cursor": cursor})
            project = (response.get("user") or {}).get("projectV2")
        if (not isinstance(project, dict) or project.get("id") != project_id
                or project.get("title") != PROJECT_TITLE):
            raise ValueError("Configured Project identity could not be verified")
        if page == 0:
            fields = (project.get("fields") or {}).get("nodes") or []
        connection = project.get("items") or {}
        for row in connection.get("nodes") or []:
            content = row.get("content") or {}
            if content.get("__typename") != "PullRequest" or row.get("isArchived"):
                continue
            items.append({"id": row.get("id"), "number": content.get("number"),
                          "node_id": content.get("id"), "state": content.get("state"),
                          "repository": (content.get("repository") or {}).get("nameWithOwner"),
                          "status": (row.get("fieldValueByName") or {}).get("name")})
        page_info = connection.get("pageInfo") or {}
        if not page_info.get("hasNextPage"):
            return {"id": project_id, "title": project["title"],
                    "fields": fields, "items": items}
        next_cursor = page_info.get("endCursor")
        if not isinstance(next_cursor, str) or next_cursor == cursor:
            raise RuntimeError("Project pagination is incomplete")
        cursor = next_cursor
    raise RuntimeError("Project has more than 500 items; refusing partial discovery")


def plan_pr_inbox(prs: list[dict], project: dict, repository: str) -> dict:
    """No ownership guesses; only the existing bot-owned status field is used."""
    fields = [f for f in project["fields"] if f.get("name") == FIELD_NAME]
    if len(fields) != 1 or not isinstance(fields[0].get("id"), str):
        raise ValueError("Exactly one Tracker status single-select field is required")
    options = fields[0].get("options") or []
    values = {o.get("name"): o.get("id") for o in options}
    if (len(values) != len(options)
            or not all(isinstance(values.get(s), str)
                       for s in ("PR open", "Merged — needs acceptance"))):
        raise ValueError("Project status options are missing or ambiguous")
    existing = {}
    for item in project["items"]:
        if item["repository"] != repository:
            continue
        number = item["number"]
        if not isinstance(number, int) or not isinstance(item["id"], str):
            raise ValueError("Project has an invalid PR item")
        if number in existing:
            raise ValueError("Project contains duplicate PR cards")
        existing[number] = item
    adds, status_updates, clears = [], [], []
    open_numbers = {p["number"] for p in prs}
    for pr in prs:
        item = existing.get(pr["number"])
        if item is None:
            adds.append(pr)
        elif item["node_id"] != pr["node_id"]:
            raise ValueError("Project PR identity mismatch")
        elif item["status"] is None:
            status_updates.append((item["id"], values["PR open"]))
    for number, item in existing.items():
        if number in open_numbers:
            continue
        if item["state"] == "MERGED" and item["status"] in {None, "PR open"}:
            status_updates.append((item["id"], values["Merged — needs acceptance"]))
        elif item["state"] == "CLOSED" and item["status"] == "PR open":
            clears.append(item["id"])
    return {"adds": adds, "sets": status_updates, "clears": clears,
            "field": fields[0]["id"], "open_option": values["PR open"]}


def apply_pr_inbox(token: str, project_id: str, plan: dict, *, caller=graphql) -> None:
    for pr in plan["adds"]:
        data = caller(token, ADD_PR, {"project": project_id, "pr": pr["node_id"]})
        item_id = ((data.get("addProjectV2ItemById") or {}).get("item") or {}).get("id")
        if not isinstance(item_id, str) or not item_id:
            raise RuntimeError("Project did not confirm the PR addition")
        caller(token, SET_STATUS, {"project": project_id, "item": item_id,
                                   "field": plan["field"], "option": plan["open_option"]})
    for item_id, option in plan["sets"]:
        caller(token, SET_STATUS, {"project": project_id, "item": item_id,
                                   "field": plan["field"], "option": option})
    for item_id in plan["clears"]:
        caller(token, CLEAR_STATUS, {"project": project_id, "item": item_id,
                                     "field": plan["field"]})


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", default=os.environ.get("GITHUB_REPOSITORY"))
    parser.add_argument("--project-id", default=os.environ.get("PESATRACK_TRACKER_PROJECT_ID"))
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args(argv)
    if (not args.repository or not args.project_id
            or not os.environ.get("GITHUB_TOKEN")
            or not os.environ.get("PESATRACK_TRACKER_PROJECTS_TOKEN")):
        parser.error("Repository, Project ID and existing Project token are required")
    if args.apply and os.environ.get("PESATRACK_TRACKER_PROJECT_SYNC_ENABLED") != "true":
        parser.error("PR intake writes require enabled Project synchronization")
    try:
        prs = fetch_open_prs(os.environ["GITHUB_TOKEN"], args.repository)
        project = load_pr_project(os.environ["PESATRACK_TRACKER_PROJECTS_TOKEN"],
                                  args.project_id, args.repository)
        plan = plan_pr_inbox(prs, project, args.repository)
        print(f"PR intake: {len(prs)} open PRs; {len(plan['adds'])} additions, "
              f"{len(plan['sets'])} status updates, {len(plan['clears'])} cleared closed statuses.")
        if args.apply:
            apply_pr_inbox(os.environ["PESATRACK_TRACKER_PROJECTS_TOKEN"], args.project_id, plan)
            print("PR intake applied; canonical task Issues and chat owners unchanged.")
        return 0
    except (RuntimeError, ValueError) as exc:
        print(f"PR intake stopped: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
