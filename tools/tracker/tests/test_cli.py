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


if __name__ == "__main__":
    unittest.main()
