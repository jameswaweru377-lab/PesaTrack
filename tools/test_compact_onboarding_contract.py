"""Source-level regression checks for the approved PesaTrack first-run path."""

from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SCREENS = ROOT / "app/src/main/java/com/kedevjimmy/pesatrack/ui/screens"
COMPONENTS = ROOT / "app/src/main/java/com/kedevjimmy/pesatrack/ui/components"


def contents(name):
    return (SCREENS / name).read_text(encoding="utf-8")


class CompactOnboardingContract(unittest.TestCase):
    def test_approved_photo_and_current_logo(self):
        welcome = contents("PesaTrackWelcomePage.kt")
        logo = (COMPONENTS / "PesaComponents.kt").read_text(encoding="utf-8")
        self.assertIn("onboarding_cafe_clean", welcome)
        self.assertNotIn("onSkip", welcome)
        self.assertIn("ic_pesatrack_home_mark", logo)

    def test_local_profile_pin_and_consent_before_native_sms(self):
        profile = contents("OnboardingLocalProfilePage.kt")
        setup = contents("SmartOnboardingSetupScreen.kt")
        self.assertIn('"Full name"', profile)
        self.assertIn('"M-PESA phone number"', profile)
        self.assertIn('"Confirm PIN"', profile)
        self.assertIn("AppLockManager", profile)
        self.assertIn("SmsAccessConsentStore.setAccepted", profile)
        self.assertIn("SmartSetupStage.PROFILE", setup)
        self.assertIn("RequestPermission()", setup)
        self.assertLess(setup.index("if (stage == SmartSetupStage.PROFILE)"),
                        setup.index("SmartSmsPermissionPage("),
                        "The new native-permission route must return before legacy privacy UI")

    def test_all_history_with_full_black_ring_and_no_progress_card(self):
        setup = contents("SmartOnboardingSetupScreen.kt")
        scanning = contents("OnboardingDarkScanPage.kt")
        self.assertIn("ALL_HISTORY_SELECTION", setup)
        self.assertIn("sinceEpochMs = startEpochMs", setup)
        self.assertIn("CircularProgressIndicator", scanning)
        self.assertIn("Receipt", scanning)
        self.assertIn("PremiumVisualTokens.darkCanvas", scanning)
        self.assertNotIn("OnboardingProgressPanel", scanning)

    def test_borderless_search_and_explicit_selection_to_home(self):
        picker = contents("OnboardingDiscoveryPage.kt")
        setup = contents("SmartOnboardingSetupScreen.kt")
        self.assertNotIn(".border(", picker)
        self.assertNotIn("OutlinedTextField(", picker)
        self.assertIn("selectedOwnedKeys", picker)
        self.assertIn("onDone()", setup)

    def test_transaction_ranking_does_not_count_promotions_or_balance_only_events(self):
        presentation = contents("OnboardingInstitutionPresentationPolicy.kt")
        setup = contents("SmartOnboardingSetupScreen.kt")
        self.assertIn("fun isTransactionEvidence", presentation)
        self.assertIn("insertedTransactions > 0", presentation)
        self.assertIn("isTransactionEvidence(it)", setup)


if __name__ == "__main__":
    unittest.main()
