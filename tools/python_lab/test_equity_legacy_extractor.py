"""Synthetic legacy graphic-header bank statements; never customer data."""

import base64
import io
import json
import unittest

from PIL import Image
from reportlab.lib.utils import ImageReader
from reportlab.pdfgen import canvas

from bank_table_extractor import inspect_pdf


WIDTH, HEIGHT = 595, 842
HEADERS = (
    ("Transaction Details", 37), ("Payment reference", 246),
    ("Value Date", 324), ("Credit (Money In)", 393),
    ("Debit (Money Out)", 465), ("Balance", 537),
)
SOURCE_ROWS = (
    ("01/09/2026", "Invented deposit", "ABC0123456", "100.00", "", "100.00"),
    ("02/09/2026", "Invented payment", "ABC0123457", "", "20.00", "80.00"),
    ("03/09/2026", "Invented fee", "ABC0123458", "", "1.00", "79.00"),
)


def invented_legacy_pdf(*, wrong_balance=False, missing_money=False,
                        wrong_account=False, remove_header=False,
                        remove_logo=False, extra_row=False,
                        wrong_grand_total=False, stray_money=False):
    output = io.BytesIO()
    pdf = canvas.Canvas(output, pagesize=(WIDTH, HEIGHT))

    def write(value, x, top, font=9):
        pdf.setFont("Helvetica", font)
        pdf.drawString(x, HEIGHT - top - font, value)

    def heading(top):
        if not remove_header:
            for label, x in HEADERS:
                write(label, x, top, 5.5)

    def rows(items, start):
        for index, cells in enumerate(items):
            top = start + index * 52
            write(cells[1], 38, top - 15)
            write(cells[2], 246, top)
            write(cells[0], 324, top)
            if cells[3] and not (missing_money and index == 0):
                write(cells[3], 394, top)
            if cells[4]:
                write(cells[4], 466, top)
            write("90.00" if wrong_balance and index == 1 else cells[5], 536, top)

    if not remove_logo:
        img = Image.new("RGB", (60, 40), "#822c19")
        pdf.drawImage(ImageReader(img), 42, HEIGHT - 71, width=58, height=40)
    write("Account Number", 384, 25, 8)
    write("0099999999999" if wrong_account else "0012345678901", 447, 25, 8)
    write("Account Currency", 384, 40, 8)
    write("KES", 500, 40, 8)
    write("Statement Date", 384, 70, 8)
    write("04/09/2026", 447, 70, 8)
    heading(180)
    rows(SOURCE_ROWS[:2], 215)
    if stray_money:
        write("55.00", 466, 345)
    if extra_row:
        write("04/09/2026", 324, 345)
        write("1.00", 466, 345)
        write("78.00", 536, 345)
    pdf.showPage()

    heading(7)
    rows(SOURCE_ROWS[2:], 40)
    # Some older issuer PDFs double-draw summary glyphs. The source has
    # a grand Total, but no printed opening or per-page totals.
    for value, x in (
        ("101.00" if wrong_grand_total else "100.00", 410),
        ("21.00", 486), ("79.00", 544),
    ):
        write(value, x, 94)
        write(value, x, 94)
    write("Total", 37, 94)
    write("Total", 37, 94)
    pdf.showPage()
    pdf.save()
    return base64.b64encode(output.getvalue()).decode("ascii")


class EquityLegacyExtractorTest(unittest.TestCase):
    def inspect(self, **kwargs):
        return json.loads(inspect_pdf(invented_legacy_pdf(**kwargs), "", "KENYAN_BANK_EQUITY"))

    def test_graphical_issuer_and_no_page_totals_produce_limited_review(self):
        response = self.inspect()
        self.assertEqual("ready_for_bank_review", response["status"], response.get("reason"))
        self.assertEqual("grand_total_and_chain_only", response["document"]["assurance"])
        self.assertIsNone(response["document"]["openingBalanceMinor"])
        self.assertEqual(
            {"moneyInMinor": 10_000, "moneyOutMinor": 2_100},
            response["document"]["printedGrandTotal"],
        )
        self.assertEqual("0012345678901", response["document"]["accountIdentifier"])
        self.assertEqual(2, len(response["tables"]))
        self.assertEqual(3, sum(len(page["rows"]) for page in response["tables"]))

    def test_incorrect_running_balance_blocks_full_document(self):
        self.assertEqual("blocked", self.inspect(wrong_balance=True)["status"])

    def test_missing_cash_cell_blocks_full_document(self):
        self.assertEqual("blocked", self.inspect(missing_money=True)["status"])

    def test_unknown_header_or_logo_is_not_an_issuer_proof(self):
        self.assertEqual("blocked", self.inspect(remove_header=True)["status"])
        self.assertEqual("blocked", self.inspect(remove_logo=True)["status"])

    def test_unaccounted_cash_row_blocks_full_document(self):
        self.assertEqual("blocked", self.inspect(extra_row=True)["status"])

    def test_orphan_cash_cell_without_date_blocks_full_document(self):
        self.assertEqual("blocked", self.inspect(stray_money=True)["status"])

    def test_mismatched_printed_grand_total_blocks_full_document(self):
        self.assertEqual("blocked", self.inspect(wrong_grand_total=True)["status"])

    def test_unrecognized_bank_does_not_gain_equity_assurance(self):
        response = json.loads(inspect_pdf(invented_legacy_pdf(), "", "KENYAN_BANK_UNLISTED"))
        self.assertEqual("blocked", response["status"])


if __name__ == "__main__":
    unittest.main()
