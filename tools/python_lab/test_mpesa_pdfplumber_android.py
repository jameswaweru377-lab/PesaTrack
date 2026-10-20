"""No private/customer PDFs: synthetic tables exercising the embedded engine."""
import io
import unittest
from datetime import datetime
from decimal import Decimal
from unittest.mock import patch

from reportlab.pdfgen import canvas
from mpesa_pdf_engine import extract_bytes
from mpesa_wallet_reconcile import reconcile


def draw_pdf(wrong_total=False):
    output = io.BytesIO()
    c = canvas.Canvas(output, pagesize=(1000, 700))
    def table(xs, top, header, body, row_height=37):
        ys = [700-top-i*row_height for i in range(len(body)+2)]
        for x in xs:
            c.line(x,ys[0],x,ys[-1])
        for y in ys:
            c.line(xs[0],y,xs[-1],y)
        for row_no, row in enumerate([header]+body):
            for i, value in enumerate(row):
                lines = str(value).split("\n")
                for index, text in enumerate(lines):
                    c.setFont("Helvetica", 10)
                    c.drawString(xs[i]+5, ys[row_no]-13-index*11, text)
    table([45,300,425,565],45,
          ["Transaction\nType","Paid In","Paid Out"],
          [["Received Money","300.00","0.00"],
           ["Overdraft","100.00","0.00"],
           ["Send Money","0.00","300.00"],
           ["Pay Bill","0.00","100.00"],
           ["TOTAL","400.00" if not wrong_total else "500.00","400.00"]],
          27)
    table([45,145,300,545,668,754,843,965],245,
          ["Receipt\nNo.","Completion\nTime","Details","Transaction\nStatus","Paid\nIn","Withdrawn","Balance"],
          [["QAB1234569","2026-04-26\n07:03:19","Send Money Reversal via API","Completed","-","-300.00","0.00"],
           ["QAB1234568","2026-04-25\n22:48:09","Fuliza Paybill payment","Completed","-","-100.00","0.00"],
           ["QAB1234568","2026-04-25\n22:48:09","OverDraft of Credit Party","Completed","100.00","-","100.00"],
           ["QAB1234567","2026-04-25\n22:40:16","Loan Payments","Completed","300.00","-","300.00"]],
          43)
    c.showPage();c.save()
    return output.getvalue()


class OfflineMpesaPdfTest(unittest.TestCase):
    def test_physical_table_cells_are_extracted_once(self):
        import mpesa_pdf_engine

        original = mpesa_pdf_engine._cell
        calls = 0

        def counted(page, rect):
            nonlocal calls
            calls += 1
            return original(page, rect)

        with patch.object(mpesa_pdf_engine, "_cell", counted):
            result = extract_bytes(draw_pdf())

        self.assertEqual("ready_for_separate_activity_review", result["status"])
        # Only the summary table uses per-cell crops; the physical table uses one
        # page-wide character assignment and retains the exact ownership checks.
        self.assertEqual(18, calls)

    def test_review_deadline_fails_closed(self):
        import mpesa_pdf_engine

        with patch.object(mpesa_pdf_engine, "MAX_REVIEW_SECONDS", 0):
            with self.assertRaisesRegex(ValueError, "local_review_timeout"):
                extract_bytes(draw_pdf())
    def test_unusual_grid_text_uses_crop_fallback(self):
        import mpesa_pdf_engine

        original = mpesa_pdf_engine._extract_table_grid_once

        def altered_header(page, rectangles):
            result = original(page, rectangles)
            if result is not None and result[0]:
                result[0][0][0] = "UNEXPECTED"
            return result

        with patch.object(mpesa_pdf_engine, "_extract_table_grid_once", altered_header):
            result = extract_bytes(draw_pdf())

        self.assertEqual("ready_for_separate_activity_review", result["status"])
        self.assertEqual(4, len(result["lines"]))

    def test_crop_fallback_checks_deadline_between_cells(self):
        import mpesa_pdf_engine

        original_grid = mpesa_pdf_engine._extract_table_grid_once
        original_cell = mpesa_pdf_engine._cell
        now = [0.0]
        calls = 0

        def altered_header(page, rectangles):
            result = original_grid(page, rectangles)
            if result is not None and result[0]:
                result[0][0][0] = "UNEXPECTED"
            return result

        def expire_after_first_fallback_cell(page, rect):
            nonlocal calls
            calls += 1
            value = original_cell(page, rect)
            if calls == 19:  # 18 summary cells, then one detailed-table cell
                now[0] = mpesa_pdf_engine.MAX_REVIEW_SECONDS + 1
            return value

        with patch.object(mpesa_pdf_engine.time, "monotonic", lambda: now[0]), \
                patch.object(mpesa_pdf_engine, "_extract_table_grid_once", altered_header), \
                patch.object(mpesa_pdf_engine, "_cell", expire_after_first_fallback_cell):
            with self.assertRaisesRegex(ValueError, "local_review_timeout"):
                extract_bytes(draw_pdf())

        self.assertEqual(19, calls)

    def test_physical_table_and_fuliza_review(self):
        result=extract_bytes(draw_pdf())
        self.assertEqual("ready_for_separate_activity_review",result["status"])
        self.assertEqual(4,len(result["lines"]))
        self.assertEqual(4,result["report"]["financialRows"])
        self.assertEqual(1,result["report"]["fuliza_separately_checked"])
        self.assertEqual(40000,result["report"]["paidInMinor"])
        self.assertEqual(40000,result["report"]["paidOutMinor"])
        self.assertFalse(result["report"]["verified_for_sms_ledger_import"])

    def test_official_summary_mismatch_rejected(self):
        with self.assertRaisesRegex(ValueError,"detailed_totals_mismatch"):
            extract_bytes(draw_pdf(True))

    def test_non_fuliza_zero_sum_cannot_bypass(self):
        t=datetime(2026,4,26,7)
        from datetime import timedelta
        def row(receipt,minute,delta,balance,description):
            return {"receipt":receipt,"when":t-timedelta(minutes=minute),
                    "delta":Decimal(delta),"balance":Decimal(balance),
                    "description":description,"page":1,"row":minute+1}
        rows=[row("R1",0,"-300","0","Reversal"),
              row("R2",10,"-100","0","ordinary payment"),
              row("R2",10,"100","100","OverDraft of Credit Party"),
              row("R3",20,"300","300","Loan payment")]
        with self.assertRaises(ValueError):
            reconcile(rows)

    def test_same_receipt_distinct_legs_preserved(self):
        result=extract_bytes(draw_pdf())
        self.assertEqual("QAB1234568",result["lines"][1]["reference"])
        self.assertEqual("QAB1234568",result["lines"][2]["reference"])
        self.assertEqual(-10000,-result["lines"][1]["withdrawnMinor"])
        self.assertEqual(10000,result["lines"][2]["paidInMinor"])


if __name__=="__main__":
    unittest.main()
