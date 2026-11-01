"""Fail-closed, bounded PR intake tests; no GitHub write during tests."""

import unittest
from unittest.mock import patch

from tools.tracker.pr_inbox import (ADD_PR, CLEAR_STATUS, OWNER_QUERY, PROJECT_QUERY,
                                    SET_STATUS, apply_pr_inbox, fetch_open_prs,
                                    load_pr_project, main, plan_pr_inbox)

REPO = "jameswaweru377-lab/PesaTrack"


def project(items=None):
    return {"id": "PVT_test", "title": "PesaTrack Development Tracker",
            "fields": [{"id": "PVTSSF_test", "name": "Tracker status", "options": [
                {"id": "pr-open", "name": "PR open"},
                {"id": "merged", "name": "Merged — needs acceptance"}]}],
            "items": list(items or [])}


def item(number=10, state="OPEN", status="PR open"):
    return {"id": "PVTI_test", "number": number, "node_id": f"PR_test_{number}",
            "repository": REPO, "state": state, "status": status}


def pull(number=10):
    return {"number": number, "node_id": f"PR_test_{number}"}


class PrInboxTest(unittest.TestCase):
    def test_open_pr_inventory_fetches_all_pages_and_preserves_drafts(self):
        calls = []
        def read(url, headers, timeout):
            calls.append(url)
            return [{"number": i + 1, "node_id": f"PR_test_{i+1}", "state": "open",
                     "draft": True} for i in range(100)] if url.endswith("page=1") else [
                         {"number": 101, "node_id": "PR_test_101", "state": "open"}]
        result = fetch_open_prs("fake", REPO, opener=read)
        self.assertEqual(101, len(result))
        self.assertEqual(2, len(calls))
        self.assertIn("page=2", calls[-1])

    def test_api_unknown_shape_and_duplicate_or_missing_ids_fail_before_writes(self):
        for rows in ("not a list", [{"number": 2, "state": "open"}],
                     [{"number": 2, "node_id": "BAD", "state": "open"}],
                     [{"number": 2, "node_id": "PR_a", "state": "open"}] * 2):
            with self.subTest(rows=rows), self.assertRaises(RuntimeError):
                fetch_open_prs("fake", REPO, opener=lambda *args: rows)
        with self.assertRaises(ValueError):
            fetch_open_prs("fake", "../other", opener=lambda *args: [])
        with self.assertRaises(ValueError):
            fetch_open_prs("", REPO, opener=lambda *args: [])

    def test_plan_adds_only_missing_open_prs_and_preserves_existing(self):
        plan = plan_pr_inbox([pull(10), pull(11)], project([item()]), REPO)
        self.assertEqual([pull(11)], plan["adds"])
        self.assertEqual([], plan["sets"])
        self.assertEqual([], plan["clears"])

    def test_existing_pr_without_bot_status_is_backfilled(self):
        plan = plan_pr_inbox([pull()], project([item(status=None)]), REPO)
        self.assertEqual([], plan["adds"])
        self.assertEqual([("PVTI_test", "pr-open")], plan["sets"])

    def test_merges_need_acceptance_and_closed_without_merge_is_unset(self):
        merged = item(state="MERGED")
        closed = {**item(11, state="CLOSED"), "id": "PVTI_11"}
        plan = plan_pr_inbox([], project([merged, closed]), REPO)
        self.assertEqual([("PVTI_test", "merged")], plan["sets"])
        self.assertEqual(["PVTI_11"], plan["clears"])
        self.assertEqual([], plan["adds"])

    def test_archived_existing_pr_is_not_readded_or_reactivated(self):
        # GitHub addProjectV2ItemById returns an existing archived item ID.
        # Re-adding and setting PR open would overwrite intentional Paused work.
        archived = {**item(state="OPEN", status="Paused"), "archived": True}
        for status in ("Paused", None):
            with self.subTest(status=status):
                archived["status"] = status
                plan = plan_pr_inbox([pull()], project([archived]), REPO)
                self.assertEqual(([], [], []), (plan["adds"], plan["sets"], plan["clears"]))
        archived["state"] = "MERGED"
        archived["status"] = "PR open"
        plan = plan_pr_inbox([], project([archived]), REPO)
        self.assertEqual(([], [], []), (plan["adds"], plan["sets"], plan["clears"]))

    def test_merged_card_without_status_recovers_from_interrupted_first_write(self):
        # addProjectV2ItemById can succeed before the status write is cancelled.
        # If the PR merges meanwhile, a later sync must still repair its card.
        merged = item(state="MERGED", status=None)
        plan = plan_pr_inbox([], project([merged]), REPO)
        self.assertEqual([], plan["adds"])
        self.assertEqual([("PVTI_test", "merged")], plan["sets"])
        self.assertEqual([], plan["clears"])

    def test_never_invents_owners_or_modifies_existing_task_issue_cards(self):
        issue = {"id": "PVTI_issue", "number": 11, "repository": REPO,
                 "status": "Claimed", "node_id": "I_test", "state": "OPEN"}
        actual = project([issue])  # Normal project loader excludes Issue items.
        actual["items"] = []
        plan = plan_pr_inbox([pull(11)], actual, REPO)
        self.assertEqual([pull(11)], plan["adds"])
        self.assertNotIn("Chat owner", repr(plan))

    def test_fails_closed_for_duplicate_or_wrong_project_status_config(self):
        duplicate = project([item(), {**item(), "id": "PVTI_other"}])
        with self.assertRaisesRegex(ValueError, "duplicate"):
            plan_pr_inbox([pull()], duplicate, REPO)
        bad = project()
        bad["fields"][0]["options"] = [{"name": "PR open", "id": "pr"}]
        with self.assertRaisesRegex(ValueError, "options"):
            plan_pr_inbox([pull()], bad, REPO)
        wrong = project([item()])
        wrong["items"][0]["node_id"] = "PR_wrong"
        with self.assertRaisesRegex(ValueError, "identity"):
            plan_pr_inbox([pull()], wrong, REPO)

    def test_project_query_checks_identity_type_and_pagination(self):
        calls = []
        def read(token, query, variables):
            calls.append(variables["cursor"])
            self.assertIn("on PullRequest", query)
            number = 10 if variables["cursor"] is None else 11
            return {"node": {"id": "PVT_test", "title": "PesaTrack Development Tracker",
                    "fields": {"nodes": project()["fields"]},
                    "items": {"nodes": [{"id": f"PVTI_{number}", "isArchived": False,
                        "content": {"__typename": "PullRequest", "id": f"PR_test_{number}",
                            "number": number, "state": "OPEN", "repository": {
                                "nameWithOwner": REPO}},
                        "fieldValueByName": {"name": "PR open"}},
                        {"id": "PVTI_issue", "content": {"__typename": "Issue", "number": 11}}],
                        "pageInfo": {"hasNextPage": variables["cursor"] is None,
                                     "endCursor": "next"}}}}
        found = load_pr_project("fake", "PVT_test", REPO, caller=read)
        self.assertEqual([None, "next"], calls)
        self.assertEqual([10, 11], [x["number"] for x in found["items"]])

    def test_private_project_owner_fallback_rejects_wrong_id(self):
        def read(token, query, variables):
            if "project" in variables:
                raise RuntimeError("GitHub Project query was rejected (NOT_FOUND)")
            return {"user": {"projectV2": {"id": "PVT_wrong", "title":
                        "PesaTrack Development Tracker", "fields": {}, "items": {}}}}
        with self.assertRaisesRegex(ValueError, "identity"):
            load_pr_project("fake", "PVT_test", REPO, caller=read)
        self.assertIn("user(login: $owner)", OWNER_QUERY)

    def test_mutations_target_only_added_pr_item_and_bot_field(self):
        calls = []
        def write(token, query, variables):
            calls.append((query, variables))
            if "addProjectV2ItemById" in query:
                return {"addProjectV2ItemById": {"item": {"id": "PVTI_added"}}}
            return {"updateProjectV2ItemFieldValue": {"projectV2Item": {"id": "PVTI_added"}}}
        plan = plan_pr_inbox([pull(12)], project(), REPO)
        apply_pr_inbox("fake", "PVT_test", plan, caller=write)
        self.assertEqual(2, len(calls))
        self.assertIn("addProjectV2ItemById", calls[0][0])
        self.assertEqual("PR_test_12", calls[0][1]["pr"])
        self.assertEqual("pr-open", calls[1][1]["option"])
        self.assertNotIn("Chat owner", str(calls))

    def test_main_requires_separate_existing_project_optin_before_apply(self):
        with patch.dict("os.environ", {"GITHUB_TOKEN": "fake", "PESATRACK_TRACKER_PROJECTS_TOKEN": "fake",
                                    "PESATRACK_TRACKER_PROJECT_SYNC_ENABLED": "false"}):
            with self.assertRaises(SystemExit) as exc:
                main(["--repository", REPO, "--project-id", "PVT_test", "--apply"])
        self.assertEqual(2, exc.exception.code)


if __name__ == "__main__":
    unittest.main()
