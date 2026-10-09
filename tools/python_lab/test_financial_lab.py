"""Standard-library regression checks, independent of Android and any account."""
import json
import unittest
from copy import deepcopy
from pathlib import Path

try:
    from tools.python_lab import financial_lab as lab
except ModuleNotFoundError:  # unittest discovery started from this folder
    import financial_lab as lab


class SyntheticFinancialLabTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.fixture = json.loads(lab.FIXTURES.read_text(encoding="utf-8"))

    def test_fixtures_are_intentionally_synthetic(self):
        self.assertIn("INVENTED", self.fixture["notice"])
        self.assertGreaterEqual(len(self.fixture["messages"]), 20)
        self.assertEqual({x["kind"] for x in self.fixture["messages"]}, lab.KINDS)
        ids = [x["id"] for x in self.fixture["messages"]]
        self.assertEqual(len(ids), len(set(ids)))

    def test_candidate_abstains_on_unfamiliar_language(self):
        result = lab.suggest_kind("Garden flowers and sunrise greetings", lab.samples())
        self.assertIsNone(result["suggestion"])

    def test_model_evaluation_counts_every_fixture(self):
        report = lab.evaluate(lab.samples())
        self.assertEqual(report["total"], report["correct"] + report["incorrect"] + report["abstained"])
        self.assertGreaterEqual(report["coverage"], 0)
        self.assertLessEqual(report["coverage"], 1)

    def test_no_prediction_can_write_a_ledger_record(self):
        prediction = lab.suggest_kind("Loan approved and deposited into mobile wallet", lab.samples())
        self.assertNotIn("ledger", prediction)
        self.assertNotIn("amount", prediction)
        self.assertNotIn("balance", prediction)

    def test_canonical_fixture_has_valid_invariants(self):
        self.assertEqual(lab.validate_ledger(self.fixture["ledger"]), [])

    def test_duplicate_ids_are_rejected(self):
        items = deepcopy(self.fixture["ledger"])
        items.append(deepcopy(items[0]))
        self.assertTrue(any("duplicate" in x for x in lab.validate_ledger(items)))

    def test_loan_and_transfer_not_counted_as_income_or_spend(self):
        result = lab.monthly_summary(self.fixture["ledger"], "2026-09")
        self.assertEqual(result["income"], "45000.00")
        self.assertEqual(result["net_spending_after_refunds"], "812.00")
        self.assertEqual(result["loan_drawdowns_not_income"], "1381.00")
        self.assertEqual(result["loan_repayments_not_expenses"], "1381.00")
        self.assertEqual(result["transfers_not_expenses"], "11500.00")
        self.assertEqual(result["categories"]["Transport"], "0.00")

    def test_invalid_and_float_amounts_rejected(self):
        for invalid in [-220.0, 220.0, "-220", "0", "NaN", "Infinity", "0.0001"]:
            with self.subTest(invalid=invalid):
                records = deepcopy(self.fixture["ledger"])
                records[0]["amount"] = invalid
                self.assertTrue(any("invalid_amount" in x for x in lab.validate_ledger(records)))
                with self.assertRaises(ValueError):
                    lab.monthly_summary(records, "2026-09")

    def test_refund_without_provenance_is_rejected(self):
        records = deepcopy(self.fixture["ledger"])
        next(row for row in records if row["kind"] == "refund").pop("linked_event_id")
        self.assertTrue(any("refund_requires_link" in x for x in lab.validate_ledger(records)))

    def test_ambiguous_event_rejected_from_canonical_ledger(self):
        records = deepcopy(self.fixture["ledger"])
        records.append({"id":"unsure","kind":"no_movement","amount":"1","date":"2026-09-30"})
        self.assertTrue(any("no_movement_not_ledger_event" in x for x in lab.validate_ledger(records)))

    def test_anomaly_requires_three_month_baseline(self):
        h = self.fixture["spending_history"]
        current = self.fixture["spending_current"]
        self.assertEqual(lab.spending_changes(dict(list(h.items())[:2]), current), [])
        signals = lab.spending_changes(h, current)
        self.assertEqual([x["category"] for x in signals], ["Transport"])

    def test_goal_forecast_is_explicitly_a_projection(self):
        report = lab.goal_forecast("48000", "19000", ["7000", "7200", "7500"])
        self.assertEqual(report["estimated_months"], 5)
        self.assertEqual(report["type"], "illustrative_not_guaranteed")
        self.assertIsNone(lab.goal_forecast("48000", "19000", ["0"])["estimated_months"])
        with self.assertRaises(ValueError):
            lab.goal_forecast("48000", "50000", ["2000"])

    def test_no_runtime_network_or_android_dependencies(self):
        source = Path(lab.__file__).read_text(encoding="utf-8")
        for forbidden in ("import requests", "import urllib", "import sqlite3", "import socket", "import android", "import chaquopy"):
            self.assertNotIn(forbidden, source)
        self.assertFalse(lab.demo()["production_connected"])


if __name__ == "__main__":
    unittest.main()
