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


def docs_pr(number=201, head="a" * 40):
    result = pr(number, head=head, merged=True,
                files=["docs/tracker/2026-11-01-note.md"])
    result["file_changes"] = [{"filename": result["files"][0], "status": "added"}]
    return result


def docs_issue(number=100, pr_number=201):
    result = issue(number=number, device="not applicable")
    result["body"] += f"\nRelated PRs: #{pr_number}"
    return result


def complete_checks(head="a" * 40):
    return [check(head=head), {**check(head=head), "name": "Security - PR Secrets"}]


class ReconcileTest(unittest.TestCase):
    def test_docs_only_merged_issue_is_verified_without_manual_final_marker(self):
        result = reconcile({"checked_at": "2026-11-01T10:00:00Z", "main_sha": "c" * 40,
                            "issues": [docs_issue()], "prs": [docs_pr()],
                            "checks": complete_checks(), "historical_window_limited": True})
        task = result["tasks"][0]
        self.assertEqual("Verified", task["status"])
        self.assertEqual("automatic-docs", task["acceptance"]["mode"])
        self.assertEqual("none", task["acceptance"]["action"])

    def test_docs_auto_acceptance_fails_closed_for_missing_or_revoked_evidence(self):
        cases = ("missing-secrets", "later-secrets-failure", "changed-file", "unknown-file",
                 "device-required", "verification-withdrawn", "incomplete", "history-unlinked",
                 "open-newer-pr", "cross-issue-link", "owner-mismatch", "unclaimed", "untriaged",
                 "conflicting-status", "unknown-status", "conflicting-device")
        for case in cases:
            with self.subTest(case=case):
                selected_issue, selected_pr = docs_issue(), docs_pr()
                evidence = complete_checks()
                snapshot = {"issues": [selected_issue], "prs": [selected_pr],
                            "checks": evidence, "historical_window_limited": True}
                if case == "missing-secrets":
                    evidence.pop()
                elif case == "later-secrets-failure":
                    evidence[1].update(run_id=2, run_attempt=1)
                    evidence.append({**evidence[1], "run_id": 3,
                                     "conclusion": "failure"})
                elif case == "changed-file":
                    selected_pr["file_changes"][0]["status"] = "modified"
                elif case == "unknown-file":
                    selected_pr.pop("file_changes")
                elif case == "device-required":
                    selected_issue["body"] = selected_issue["body"].replace(
                        "Device acceptance: not applicable", "Device acceptance: not verified")
                elif case == "verification-withdrawn":
                    selected_issue["body"] += "\nFinal verification: not verified"
                elif case == "incomplete":
                    snapshot["truncated"] = True
                elif case == "history-unlinked":
                    selected_issue["body"] = selected_issue["body"].replace(
                        "Related PRs: #201", "")
                elif case == "open-newer-pr":
                    snapshot["prs"].append(pr(202))
                elif case == "cross-issue-link":
                    other = docs_issue(number=101)
                    snapshot["issues"].append(other)
                elif case == "owner-mismatch":
                    selected_pr["body"] = "Tracks #100\nChat owner: CHAT-OTHER"
                elif case == "unclaimed":
                    selected_issue["body"] = selected_issue["body"].replace(
                        "Chat owner: CHAT-STATEMENTS", "Chat owner: Unclaimed")
                elif case == "untriaged":
                    selected_issue["body"] += "\nTask status: Untriaged"
                elif case == "conflicting-status":
                    selected_issue["body"] += "\nTask status: Paused\nTask status: In progress"
                elif case == "unknown-status":
                    selected_issue["body"] += "\nTask status: something unexpected"
                elif case == "conflicting-device":
                    selected_issue["body"] += "\nDevice acceptance: not verified"
                task = reconcile(snapshot)["tasks"][0]
                self.assertNotEqual("Verified", task["status"])
                self.assertNotEqual("none", task["acceptance"]["action"])

    def test_exception_action_suggests_ci_repair_or_fresh_main_review(self):
        failing = reconcile({"issues": [issue()], "prs": [pr(201)],
                             "checks": [check(conclusion="failure")]})["tasks"][0]
        self.assertEqual("repair-ci", failing["acceptance"]["action"])
        stale = reconcile({"issues": [issue()], "prs": [pr(201)],
                           "main_sha": "e" * 40, "checks": [check()]})["tasks"][0]
        self.assertEqual("update-from-main", stale["acceptance"]["action"])
        unclaimed = reconcile({"issues": [issue(owner="Unclaimed")],
                               "prs": [], "checks": []})["tasks"][0]
        self.assertEqual("claim-or-triage-task", unclaimed["acceptance"]["action"])

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

    def test_explicit_untriaged_and_paused_states_are_not_silently_claimed(self):
        first = issue(owner="Unclaimed")
        first["body"] += "\nTask status: Untriaged"
        second = issue(number=101, owner="CHAT-UI")
        second["body"] += "\nTask status: Paused"
        data = reconcile({"checked_at": "2026-10-31T10:00:00Z", "issues": [first, second]})
        self.assertEqual(["Untriaged", "Paused"], [t["status"] for t in data["tasks"]])

    def test_human_pause_blocks_an_automatic_ci_pass_status(self):
        frozen = issue()
        frozen["body"] += "\nTask status: Paused"
        data = reconcile({"checked_at": "2026-10-31T10:00:00Z", "issues": [frozen],
                          "prs": [pr(201)], "checks": [check()]})
        self.assertEqual("Paused", data["tasks"][0]["status"])

    def test_case_insensitive_pause_prevents_an_open_pr_from_overriding_owner(self):
        for spelling in ("paused", "PAUSED", "PaUsEd"):
            with self.subTest(spelling=spelling):
                frozen = issue()
                frozen["body"] += f"\nTask status: {spelling}"
                data = reconcile({"checked_at": "2026-10-31T10:00:00Z",
                                  "issues": [frozen], "prs": [pr(201)],
                                  "checks": [check()]})
                self.assertEqual("Paused", data["tasks"][0]["status"])

    def test_equivalent_human_state_lines_are_not_treated_as_conflicts(self):
        frozen = issue()
        frozen["body"] += "\nTask status: paused\nTask status: PAUSED"
        data = reconcile({"checked_at": "2026-10-31T10:00:00Z",
                          "issues": [frozen], "prs": [pr(201)],
                          "checks": [check()]})
        self.assertEqual("Paused", data["tasks"][0]["status"])

    def test_distinct_human_state_lines_remain_ambiguous(self):
        frozen = issue()
        frozen["body"] += "\nTask status: Paused\nTask status: Blocked"
        data = reconcile({"checked_at": "2026-10-31T10:00:00Z",
                          "issues": [frozen], "prs": [pr(201)],
                          "checks": [check()]})
        self.assertEqual("CI passed", data["tasks"][0]["status"])

    def test_open_unlinked_prs_are_visible_without_inventing_task_owners(self):
        unlinked = pr(211)
        unlinked["body"] = "A legitimate independent change without a tracker reference"
        data = reconcile({"checked_at": "2026-10-31T10:00:00Z", "issues": [issue()],
                          "prs": [pr(201), unlinked], "checks": [check()]})
        self.assertEqual([211], data["untracked_open_prs"])
        self.assertEqual("CHAT-STATEMENTS", data["tasks"][0]["owner"])

    def test_auto_merge_label_is_request_only_not_proof_of_approval(self):
        proposed = pr(201)
        proposed["labels"] = ["pesatrack-safe-automerge"]
        data = reconcile({"checked_at": "2026-10-31T10:00:00Z",
                          "issues": [issue()], "prs": [proposed], "checks": [check()]})
        self.assertEqual("requested — gates unverified", data["tasks"][0]["prs"][0]["auto_merge"])


if __name__ == "__main__":
    unittest.main()
