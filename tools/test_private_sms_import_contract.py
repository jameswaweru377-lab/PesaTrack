"""Offline source-level privacy contracts; no device SMS or instrumentation."""

from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
INGESTION = ROOT / "app/src/main/java/com/kedevjimmy/pesatrack/ingestion"
SCREENS = ROOT / "app/src/main/java/com/kedevjimmy/pesatrack/ui/screens"


def source(folder, name):
    return (folder / name).read_text(encoding="utf-8")


class PrivateSmsImportContract(unittest.TestCase):
    def test_reader_queries_sender_metadata_before_any_message_body(self):
        reader = source(INGESTION, "SelectiveSmsInboxReader.kt")
        self.assertIn("Telephony.Sms.ADDRESS", reader)
        self.assertIn("Telephony.Sms.BODY", reader)
        self.assertIn("BaseColumns._ID", reader)
        self.assertIn("FinancialSmsSenderPolicy.isTrusted", reader)
        self.assertIn("BODY_PROJECTION", reader)
        self.assertIn("METADATA_PROJECTION", reader)
        self.assertRegex(reader, r"BaseColumns\._ID\s*\+\s*\" IN")

    def test_historical_import_does_not_query_all_sms_bodies(self):
        importer = source(INGESTION, "HistoricalSmsImporter.kt")
        self.assertIn("SelectiveSmsInboxReader", importer)
        self.assertNotIn("Telephony.Sms.BODY", importer)
        self.assertNotIn("Telephony.Sms.Inbox.CONTENT_URI", importer)

    def test_other_sms_readers_use_same_private_sender_filter(self):
        for filename in ("SalarySmsCandidateFinder.kt", "TransactionMessageEvidenceReader.kt"):
            with self.subTest(filename=filename):
                reader = source(INGESTION, filename)
                self.assertIn("SelectiveSmsInboxReader", reader)
                self.assertNotIn("Telephony.Sms.BODY", reader)

    def test_no_second_full_inbox_import_after_ownership_selection(self):
        screen = source(SCREENS, "SmartOnboardingSetupScreen.kt")
        self.assertEqual(1, len(re.findall(r"HistoricalSmsImporter\s*\(", screen)))

    def test_unknown_sender_approval_requires_explicit_import_screen_action(self):
        screen = source(SCREENS, "HistoricalImportScreen.kt")
        section = source(SCREENS, "FinancialSmsSenderApprovalSection.kt")
        self.assertIn("FinancialSmsSenderApprovalSection(importing", screen)
        self.assertIn("FinancialSmsSenderApprovalStore", section)
        self.assertIn("Approve sender", section)
        self.assertIn("Remove", section)
        self.assertIn("Sender name or shortcode", section)


if __name__ == "__main__":
    unittest.main()
