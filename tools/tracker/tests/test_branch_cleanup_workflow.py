"""Least-privilege and owner-gate contract for the monthly cleanup workflow."""

from pathlib import Path
import unittest

import yaml


ROOT = Path(__file__).resolve().parents[3]
WORKFLOW_PATH = ROOT / ".github/workflows/branch-cleanup.yml"


class BranchCleanupWorkflowTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.workflow = yaml.load(WORKFLOW_PATH.read_text(encoding="utf-8"),
                                 Loader=yaml.BaseLoader)

    def test_only_monthly_and_manual_dispatch_events_are_enabled(self):
        triggers = self.workflow["on"]
        self.assertEqual({"schedule", "workflow_dispatch"}, set(triggers))
        self.assertEqual(["17 2 1 * *"], [row["cron"] for row in triggers["schedule"]])
        self.assertEqual("false", triggers["workflow_dispatch"]["inputs"]["apply"]["default"])
        self.assertEqual("boolean", triggers["workflow_dispatch"]["inputs"]["apply"]["type"])

    def test_preview_is_read_only_and_runs_by_default(self):
        preview = self.workflow["jobs"]["preview"]
        self.assertEqual("read", preview["permissions"]["contents"])
        self.assertEqual("read", preview["permissions"]["pull-requests"])
        run = next(step["run"] for step in preview["steps"]
                   if step.get("id") == "preview")
        self.assertIn("tools/tracker/branch_cleanup.py", run)
        self.assertNotIn("--apply", run)
        self.assertIn("GITHUB_TOKEN", preview["env"])

    def test_apply_is_default_branch_owner_only_and_separately_scoped(self):
        apply = self.workflow["jobs"]["apply"]
        condition = apply["if"]
        self.assertIn("github.event.repository.default_branch", condition)
        self.assertIn("github.actor == 'jameswaweru377-lab'", condition)
        self.assertIn("github.event_name == 'schedule'", condition)
        self.assertIn("inputs.apply == true", condition)
        self.assertEqual("write", apply["permissions"]["contents"])
        self.assertEqual("read", apply["permissions"]["pull-requests"])
        self.assertEqual({"contents", "pull-requests"}, set(apply["permissions"]))
        run = next(step["run"] for step in apply["steps"] if step.get("id") == "apply")
        self.assertIn("--apply", run)
        self.assertIn("GITHUB_TOKEN", apply["env"])

    def test_workflow_has_no_other_write_scopes_or_pull_request_triggers(self):
        self.assertEqual("read", self.workflow["permissions"]["contents"])
        self.assertEqual("read", self.workflow["permissions"]["pull-requests"])
        self.assertEqual("false", self.workflow["concurrency"]["cancel-in-progress"])
        self.assertIn("github.repository", self.workflow["concurrency"]["group"])
        serialized = str(self.workflow).lower()
        for forbidden in ("issues: write", "pull-requests: write", "actions: write",
                          "gh pr merge", "gh issue close", "pull_request:"):
            self.assertNotIn(forbidden, serialized)

    def test_all_actions_are_pinned_to_full_commit_shas(self):
        for job in self.workflow["jobs"].values():
            for step in job["steps"]:
                if "uses" in step:
                    self.assertRegex(step["uses"], r"@[0-9a-f]{40}")


if __name__ == "__main__":
    unittest.main()
