"""Lightweight workflow must not turn into an Android or write operation."""

from pathlib import Path
import unittest

import yaml


ROOT = Path(__file__).resolve().parents[3]


class WorkflowContractTest(unittest.TestCase):
    def test_tracker_runs_independently_with_read_only_scopes(self):
        data = yaml.load((ROOT / ".github/workflows/development-tracker.yml").read_text(),
                         Loader=yaml.BaseLoader)
        self.assertIn("pull_request", data["on"])
        self.assertIn("workflow_run", data["on"])
        self.assertIn("schedule", data["on"])
        self.assertIn("workflow_dispatch", data["on"])
        self.assertEqual("read", data["permissions"]["contents"])
        self.assertEqual("read", data["permissions"]["issues"])
        self.assertEqual("read", data["permissions"]["pull-requests"])
        self.assertEqual("read", data["permissions"]["actions"])
        serialized = str(data).lower()
        for forbidden in ("gradlew", "assembledebug", "run_instrumentation",
                          "gh pr merge", "release upload", "issues: write"):
            self.assertNotIn(forbidden, serialized)

    def test_first_pr_skips_safely_until_trusted_main_has_tracker_module(self):
        workflow = yaml.load((ROOT / ".github/workflows/development-tracker.yml").read_text(),
                             Loader=yaml.BaseLoader)
        script = workflow["jobs"]["report"]["steps"][-1]["run"]
        self.assertIn("-f tools/tracker/cli.py", script)
        self.assertIn("not installed on main", script)


if __name__ == "__main__":
    unittest.main()
