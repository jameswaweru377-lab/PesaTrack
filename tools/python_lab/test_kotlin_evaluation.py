"""Contract tests use invented exports, never claim these are Kotlin executions."""
import unittest
import hashlib
import json
import os
from copy import deepcopy
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest.mock import patch

from tools.python_lab import kotlin_evaluation as bridge


class KotlinEvaluationTests(unittest.TestCase):
    def setUp(self):
        self.sha = "a" * 40
        self.manifest = {"corpus": "test", "cases": [
            {"id": "transfer", "text": "Move money", "sha256": "b" * 64},
            {"id": "balance", "text": "Balance only", "sha256": "c" * 64},
            {"id": "receipt", "text": "Receipt", "sha256": "d" * 64},
        ]}
        self.export = {"schema_version": 1, "corpus": "test", "source_sha": self.sha,
                       "cases": [
            {"id": "transfer", "sha256": "b" * 64, "kinds": ["TRANSFER", "FEE"],
             "recognized": True, "requires_legacy_fallback": False},
            {"id": "balance", "sha256": "c" * 64, "kinds": ["BALANCE_OBSERVATION"],
             "recognized": True, "requires_legacy_fallback": False},
            {"id": "receipt", "sha256": "d" * 64, "kinds": ["RECEIPT"],
             "recognized": True, "requires_legacy_fallback": False},
        ]}

    def test_rejects_stale_modified_duplicate_missing_and_extra_cases(self):
        mutations = []
        for field, value in [("source_sha", "e" * 40), ("schema_version", True),
                             ("corpus", "other")]:
            item = deepcopy(self.export)
            item[field] = value
            mutations.append(item)
        for mutate in [lambda e: e["cases"].pop(),
                       lambda e: e["cases"].append(deepcopy(e["cases"][0])),
                       lambda e: e["cases"][0].update(sha256="f" * 64),
                       lambda e: e["cases"][0].update(body="private SMS"),
                       lambda e: e["cases"][0].update(kinds=["UNKNOWN"]),
                       lambda e: e["cases"][0].update(recognized="true"),
                       lambda e: e["cases"][0].update(id="unreviewed")]:
            item = deepcopy(self.export)
            mutate(item)
            mutations.append(item)
        for item in mutations:
            with self.subTest(item=item), self.assertRaises(ValueError):
                bridge.validate_export(item, self.manifest, self.sha)

    def test_receipts_cash_and_uncertain_cases_are_not_assumed_income(self):
        for kinds, fallback in [(["RECEIPT"], False), (["CASH_DEPOSIT"], False),
                                (["UNRESOLVED_MOVEMENT"], True), ([], True),
                                (["PAYMENT", "DEBT_DRAWDOWN"], False)]:
            self.assertIsNone(bridge.expected_labels(kinds, fallback))
        self.assertEqual(bridge.expected_labels(["TRANSFER", "FEE"], False), {"transfer", "savings_transfer"})
        self.assertEqual(bridge.expected_labels(["DEBT_DRAWDOWN"], True), {"loan_drawdown"})

    def test_counts_wrong_unsafe_and_unscored_without_exposing_text(self):
        rows = bridge.validate_export(self.export, self.manifest, self.sha)
        report = bridge.compare(rows, ["income", "expense", "income"], self.sha)
        self.assertEqual(report["counts"], {"total": 3, "correct": 0, "incorrect": 2,
                                          "abstained": 0, "unsupported": 1})
        self.assertEqual(report["unsafe_suggestions"], {"non_income_as_income": 1,
                                                       "false_positive_movement": 1})
        self.assertEqual(report["candidate_gate"], "blocked")
        self.assertNotIn("text", report["cases"][0])
        self.assertFalse(report["production_connected"])

    def test_abstentions_and_compatible_labels_are_counted_separately(self):
        rows = bridge.validate_export(self.export, self.manifest, self.sha)
        report = bridge.compare(rows, ["savings_transfer", None, None], self.sha)
        self.assertEqual(report["counts"]["correct"], 1)
        self.assertEqual(report["counts"]["abstained"], 1)
        self.assertEqual(report["counts"]["unsupported"], 1)
        self.assertEqual(report["coverage"], 0.5)
        self.assertEqual(report["candidate_gate"], "not_assessed_for_integration")

    def test_predictions_must_cover_every_case_and_use_known_labels(self):
        rows = bridge.validate_export(self.export, self.manifest, self.sha)
        for predictions in [[], [None] * 2, [None] * 4, ["made_up", None, None]]:
            with self.assertRaises(ValueError):
                bridge.compare(rows, predictions, self.sha)

    def test_fixed_file_evaluation_is_reproducible_read_only_and_rejects_stale_sha(self):
        manifest = deepcopy(self.manifest)
        export = deepcopy(self.export)
        for row, actual in zip(manifest["cases"], export["cases"]):
            row["title"] = "TEST"
            row["sha256"] = hashlib.sha256(("TEST\n" + row["text"]).encode()).hexdigest()
            actual["sha256"] = row["sha256"]
        with TemporaryDirectory() as directory:
            manifest_path = Path(directory) / "manifest.json"
            export_path = Path(directory) / "export.json"
            manifest_path.write_text(json.dumps(manifest), encoding="utf-8")
            export_path.write_text(json.dumps(export), encoding="utf-8")
            before = (manifest_path.read_bytes(), export_path.read_bytes())
            with patch.object(bridge, "MANIFEST", manifest_path), patch.object(bridge, "EXPORT", export_path):
                with patch.dict(os.environ, PESATRACK_LAB_SOURCE_SHA=self.sha):
                    first = bridge.run()
                    self.assertEqual(first, bridge.run())
                    self.assertFalse(first["production_connected"])
                with patch.dict(os.environ, PESATRACK_LAB_SOURCE_SHA="e" * 40):
                    with self.assertRaises(ValueError):
                        bridge.run()
            self.assertEqual(before, (manifest_path.read_bytes(), export_path.read_bytes()))

    def test_duplicate_json_fields_are_rejected(self):
        with self.assertRaises(ValueError):
            json.loads('{"source_sha":"a","source_sha":"b"}', object_pairs_hook=bridge._unique_object)

    def test_reviewed_manifest_is_disjoint_from_training_and_hashes_are_valid(self):
        manifest = json.loads(bridge.MANIFEST.read_text(encoding="utf-8"))
        training = bridge.lab.samples()
        self.assertEqual(len(manifest["cases"]), 19)
        self.assertFalse({row["text"] for row in manifest["cases"]} & {row["text"] for row in training})
        for row in manifest["cases"]:
            self.assertEqual(row["sha256"], hashlib.sha256((row["title"] + "\n" + row["text"]).encode()).hexdigest())


if __name__ == "__main__":
    unittest.main()
