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
        preview = next(s for s in workflow["jobs"]["report"]["steps"] if s["name"] == "Preview approved Project tracker-field updates")
        self.assertIn("preview_project == true", preview["if"])
        self.assertNotIn("--apply", preview["run"])
        step = next(s for s in workflow["jobs"]["report"]["steps"] if s["name"] == "Mirror canonical Issues to the approved Project tracker field")
        self.assertIn("PESATRACK_TRACKER_PROJECT_SYNC_ENABLED == 'true'", step["if"])
        self.assertIn("github.event.pull_request.head.repo.full_name == github.repository", step["if"])
        self.assertIn("secrets.PESATRACK_TRACKER_PROJECTS_TOKEN", step["env"]["PESATRACK_TRACKER_PROJECTS_TOKEN"])
        self.assertIn("--apply", step["run"])
        self.assertEqual("read", workflow["permissions"]["issues"])

    def test_chat_owner_is_previsualized_and_separately_opted_in(self):
        workflow = yaml.load((ROOT / ".github/workflows/development-tracker.yml").read_text(),
                             Loader=yaml.BaseLoader)
        self.assertEqual("false", workflow["on"]["workflow_dispatch"]["inputs"][
            "preview_owner"]["default"])
        preview = next(s for s in workflow["jobs"]["report"]["steps"] if s["name"] == "Preview approved Project Chat owner text changes")
        self.assertIn("preview_owner == true", preview["if"])
        self.assertIn("--preview-owner", preview["run"])
        self.assertNotIn("--apply", preview["run"])
        mirror = next(s for s in workflow["jobs"]["report"]["steps"] if s["name"] == "Mirror canonical Issues to the approved Project tracker field")
        self.assertEqual("${{ vars.PESATRACK_TRACKER_OWNER_SYNC_ENABLED }}",
                         mirror["env"]["PESATRACK_TRACKER_OWNER_SYNC_ENABLED"])
        self.assertIn("inputs.preview_owner != true", mirror["if"])

    def test_dependabot_prs_keep_reporting_without_privileged_project_writes(self):
        workflow = yaml.load((ROOT / ".github/workflows/development-tracker.yml").read_text(),
                             Loader=yaml.BaseLoader)
        steps = workflow["jobs"]["report"]["steps"]
        self.assertIn("pull_request", workflow["on"])
        self.assertIn("tools.tracker.cli", steps[1]["run"])
        mirror_condition = next(s for s in steps if s["name"] == "Mirror canonical Issues to the approved Project tracker field")["if"]
        self.assertIn("github.actor != 'dependabot[bot]'", mirror_condition)
        self.assertIn("github.event.pull_request.user.login != 'dependabot[bot]'",
                      mirror_condition)


    def test_pr_inbox_discovers_without_replacing_issue_mirror(self):
        workflow = yaml.load((ROOT / ".github/workflows/development-tracker.yml").read_text(),
                             Loader=yaml.BaseLoader)
        self.assertEqual("false", workflow["on"]["workflow_dispatch"]["inputs"]["preview_pr_inbox"]["default"])
        steps = workflow["jobs"]["report"]["steps"]
        names = [step["name"] for step in steps]
        self.assertLess(names.index("Mirror canonical Issues to the approved Project tracker field"),
                        names.index("Discover open PRs as separate Project intake cards"))
        preview = next(s for s in steps if s["name"] == "Preview open-PR intake into existing Project")
        intake = next(s for s in steps if s["name"] == "Discover open PRs as separate Project intake cards")
        self.assertNotIn("--apply", preview["run"])
        self.assertIn("--apply", intake["run"])
        self.assertIn("github.actor != 'dependabot[bot]'", intake["if"])
        self.assertIn("github.event.pull_request.head.repo.full_name == github.repository", intake["if"])
        self.assertIn("PESATRACK_TRACKER_PROJECT_SYNC_ENABLED == 'true'", intake["if"])
        self.assertIn("secrets.PESATRACK_TRACKER_PROJECTS_TOKEN", intake["env"]["PESATRACK_TRACKER_PROJECTS_TOKEN"])


if __name__ == "__main__":
    unittest.main()
