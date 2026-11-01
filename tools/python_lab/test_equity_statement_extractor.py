"""Synthetic two-page Equity-shaped PDF contract; no real customer data."""
import base64
import io
import json
import unittest
from unittest.mock import patch
from pdfminer.pdfdocument import PDFPasswordIncorrect
from reportlab.pdfgen import canvas
from reportlab.lib.pdfencrypt import StandardEncryption

from bank_table_extractor import inspect_pdf

WIDTH, HEIGHT = 1200, 1350
EDGES = (30.5, 123.4, 233.9, 557, 691, 835, 1029.5)
HEADERS = ("Date", "Value", "Particulars", "Money Out", "Money In", "Balance")


def invented_pdf(wrong_total=False, unknown_value=False, missing_footer=False,
                 reversed_account_label=False, colon_summary_labels=False,
                 wide_notice=False, other_account_on_second_page=False,
                 broken_running_balance=False, encrypted=False, only_total=None):
    output = io.BytesIO()
    pdf = canvas.Canvas(output, pagesize=(WIDTH, HEIGHT),
                        encrypt=StandardEncryption(userPassword="invented-opening-pass", ownerPassword="invented-owner-pass")
                        if encrypted else None)

    def word(text, x, top):
        pdf.setFont("Helvetica", 11)
        pdf.drawString(x, HEIGHT - top - 11, text)

    def grid(top, bottom):
        for x in EDGES:
            pdf.line(x, HEIGHT - top, x, HEIGHT - bottom)
        pdf.line(EDGES[0], HEIGHT - top, EDGES[-1], HEIGHT - top)
        pdf.line(EDGES[0], HEIGHT - top - 28, EDGES[-1], HEIGHT - top - 28)
        pdf.line(EDGES[0], HEIGHT - bottom, EDGES[-1], HEIGHT - bottom)
        for i, h in enumerate(HEADERS):
            word(h, EDGES[i] + 5, top + 7)

    word("EQUITY BANK KENYA", 35, 45)
    account_header = "001234567890 ACCOUNT KES" if reversed_account_label else "ACCOUNT 001234567890 KES"
    word(account_header, 35, 242)
    grid(320, 1139)
    word("Statement information and general notices", 35, 354)
    pdf.showPage()

    word("EQUITY BANK KENYA", 35, 45)
    word("ACCOUNT 009999999999 KES" if other_account_on_second_page else account_header, 35, 134)
    grid(165, 1184)
    word("0.00 Cr", 840, 207)
    rows = [
        ("01-09-2026", "Invented deposit", "", "100.00", "100.00 Cr"),
        ("02-09-2026", "Invented transfer", "20.00", "", "80.00 Cr"),
        ("02-09-2026", "TRANSACTION + SMS CHARGE", "1.00", "", "79.00 Cr"),
        ("03-09-2026", "Invented receipt", "", "30.00", "109.00 Cr"),
        ("04-09-2026", "Invented bill", "15.00", "", "94.00 Cr"),
        ("05-09-2026", "Invented service", "5.00", "", "89.00 Cr"),
        ("06-09-2026", "Invented purchase", "9.00", "", "80.00 Cr"),
    ]
    for y, (day, description, out, incoming, balance) in zip((224, 244, 260, 280, 296, 316, 332), rows):
        word(day, 36, y)
        if unknown_value and y == 260:
            word("99.99", 132, y)
        word(description, 240, y)
        if out:
            word(out, 562, y)
        if incoming:
            word(incoming, 698, y)
        word("88.00 Cr" if broken_running_balance and y == 316 else balance, 840, y)
    if wide_notice:
        word("THIS IS AN INVENTED NON FINANCIAL NOTICE THAT CROSSES THE PHYSICAL TABLE COLUMNS AND IS NOT A PAYMENT", 36, 350)
    if not missing_footer:
        for label, y in (("Page Total", 368), ("Grand Total", 404)):
            if only_total and label != only_total:
                continue
            word(label + (":" if colon_summary_labels else ""), 35, y)
            word("51.00" if wrong_total else "50.00", 562, y)
            word("130.00", 698, y)
            word("80.00 Cr", 840, y)
        word("Uncleared Cheques" + (":" if colon_summary_labels else ""), 35, 440)
        word("0.00", 562, 440)
    word("This invented statement has no real customer details.", 35, 476)
    pdf.showPage()
    pdf.save()
    return base64.b64encode(output.getvalue()).decode("ascii")


