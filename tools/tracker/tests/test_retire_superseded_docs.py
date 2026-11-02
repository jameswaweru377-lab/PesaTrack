"""Only the two owner-approved, older documentation proposal refs may retire."""

import unittest

from tools.tracker.retire_superseded_docs import approved_candidates, plan
from tools.tracker.tests.test_branch_cleanup import REPOSITORY, MAIN_SHA, branch, inventory, pull


class SupersededDocumentationTests(unittest.TestCase):
    def test_fixed_exact_tip_allowlist(self):
        rows = approved_candidates()
        self.assertEqual(2, len(rows))
        self.assertEqual({
            "design/backup-restore-phase1-20261030",
            "design/universal-statement-auto-import-20261031",
        }, {row["branch"] for row in rows})
        self.assertTrue(all(len(row["sha"]) == 40 for row in rows))
        self.assertTrue(all(row["doc"].startswith("docs/superpowers/") for row in rows))

    def test_moved_protected_and_open_pr_are_never_eligible(self):
        first, second = approved_candidates()
        snap = inventory([branch("main", MAIN_SHA),
                          branch(first["branch"], "a" * 40),
                          branch(second["branch"], second["sha"], True)])
        result = plan(snap)
        self.assertEqual([], result["candidates"])
        self.assertEqual({"moved", "protected"},
                         {row["reason"] for row in result["skipped"]})
        snap = inventory([branch("main", MAIN_SHA), branch(first["branch"], first["sha"])],
                         pulls=[pull(27, first["branch"], first["sha"], state="open", merged_at=None)])
        result = plan(snap)
        self.assertEqual([], result["candidates"])
        self.assertIn("open_pr", {row["reason"] for row in result["skipped"]})


if __name__ == "__main__":
    unittest.main()
