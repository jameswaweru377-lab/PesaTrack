"""Contracts for invented, deterministic corpus variations only."""
import unittest
from tools.python_lab.synthetic_corpus import generate


class SyntheticCorpusTests(unittest.TestCase):
    def test_count_uniqueness_and_reproducibility(self):
        corpus = generate()
        self.assertEqual(corpus, generate())
        self.assertEqual(2432, len(corpus["cases"]))
        self.assertEqual(2432, len({r["id"] for r in corpus["cases"]}))
        self.assertEqual(19, len({r["fixture_id"] for r in corpus["cases"]}))

    def test_exact_scaling_without_touching_identity_or_dates(self):
        rows = generate()["cases"]
        row = next(r for r in rows if r["fixture_id"] == "ordinary_mpesa_payment" and r["scale"] == 3)
        self.assertIn("Ksh 1,350.00 sent to SAMPLE PERSON 0712000000 on 28/9/26", row["text"])
        self.assertIn("Transaction cost, Ksh 21.00", row["text"])

    def test_zero_fees_stay_zero(self):
        row = next(r for r in generate()["cases"] if r["fixture_id"] == "mpesa_buy_goods_paid_to" and r["scale"] == 128)
        self.assertIn("Transaction cost, Ksh 0.00", row["text"])

    def test_no_external_input_interface(self):
        with self.assertRaises(TypeError):
            generate("personal-sms.json")