class EquityStatementExtractorTest(unittest.TestCase):
    def test_single_printed_summary_is_accepted(self):
        for label in ("Page Total", "Grand Total"):
            with self.subTest(label=label):
                response = json.loads(inspect_pdf(invented_pdf(only_total=label), "", "KENYAN_BANK_EQUITY"))
                self.assertEqual("ready_for_bank_review", response["status"], response.get("reason"))
                document = response["document"]
                self.assertEqual(label == "Grand Total", document["printedGrandTotal"] is not None)
                self.assertEqual(label == "Page Total", bool(document["printedPageTotals"]))
                wrong = json.loads(inspect_pdf(invented_pdf(wrong_total=True, only_total=label), "", "KENYAN_BANK_EQUITY"))
                self.assertEqual("blocked", wrong["status"])

    def test_real_encrypted_statement_fails_without_password_then_passes_with_one(self):
        encoded = invented_pdf(encrypted=True)
        locked = json.loads(inspect_pdf(encoded, "", "KENYAN_BANK_EQUITY"))
        self.assertEqual("pdf_password_rejected", locked["reason"])
        wrong = json.loads(inspect_pdf(encoded, "invented-wrong-password", "KENYAN_BANK_EQUITY"))
        self.assertEqual("pdf_password_rejected", wrong["reason"])
        unlocked = json.loads(inspect_pdf(encoded, "invented-opening-pass", "KENYAN_BANK_EQUITY"))
        self.assertEqual("ready_for_bank_review", unlocked["status"], unlocked.get("reason"))

    def test_reconstructs_seven_legs_and_independent_fee_without_dropping_cover(self):
        response = json.loads(inspect_pdf(invented_pdf(), "", "KENYAN_BANK_EQUITY"))
        self.assertEqual("ready_for_bank_review", response["status"], response.get("reason"))
        self.assertEqual(2, response["document"]["pages"])
        self.assertEqual(1, len(response["tables"]))
        self.assertEqual(7, len(response["tables"][0]["rows"]))
        self.assertEqual("TRANSACTION + SMS CHARGE", response["tables"][0]["rows"][2][2])
        self.assertEqual(13000, response["document"]["printedGrandTotal"]["moneyInMinor"])
        self.assertEqual(5000, response["document"]["printedGrandTotal"]["moneyOutMinor"])

    def test_wrong_totals_block_whole_import(self):
        response = json.loads(inspect_pdf(invented_pdf(wrong_total=True), "", "KENYAN_BANK_EQUITY"))
        self.assertEqual("blocked", response["status"])

    def test_unknown_value_column_money_blocks(self):
        response = json.loads(inspect_pdf(invented_pdf(unknown_value=True), "", "KENYAN_BANK_EQUITY"))
        self.assertEqual("blocked", response["status"])

    def test_missing_footer_blocks(self):
        response = json.loads(inspect_pdf(invented_pdf(missing_footer=True), "", "KENYAN_BANK_EQUITY"))
        self.assertEqual("blocked", response["status"])

    def test_unknown_issuer_blocks(self):
        response = json.loads(inspect_pdf(invented_pdf(), "", "KENYAN_BANK_UNLISTED"))
        self.assertEqual("blocked", response["status"])

    def test_account_number_may_precede_the_header_label_when_unique(self):
        response = json.loads(inspect_pdf(invented_pdf(reversed_account_label=True), "", "KENYAN_BANK_EQUITY"))
        self.assertEqual("ready_for_bank_review", response["status"], response.get("reason"))

    def test_colon_terminated_summary_labels(self):
        response = json.loads(inspect_pdf(invented_pdf(colon_summary_labels=True), "", "KENYAN_BANK_EQUITY"))
        self.assertEqual("ready_for_bank_review", response["status"], response.get("reason"))

    def test_wide_nonfinancial_notice_between_last_transaction_and_totals(self):
        response = json.loads(inspect_pdf(invented_pdf(wide_notice=True), "", "KENYAN_BANK_EQUITY"))
        self.assertEqual("ready_for_bank_review", response["status"], response.get("reason"))

    def test_account_identity_cannot_change_between_pages(self):
        response = json.loads(inspect_pdf(invented_pdf(other_account_on_second_page=True), "", "KENYAN_BANK_EQUITY"))
        self.assertEqual("blocked", response["status"])

    def test_running_balance_error_blocks_all_rows(self):
        response = json.loads(inspect_pdf(invented_pdf(broken_running_balance=True), "", "KENYAN_BANK_EQUITY"))
        self.assertEqual("blocked", response["status"])

    def test_rejected_password_is_distinguished_without_leaking_exception_text(self):
        with patch("pdfplumber.open", side_effect=PDFPasswordIncorrect("PRIVATE_PASSWORD_DETAILS")):
            response = json.loads(inspect_pdf(invented_pdf(), "not-the-password", "KENYAN_BANK_EQUITY"))
        self.assertEqual("pdf_password_rejected", response["reason"])
        self.assertNotIn("PRIVATE_PASSWORD_DETAILS", json.dumps(response))

    def test_unexpected_document_open_error_is_sanitized(self):
        with patch("pdfplumber.open", side_effect=RuntimeError("PRIVATE_DOCUMENT_DETAILS")):
            response = json.loads(inspect_pdf(invented_pdf(), "", "KENYAN_BANK_EQUITY"))
        self.assertEqual("pdf_open_failed", response["reason"])
        self.assertNotIn("PRIVATE_DOCUMENT_DETAILS", json.dumps(response))

    def test_unexpected_page_extraction_error_is_sanitized(self):
        with patch("bank_table_extractor._read_page", side_effect=RuntimeError("PRIVATE_ROW_DETAILS")):
            response = json.loads(inspect_pdf(invented_pdf(), "", "KENYAN_BANK_EQUITY"))
        self.assertEqual("pdf_page_extraction_failed", response["reason"])
        self.assertNotIn("PRIVATE_ROW_DETAILS", json.dumps(response))


if __name__ == "__main__":
    unittest.main()
