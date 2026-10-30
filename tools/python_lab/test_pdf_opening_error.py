"""Synthetic error-chain test; never include customer PDF information."""
import unittest

from pdfminer.pdfdocument import PDFPasswordIncorrect
from pdf_opening_error import requires_password


class PdfOpeningErrorTest(unittest.TestCase):
    def test_password_error_in_context_is_found_when_wrapper_also_has_cause(self):
        wrapped = RuntimeError("redacted")
        wrapped.__cause__ = RuntimeError("redacted")
        wrapped.__context__ = PDFPasswordIncorrect("redacted")
        self.assertTrue(requires_password(wrapped))

    def test_unrelated_nested_error_is_not_reported_as_password_error(self):
        wrapped = RuntimeError("redacted")
        wrapped.__cause__ = ValueError("redacted")
        wrapped.__context__ = RuntimeError("redacted")
        self.assertFalse(requires_password(wrapped))


if __name__ == "__main__":
    unittest.main()
