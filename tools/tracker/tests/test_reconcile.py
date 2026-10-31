"""Read-only tracker policy: invented PRs, owners and CI results."""

import unittest

from tools.tracker.reconcile import reconcile


def issue(number=100, owner="CHAT-STATEMENTS", device="Not verified", state="open"):
    return {
        "number": number, "title": "Invented statements issue", "state": state,
        "body": f"Source: VC-01\nChat owner: {owner}\nDevice acceptance: {device}",
        "labels": ["pesatrack-tracker"],
    }


def pr(number, task=100, owner="CHAT-STATEMENTS", head="a" * 40,
       merged=False, files=None, base="b" * 40):
    return {
        "number": number, "state": "closed" if merged else "open",
        "merged_at": "2026-10-31T09:00:00Z" if merged else None,
        "merge_commit_sha": "c" * 40 if merged else None,
        "body": f"Tracks #{task}\nChat owner: {owner}",
        "head_sha": head, "base_sha": base,
        "files": files or ["app/src/Invented.kt"],
    }


def check(head="a" * 40, conclusion="success", status="completed"):
    return {"head_sha": head, "name": "Android CI", "status": status,
            "conclusion": conclusion}


class ReconcileTest(unittest.TestCase):
    def test_two_prs_for_same_task_are_preserved_without_overwriting(self):
        data = {"checked_at": "2026-10-31T10:00:00Z", "main_sha": "b" * 40,
                "issues": [issue()],
                "prs": [pr(201), pr(202, head="d" * 40)],
                "checks": [check(), check(head="d" * 40)]}
        task = reconcile(data)["tasks"][0]
        self.assertEqual([201, 202], [item["number"] for item in task["prs"]])
        self.assertEqual("CHAT-STATEMENTS", task["owner"])
        self.assertEqual("CI passed", task["status"])

    def test_missing_or_failed_ci_never_reports_a_pass(self):
        base = {"checked_at": "2026-10-31T10:00:00Z", "main_sha": "b" * 40,
                "issues": [issue()], "prs": [pr(201)]}
        self.assertEqual("unknown", reconcile({**base, "checks": []})["tasks"][0]["ci"])
        failed = reconcile({**base, "checks": [check(conclusion="failure")]})["tasks"][0]
        self.assertEqual("failed", failed["ci"])
        self.assertEqual("CI failed", failed["status"])
        outdated = reconcile({**base, "checks": [check(head="e" * 40)]})["tasks"][0]
        self.assertEqual("unknown", outdated["ci"])

    def test_merged_pr_without_device_acceptance_remains_open_for_verification(self):
        result = reconcile({"checked_at": "2026-10-31T10:00:00Z",
                            "main_sha": "c" * 40, "issues": [issue()],
                            "prs": [pr(201, merged=True)], "checks": [check()]})
        task = result["tasks"][0]
        self.assertTrue(task["merged"])
        self.assertEqual("Merged — needs acceptance", task["status"])
        self.assertEqual("not_verified", task["device_acceptance"])

    def test_conflicting_owner_claim_warns_without_reassigning(self):
        result = reconcile({"checked_at": "2026-10-31T10:00:00Z",
                            "issues": [issue()], "prs": [pr(201, owner="CHAT-PROFILE")],
                            "checks": [check()]})
        task = result["tasks"][0]
        self.assertEqual("CHAT-STATEMENTS", task["owner"])
        self.assertTrue(any("claim" in warning.lower() for warning in task["warnings"]))

    def test_overlapping_open_pr_paths_and_stale_base_are_non_destructive_warnings(self):
        result = reconcile({"checked_at": "2026-10-31T10:00:00Z",
                            "main_sha": "f" * 40,
                            "issues": [issue(), issue(101, "CHAT-PROFILE")],
                            "prs": [pr(201), pr(202, task=101, owner="CHAT-PROFILE")],
                            "checks": [check()]})
        self.assertTrue(any("app/src/Invented.kt" in warning for warning in result["warnings"]))
        self.assertTrue(any("base" in warning.lower() for warning in result["tasks"][0]["warnings"]))
        self.assertEqual(2, len(result["tasks"]))

    def test_unclaimed_issue_and_invalid_pr_metadata_never_guess_ownership(self):
        result = reconcile({"checked_at": "2026-10-31T10:00:00Z",
                            "issues": [issue(owner="Unclaimed")],
                            "prs": [pr(201, owner="CHAT-PROFILE\nChat owner: CHAT-UI")],
                            "checks": []})
        task = result["tasks"][0]
        self.assertEqual("Unclaimed", task["owner"])
        self.assertEqual("unknown", task["ci"])

    def test_historical_merged_pr_linked_from_issue_body_without_rewriting_old_pr(self):
        historic = pr(478, merged=True)
        historic["body"] = "Historical proposal; no tracker reference"
        task_issue = issue()
        task_issue["body"] += "\nPR: #478\n"
        result = reconcile({"checked_at": "2026-10-31T10:00:00Z",
                            "issues": [task_issue], "prs": [historic],
                            "checks": [check()]})
        task = result["tasks"][0]
        self.assertEqual([478], [p["number"] for p in task["prs"]])
        self.assertEqual("Merged — needs acceptance", task["status"])

    def test_one_pr_named_by_two_issues_warns_without_assigning_owner(self):
        first = issue()
        second = issue(number=101, owner="CHAT-PROFILE")
        first["body"] += "\nPR: #478"
        second["body"] += "\nPR: #478"
        historic = pr(478, merged=True)
        historic["body"] = "Historical change"
        result = reconcile({"checked_at": "2026-10-31T10:00:00Z",
                            "issues": [first, second], "prs": [historic],
                            "checks": []})
        self.assertTrue(any("two" in message or "multiple" in message
                            for message in result["warnings"]))
        self.assertEqual("CHAT-STATEMENTS", result["tasks"][0]["owner"])
        self.assertEqual("CHAT-PROFILE", result["tasks"][1]["owner"])

    def test_newer_run_or_rerun_overrides_old_success_or_failure(self):
        base = {"checked_at": "2026-10-31T10:00:00Z", "issues": [issue()],
                "prs": [pr(201)]}
        old_fail = {**check(conclusion="failure"), "run_id": 10, "run_attempt": 1}
        new_pass = {**check(), "run_id": 20, "run_attempt": 1}
        task = reconcile({**base, "checks": [new_pass, old_fail]})["tasks"][0]
        self.assertEqual("passed", task["ci"])
        pending = {**check(conclusion=None, status="in_progress"),
                   "run_id": 20, "run_attempt": 2}
        task = reconcile({**base, "checks": [old_fail, new_pass, pending]})["tasks"][0]
        self.assertEqual("pending", task["ci"])


if __name__ == "__main__":
    unittest.main()
