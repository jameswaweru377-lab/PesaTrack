"""Contract checks for the public tracker templates and local documentation."""

import pathlib
import unittest

import yaml


ROOT = pathlib.Path(__file__).resolve().parents[3]


class TrackerTemplatesTest(unittest.TestCase):
    def test_issue_template_is_valid_and_requires_ownership_context(self):
        path = ROOT / ".github/ISSUE_TEMPLATE/pesatrack-task.yml"
        template = yaml.safe_load(path.read_text(encoding="utf-8"))
        self.assertEqual("PesaTrack task", template["name"])
        fields = {item["id"]: item for item in template["body"] if "id" in item}
        for field in ("source", "acceptance", "chat_owner", "affected_paths", "device_status"):
            self.assertIn(field, fields)
        self.assertTrue(fields["acceptance"]["validations"]["required"])

    def test_pr_template_requires_explicit_evidence_and_never_closes_issue(self):
        text = (ROOT / ".github/PULL_REQUEST_TEMPLATE.md").read_text(encoding="utf-8")
        for label in ("Tracks #", "Chat owner:", "Base commit:", "Head commit:",
                      "CI evidence:", "Device acceptance:", "Affected paths:"):
            self.assertIn(label, text)
        self.assertNotIn("Closes #", text)

    def test_owner_registry_and_rules_discourage_private_chat_links(self):
        ownership = (ROOT / "docs/tracker/CHAT_OWNERSHIP.md").read_text(encoding="utf-8")
        rules = (ROOT / "docs/tracker/README.md").read_text(encoding="utf-8")
        self.assertIn("CHAT-", ownership)
        self.assertIn("Do not publish", ownership)
        self.assertIn("GitHub Issues", rules)
        self.assertIn("physical-device", rules)

    def test_command_center_handoff_has_no_automatic_merge_or_fake_board_link(self):
        starter = (ROOT / "docs/tracker/COMMAND_CENTER_STARTER.md").read_text(encoding="utf-8")
        rules = (ROOT / "docs/tracker/README.md").read_text(encoding="utf-8")
        for label in ("latest main", "GitHub Issues", "PR", "CI", "device",
                      "Unclaimed", "Do not merge"):
            self.assertIn(label, starter)
        self.assertIn("Create a project", rules)
        self.assertIn("pesatrack-tracker", rules)
        self.assertIn("not verified as created", rules)

    def test_repository_readme_has_public_tracker_entry_points_without_fake_board(self):
        readme = (ROOT / "README.md").read_text(encoding="utf-8")
        self.assertIn("docs/tracker/README.md", readme)
        self.assertIn("PesaTrack/issues", readme)
        self.assertIn("development-tracker.yml", readme)
        self.assertIn("board setup pending", readme.lower())


if __name__ == "__main__":
    unittest.main()
