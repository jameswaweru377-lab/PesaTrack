"""Project v2 updates are optional, bounded, and never an ownership authority."""

import json
import unittest
from io import BytesIO
from unittest.mock import patch

from tools.tracker.project_sync import graphql, load_project, main, plan_updates


def project(status="Unclaimed", *, include_item=True):
    return {
        "id": "PVT_invented", "title": "PesaTrack Development Tracker",
        "fields": [{"id": "PVTSSF_invented", "name": "Tracker status",
                    "options": [{"id": "opt_unclaimed", "name": "Unclaimed"},
                                {"id": "opt_ci", "name": "CI passed"},
                                {"id": "opt_merged", "name": "Merged — needs acceptance"}]}],
        "items": ([{"id": "PVTI_invented", "number": 123,
                    "repository": "jameswaweru377-lab/PesaTrack",
                    "current_status": status, "archived": False}]
                  if include_item else []),
    }


def report(status="CI passed"):
    return {"tasks": [{"issue_number": 123, "status": status,
                       "owner": "CHAT-STATEMENTS", "device_acceptance": "not_verified"}]}


class ProjectSyncTest(unittest.TestCase):
    def test_graphql_rejection_reports_safe_error_type_without_remote_message(self):
        response = BytesIO(json.dumps({
            "errors": [{"type": "INSUFFICIENT_SCOPES",
                        "message": "private GitHub response that must not be logged"}],
            "data": {},
        }).encode("utf-8"))
        with patch("tools.tracker.project_sync.request.urlopen", return_value=response):
            with self.assertRaises(RuntimeError) as failure:
                graphql("invented-token", "query { viewer { login } }", {})
        self.assertIn("INSUFFICIENT_SCOPES", str(failure.exception))
        self.assertNotIn("private GitHub response", str(failure.exception))

    def test_plans_only_changed_bot_owned_status_field(self):
        updates, missing = plan_updates(project(), report(), "jameswaweru377-lab/PesaTrack")
        self.assertEqual([], missing)
        self.assertEqual([{"issue_number": 123, "item_id": "PVTI_invented",
                           "field_id": "PVTSSF_invented", "option_id": "opt_ci"}], updates)
        self.assertEqual(([], []), plan_updates(project("CI passed"), report(),
                                               "jameswaweru377-lab/PesaTrack"))

    def test_unknown_status_or_missing_field_fails_before_any_remote_write(self):
        with self.assertRaisesRegex(ValueError, "option"):
            plan_updates(project(), report("Verified"), "jameswaweru377-lab/PesaTrack")
        missing_field = project()
        missing_field["fields"] = []
        with self.assertRaisesRegex(ValueError, "Tracker status"):
            plan_updates(missing_field, report(), "jameswaweru377-lab/PesaTrack")

    def test_unregistered_issue_requires_project_auto_add_not_create(self):
        updates, missing = plan_updates(project(include_item=False), report(),
                                        "jameswaweru377-lab/PesaTrack")
        self.assertEqual([], updates)
        self.assertEqual([123], missing)

    def test_foreign_issue_and_archived_item_are_never_modified(self):
        data = project()
        data["items"][0]["repository"] = "another-owner/another-repository"
        self.assertEqual(([], [123]), plan_updates(data, report(),
                                                   "jameswaweru377-lab/PesaTrack"))
        data = project()
        data["items"][0]["archived"] = True
        self.assertEqual(([], [123]), plan_updates(data, report(),
                                                   "jameswaweru377-lab/PesaTrack"))

    def test_duplicate_issue_items_and_wrong_project_identity_abort(self):
        data = project()
        data["items"].append({**data["items"][0], "id": "PVTI_duplicate"})
        with self.assertRaisesRegex(ValueError, "duplicate"):
            plan_updates(data, report(), "jameswaweru377-lab/PesaTrack")
        data = project()
        data["title"] = "Another dashboard"
        with self.assertRaisesRegex(ValueError, "Project"):
            plan_updates(data, report(), "jameswaweru377-lab/PesaTrack")

    def test_bounded_graphql_pagination_preserves_only_issue_identity_and_bot_field(self):
        cursors = []

        def fake(token, query, variables):
            self.assertEqual("invented-token", token)
            cursors.append(variables["cursor"])
            number = 123 if variables["cursor"] is None else 124
            return {"node": {
                "id": "PVT_invented", "title": "PesaTrack Development Tracker",
                "fields": {"nodes": project()["fields"]},
                "items": {"nodes": [{"id": f"PVTI_{number}", "isArchived": False,
                                      "content": {"number": number, "repository": {
                                          "nameWithOwner": "jameswaweru377-lab/PesaTrack"}},
                                      "fieldValueByName": {"name": "Unclaimed"}}],
                          "pageInfo": {"hasNextPage": variables["cursor"] is None,
                                       "endCursor": "next" if variables["cursor"] is None else None}},
            }}

        loaded = load_project("invented-token", "PVT_invented", caller=fake)
        self.assertEqual([None, "next"], cursors)
        self.assertEqual([123, 124], [item["number"] for item in loaded["items"]])

    def test_apply_aborts_without_explicit_repository_opt_in(self):
        with patch.dict("os.environ", {
            "PESATRACK_TRACKER_PROJECTS_TOKEN": "invented-secret",
            "PESATRACK_TRACKER_PROJECT_SYNC_ENABLED": "false",
        }):
            with self.assertRaises(SystemExit) as failure:
                main(["--repository", "jameswaweru377-lab/PesaTrack",
                      "--project-id", "PVT_invented", "--apply"])
        self.assertEqual(2, failure.exception.code)


if __name__ == "__main__":
    unittest.main()
