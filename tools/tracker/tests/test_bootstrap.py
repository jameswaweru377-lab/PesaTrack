"""Audit import planning never mutates GitHub or duplicates existing sources."""

import unittest
import json
from pathlib import Path

from tools.tracker.bootstrap import plan_import


class BootstrapTest(unittest.TestCase):
    def test_audit_manifest_covers_october_architecture_and_visual_registers(self):
        path = Path(__file__).resolve().parents[3] / "docs/tracker/AUDIT_MANIFEST.json"
        entries = json.loads(path.read_text(encoding="utf-8"))
        self.assertEqual(42, len(entries))
        self.assertEqual(42, len({entry["source_id"] for entry in entries}))
        self.assertEqual({"R1", "S5", "D2", "VC-01", "VC-22"}.issubset(
            {entry["source_id"] for entry in entries}), True)
        self.assertEqual(42, len(plan_import(entries, [])["create"]))

    def test_preview_creates_once_and_repeated_input_is_idempotent(self):
        entries = [
            {"source_id": "VC-01", "title": "Invented month target", "severity": "P1"},
            {"source_id": "VC-01", "title": "Invented month target", "severity": "P1"},
            {"source_id": "R4", "title": "Invented upgrade validation", "severity": "P0"},
        ]
        first = plan_import(entries, [])
        self.assertEqual(["R4", "VC-01"], [x["source_id"] for x in first["create"]])
        self.assertEqual("Untriaged", first["create"][0]["status"])
        self.assertEqual(first, plan_import(entries, []))

    def test_existing_audit_id_links_without_creating_issue(self):
        audit = [{"source_id": "VC-01", "title": "Invented month target", "severity": "P1"}]
        existing = [{"number": 123, "body": "Source: 2026-10-31 / VC-01\nChat owner: CHAT-UI"}]
        preview = plan_import(audit, existing)
        self.assertEqual([], preview["create"])
        self.assertEqual(123, preview["link"][0]["issue_number"])
        self.assertEqual("VC-01", preview["link"][0]["source_id"])

    def test_multiple_existing_issues_cause_review_instead_of_guess(self):
        audit = [{"source_id": "S5", "title": "Invented privacy", "severity": "P1"}]
        existing = [
            {"number": 41, "body": "Source: S5"},
            {"number": 42, "body": "Source: S5"},
        ]
        result = plan_import(audit, existing)
        self.assertEqual([], result["create"])
        self.assertEqual([], result["link"])
        self.assertEqual("ambiguous-existing", result["review"][0]["reason"])

    def test_matching_existing_pr_reference_links_but_never_marks_verified(self):
        audit = [{"source_id": "U3", "title": "Invented totals", "severity": "P1",
                  "related_prs": [478]}]
        existing = [{"number": 80, "body": "Tracks #80\nPR: #478\nDevice acceptance: not verified"}]
        result = plan_import(audit, existing)
        self.assertEqual([], result["create"])
        self.assertEqual(80, result["link"][0]["issue_number"])
        self.assertEqual("Untriaged", result["link"][0]["status"])

    def test_untrusted_or_missing_source_id_goes_to_review_not_create(self):
        entries = [
            {"source_id": "../unsafe", "title": "Invented", "severity": "P1"},
            {"source_id": "", "title": "Invented", "severity": "P2"},
            {"source_id": "VC-02", "title": "Invented", "severity": "fixed"},
        ]
        result = plan_import(entries, [])
        self.assertEqual([], result["create"])
        self.assertEqual(3, len(result["review"]))


if __name__ == "__main__":
    unittest.main()
