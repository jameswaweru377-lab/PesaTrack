"""Owner-approved old UI proposal retirement is exact and bounded."""

import unittest

from tools.tracker.retire_aged_ui_branches import candidates, plan
from tools.tracker.tests.test_branch_cleanup import MAIN_SHA, branch, inventory, pull


class AgedUiRetirementTests(unittest.TestCase):
    def test_exact_two_old_ui_candidates(self):
        rows = candidates()
        self.assertEqual({"codex/more-screen-refresh-20261024",
                          "codex/statements-overview-dashboard-20261025"},
                         set(rows))
        self.assertTrue(all(len(sha) == 40 for sha in rows.values()))

    def test_moved_protected_open_pr_excluded(self):
        rows = candidates()
        first, second = list(rows)
        snap = inventory([branch("main", MAIN_SHA), branch(first, "a" * 40),
                          branch(second, rows[second], True)])
        result = plan(snap)
        self.assertEqual([], result["eligible"])
        self.assertEqual({"moved", "protected"},
                         {item["reason"] for item in result["skipped"]})
        snap = inventory([branch("main", MAIN_SHA), branch(first, rows[first])],
                         pulls=[pull(99, first, rows[first], state="open", merged_at=None)])
        self.assertEqual([], plan(snap)["eligible"])


if __name__ == "__main__":
    unittest.main()
