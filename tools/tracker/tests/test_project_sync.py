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


def graphql_project():
    return {
        "id": "PVT_invented", "title": "PesaTrack Development Tracker",
        "fields": {"nodes": [{"id": "PVTSSF_invented", "name": "Tracker status",
                              "options": [{"id": "opt_ci", "name": "CI passed"}]}]},
        "items": {"nodes": [], "pageInfo": {"hasNextPage": False, "endCursor": None}},
    }


class ProjectSyncTest(unittest.TestCase):
    def test_not_found_node_falls_back_to_verified_private_owner_project(self):
        calls = []

        def fake(token, query, variables):
            calls.append(variables)
            if "project" in variables:
                raise RuntimeError("GitHub Project query was rejected (NOT_FOUND)")
            self.assertEqual("jameswaweru377-lab", variables["owner"])
            self.assertEqual(1, variables["number"])
            return {"user": {"projectV2": graphql_project()}}

        loaded = load_project(
            "invented-token", "PVT_invented", caller=fake,
            owner="jameswaweru377-lab", number=1,
        )
        self.assertEqual("PVT_invented", loaded["id"])
        self.assertEqual(2, len(calls))

    def test_owner_lookup_rejects_different_project_id(self):
        def fake(token, query, variables):
            if "project" in variables:
                raise RuntimeError("GitHub Project query was rejected (NOT_FOUND)")
            candidate = dict(graphql_project())
            candidate["id"] = "PVT_other"
            return {"user": {"projectV2": candidate}}

        with self.assertRaises(ValueError) as mismatch:
            load_project(
                "invented-token", "PVT_invented", caller=fake,
                owner="jameswaweru377-lab", number=1,
            )
        self.assertIn("Project ID", str(mismatch.exception))
        self.assertIn("PVT_other", str(mismatch.exception))

    def test_owner_lookup_never_logs_malformed_project_identifiers(self):
        def fake(token, query, variables):
            if "project" in variables:
                raise RuntimeError("GitHub Project query was rejected (NOT_FOUND)")
            candidate = dict(graphql_project())
            candidate["id"] = "PVT_other\nPRIVATE_DATA=do-not-log"
            return {"user": {"projectV2": candidate}}

        with self.assertRaises(ValueError) as mismatch:
            load_project("invented-token", "PVT_invented", caller=fake,
                         owner="jameswaweru377-lab", number=1)
        self.assertNotIn("PRIVATE_DATA", str(mismatch.exception))

    def test_owner_lookup_reports_bounded_base64_project_ids(self):
        for expected_id in ("PVT_safe-url_id", "PVT_safe+standard/id="):
            with self.subTest(expected_id=expected_id):
                def fake(token, query, variables):
                    if "project" in variables:
                        raise RuntimeError("GitHub Project query was rejected (NOT_FOUND)")
                    candidate = dict(graphql_project())
                    candidate["id"] = expected_id
                    return {"user": {"projectV2": candidate}}

                with self.assertRaises(ValueError) as mismatch:
                    load_project("invented-token", "PVT_invented", caller=fake,
                                 owner="jameswaweru377-lab", number=1)
                self.assertIn(expected_id, str(mismatch.exception))

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
        updates, missing, skipped = plan_updates(project(), report(), "jameswaweru377-lab/PesaTrack")
        self.assertEqual([], missing)
        self.assertEqual([], skipped)
        self.assertEqual([{"issue_number": 123, "item_id": "PVTI_invented",
                           "field_id": "PVTSSF_invented", "option_id": "opt_ci"}], updates)
        self.assertEqual(([], [], []), plan_updates(project("CI passed"), report(),
                                                   "jameswaweru377-lab/PesaTrack"))

    def test_capped_history_skips_only_unsafe_status_downgrades(self):
        existing = project("Merged — needs acceptance")
        existing["items"].append({**existing["items"][0],
                                  "id": "PVTI_124", "number": 124,
                                  "current_status": "Unclaimed"})
        observed = report("Claimed")
        observed["historical_window_limited"] = True
        observed["tasks"].append({"issue_number": 124, "status": "CI passed"})
        existing["fields"][0]["options"].append({"id": "opt_claimed", "name": "Claimed"})

        updates, missing, skipped = plan_updates(
            existing, observed, "jameswaweru377-lab/PesaTrack")

        self.assertEqual([], missing)
        self.assertEqual([123], skipped)
        self.assertEqual([124], [entry["issue_number"] for entry in updates])
        self.assertEqual("opt_ci", updates[0]["option_id"])

    def test_capped_history_keeps_ci_changes_and_explicit_human_pauses(self):
        existing = project("CI passed")
        existing["fields"][0]["options"].extend([
            {"id": "opt_failed", "name": "CI failed"},
            {"id": "opt_paused", "name": "Paused"},
        ])
        for status in ("CI failed", "Paused"):
            with self.subTest(status=status):
                observed = report(status)
                observed["historical_window_limited"] = True
                updates, missing, skipped = plan_updates(
                    existing, observed, "jameswaweru377-lab/PesaTrack")
                self.assertEqual(([], []), (missing, skipped))
                self.assertEqual([123], [entry["issue_number"] for entry in updates])

    def test_capped_history_allows_lost_verification_when_merged_pr_is_known(self):
        existing = project("Verified")
        for ci, device in (("failed", "verified"), ("passed", "not_verified")):
            with self.subTest(ci=ci, device=device):
                observed = report("Merged — needs acceptance")
                observed["historical_window_limited"] = True
                observed["tasks"][0]["merged"] = True
                observed["tasks"][0]["device_acceptance"] = device
                observed["tasks"][0]["prs"] = [{"number": 478, "state": "merged",
                                                "ci": ci}]

                updates, missing, skipped = plan_updates(
                    existing, observed, "jameswaweru377-lab/PesaTrack")

                self.assertEqual(([], []), (missing, skipped))
                self.assertEqual([123], [entry["issue_number"] for entry in updates])

    def test_capped_history_does_not_invent_merger_to_override_verified(self):
        existing = project("Verified")
        observed = report("Merged — needs acceptance")
        observed["historical_window_limited"] = True
        observed["tasks"][0]["merged"] = False
        observed["tasks"][0]["prs"] = []

        updates, missing, skipped = plan_updates(
            existing, observed, "jameswaweru377-lab/PesaTrack")

        self.assertEqual(([], []), (updates, missing))
        self.assertEqual([123], skipped)

    def test_uncapped_history_allows_explicit_task_reopening(self):
        existing = project("Merged — needs acceptance")
        existing["fields"][0]["options"].append({"id": "opt_claimed", "name": "Claimed"})
        updates, missing, skipped = plan_updates(
            existing, report("Claimed"), "jameswaweru377-lab/PesaTrack")
        self.assertEqual(([], []), (missing, skipped))
        self.assertEqual([123], [entry["issue_number"] for entry in updates])

    def test_unknown_status_or_missing_field_fails_before_any_remote_write(self):
        with self.assertRaisesRegex(ValueError, "option"):
            plan_updates(project(), report("Verified"), "jameswaweru377-lab/PesaTrack")
        missing_field = project()
        missing_field["fields"] = []
        with self.assertRaisesRegex(ValueError, "Tracker status"):
            plan_updates(missing_field, report(), "jameswaweru377-lab/PesaTrack")

    def test_unregistered_issue_requires_project_auto_add_not_create(self):
        updates, missing, skipped = plan_updates(project(include_item=False), report(),
                                                 "jameswaweru377-lab/PesaTrack")
        self.assertEqual([], updates)
        self.assertEqual([123], missing)
        self.assertEqual([], skipped)

    def test_foreign_issue_and_archived_item_are_never_modified(self):
        data = project()
        data["items"][0]["repository"] = "another-owner/another-repository"
        self.assertEqual(([], [123], []), plan_updates(data, report(),
                                                       "jameswaweru377-lab/PesaTrack"))
        data = project()
        data["items"][0]["archived"] = True
        self.assertEqual(([], [123], []), plan_updates(data, report(),
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
