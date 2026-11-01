"""Tracker summary must disclose incomplete evidence, never claim all work is done."""

import unittest

from tools.tracker.cli import report_markdown
from tools.tracker.reconcile import reconcile


class TrackerCliTest(unittest.TestCase):
    def test_unlabeled_tracker_shows_setup_gap_not_zero_work(self):
        report = report_markdown(reconcile({"checked_at": "2026-10-31T10:00:00Z",
                                            "issues": [], "prs": [], "checks": []}))
        self.assertIn("No labeled tracker Issues", report)
        self.assertIn("does not mean", report)

    def test_bounded_snapshot_discloses_partial_pr_coverage(self):
        report = report_markdown(reconcile({"checked_at": "2026-10-31T10:00:00Z",
                                            "issues": [], "prs": [], "checks": [],
                                            "truncated": True}))
        self.assertIn("incomplete", report)

    def test_report_displays_unlinked_active_prs_separately_from_tasks(self):
        data = reconcile({"checked_at": "2026-10-31T10:00:00Z", "issues": [],
                          "prs": [{"number": 201, "state": "open", "body": "No task link",
                                   "files": [], "head_sha": "a" * 40}], "checks": []})
        report = report_markdown(data)
        self.assertIn("Untracked open PRs: 1", report)
        self.assertIn("#201", report)

    def test_progress_counts_are_issue_evidence_not_chat_presence(self):
        data = reconcile({"checked_at": "2026-10-31T10:00:00Z",
                          "issues": [{"number": 7, "body": "Chat owner: Unclaimed\nTask status: Untriaged"}],
                          "prs": [], "checks": []})
        report = report_markdown(data)
        self.assertIn("Untriaged: 1", report)
        self.assertIn("not live ChatGPT presence", report)

    def test_report_names_automatic_evidence_and_pending_repair_action(self):
        issue = {"number": 100, "title": "Invented task", "state": "open",
                 "body": "Chat owner: CHAT-DOCS\nDevice acceptance: not applicable\nPR: #201"}
        pull = {"number": 201, "state": "closed", "merged_at": "2026-11-01T09:00:00Z",
                "body": "Tracks #100\nChat owner: CHAT-DOCS", "head_sha": "a" * 40,
                "files": ["docs/tracker/note.md"],
                "file_changes": [{"filename": "docs/tracker/note.md", "status": "added"}]}
        checks = [{"head_sha": "a" * 40, "name": name,
                   "status": "completed", "conclusion": "success"}
                  for name in ("Android CI", "Security - PR Secrets")]
        verified = report_markdown(reconcile({"issues": [issue], "prs": [pull],
                                              "checks": checks}))
        self.assertIn("automatic-docs", verified)
        self.assertIn("none", verified)
        checks[1]["conclusion"] = "failure"
        blocked = report_markdown(reconcile({"issues": [issue], "prs": [pull],
                                             "checks": checks}))
        self.assertIn("collect-acceptance-evidence", blocked)


if __name__ == "__main__":
    unittest.main()
