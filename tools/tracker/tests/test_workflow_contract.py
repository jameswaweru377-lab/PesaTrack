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
        self.assertIn("issues", data["on"])
        self.assertIn("push", data["on"])
        self.assertIn("workflow_run", data["on"])
        self.assertIn("schedule", data["on"])
        self.assertIn("workflow_dispatch", data["on"])
        self.assertEqual("read", data["permissions"]["contents"])
        self.assertEqual("read", data["permissions"]["issues"])
        self.assertEqual("read", data["permissions"]["pull-requests"])
        self.assertEqual("read", data["permissions"]["actions"])
        self.assertEqual(["17 * * * *"], [rule["cron"] for rule in data["on"]["schedule"]])
        serialized = str(data).lower()
        for forbidden in ("gradlew", "assembledebug", "run_instrumentation",
                          "gh pr merge", "release upload", "issues: write"):
            self.assertNotIn(forbidden, serialized)

    def test_first_pr_skips_safely_until_trusted_main_has_tracker_module(self):
        workflow = yaml.load((ROOT / ".github/workflows/development-tracker.yml").read_text(),
                             Loader=yaml.BaseLoader)
        script = workflow["jobs"]["report"]["steps"][1]["run"]
        self.assertIn("-f tools/tracker/cli.py", script)
        self.assertIn("not installed on main", script)

    def test_project_mirror_has_separate_secret_and_explicit_opt_in(self):
        workflow = yaml.load((ROOT / ".github/workflows/development-tracker.yml").read_text(),
                             Loader=yaml.BaseLoader)
        preview = workflow["jobs"]["report"]["steps"][-2]
        self.assertIn("preview_project == true", preview["if"])
        self.assertNotIn("--apply", preview["run"])
        step = workflow["jobs"]["report"]["steps"][-1]
        self.assertIn("PESATRACK_TRACKER_PROJECT_SYNC_ENABLED == 'true'", step["if"])
        self.assertIn("github.event.pull_request.head.repo.full_name == github.repository", step["if"])
        self.assertIn("secrets.PESATRACK_TRACKER_PROJECTS_TOKEN", step["env"]["PESATRACK_TRACKER_PROJECTS_TOKEN"])
        self.assertIn("--apply", step["run"])
        self.assertEqual("read", workflow["permissions"]["issues"])


if __name__ == "__main__":
    unittest.main()
