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
        query = source(INGESTION, "TrustedSmsBodyQuery.kt")
        self.assertIn('BaseColumns._ID + " = ? AND " + Telephony.Sms.ADDRESS + " = ?)"', query)
        self.assertIn("withApprovedSenders", reader)
        self.assertNotIn("by lazy", reader)

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

    def test_original_message_reference_is_filtered_before_any_trusted_body_is_returned(self):
        evidence = source(INGESTION, "TransactionMessageEvidenceReader.kt")
        reader = source(INGESTION, "SelectiveSmsInboxReader.kt")
        self.assertIn("bodyContainsReference = code", evidence)
        self.assertIn("trustedSmsBodyQuery", reader)
        self.assertIn("bodyContainsReference", reader)

    def test_replays_only_when_unconfirmed_events_might_be_lost(self):
        screen = source(SCREENS, "SmartOnboardingSetupScreen.kt")
        policy = source(INGESTION, "OnboardingOwnershipReplayPolicy.kt")
        self.assertEqual(2, len(re.findall(r"HistoricalSmsImporter\s*\(", screen)))
        self.assertIn("ownershipReplayRequired", screen)
        self.assertIn("shouldReplaySmsAfterOwnership", policy)
        self.assertIn("initialImport.pendingReview > 0", policy)
        self.assertIn("initialImport == null", policy)
        self.assertIn("initialImport.cancelled", policy)

    def test_interrupted_ownership_replay_does_not_silently_finish_onboarding(self):
        screen = source(SCREENS, "SmartOnboardingSetupScreen.kt")
        policy = source(INGESTION, "OnboardingOwnershipReplayPolicy.kt")
        discovery = source(SCREENS, "OnboardingDiscoveryPage.kt")
        self.assertIn("ownershipReplayCompleted", screen)
        self.assertIn("ownershipReplayCompleted", policy)
        self.assertIn("if (applySelectedAccountOwnership())", screen)
        self.assertIn("Continue with accounts only", discovery)
        self.assertIn("ownershipReplayRequired", screen)
        self.assertIn("previouslyIncomplete", policy)
        self.assertIn("historicalScanUnverified by rememberSaveable", screen)
        self.assertIn("priorScanUnverified = historicalScanUnverified", screen)
        self.assertIn("committedOwnedKeys", screen)
        self.assertIn("selectionsLocked", discovery)
        self.assertIn("isCancelled =", screen)
        # The accounts-only escape must never become available before the
        # account store has acknowledged the selected ownership writes.
        self.assertLess(screen.index("check(financialUnderstandingStore.confirmEntityOwned"),
                        screen.index("committedOwnedKeys = selectedKeys.toList()"))
        self.assertIn("check(financialUnderstandingStore.setInstitutionOwned(reference) != null)", screen)
        self.assertIn("val scanningJob =", screen)
        self.assertIn("isCancelled = { scanningJob?.isActive == false }", screen)

    def test_sender_policy_change_invalidates_checkpoints(self):
        store = source(INGESTION, "HistoricalImportCheckpointStore.kt")
        importer = source(INGESTION, "HistoricalSmsImporter.kt")
        approvals = source(ROOT / "app/src/main/java/com/kedevjimmy/pesatrack/data/local", "FinancialSmsSenderApprovalStore.kt")
        self.assertIn("approvalRevision", store)
        self.assertIn("expectedApprovalRevision", store)
        self.assertIn("approvalRevision", importer)
        self.assertIn("REVISION", approvals)
        self.assertIn("withApprovedSenders", approvals)

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
